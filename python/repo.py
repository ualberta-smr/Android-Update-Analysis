import urllib.request
import os
import xml.etree.ElementTree as ET
from urllib.parse import urlparse

aosp_manifest_url_base = 'https://raw.githubusercontent.com/android/platform_manifest/{}/default.xml'
aosp_git_base = 'https://android.googlesource.com/'


def read_file(path):
    with open(path) as file:
        return file.read()


def download_url(url):
    response = urllib.request.urlopen(url)
    data = response.read()
    text = data.decode('utf-8')
    return text


def normalize_manifest_fetch_url(current_url, url):
    if url[:2] == '..':
        parsed_uri = urlparse(current_url)
        domain = '{uri.scheme}://{uri.netloc}/'.format(uri=parsed_uri)
        return domain + url[2:]
    if url[-1:] != '/':
        url = url + '/'
    return url


def get_target_repositories(manifest_text, target_remote=None, fetch_includes=True, manifest_url=None):
    target_repositories = dict()
    if manifest_text is None or manifest_text == '':
        manifest_text = download_url(manifest_url)
    root = ET.fromstring(manifest_text)

    if fetch_includes and root.find('include') is not None:
        for include in root.findall('include'):
            included_manifest_url = manifest_url[:manifest_url.rfind('/') + 1] + include.get('name')
            included_repos = get_target_repositories(None, target_remote, True, included_manifest_url)
            target_repositories = {**target_repositories, **included_repos}

    for repository in root.findall('project'):
        if target_remote is None and 'remote' in repository.keys():
            continue
        if target_remote is not None:
            if 'remote' not in repository.keys() or repository.get('remote') != target_remote:
                continue
        target_repositories[repository.get('path')] = dict()
        for attr in repository.keys():
            if attr != 'path':
                target_repositories[repository.get('path')][attr] = repository.get(attr)
    return target_repositories


def get_version_manifest(manifest_url, project_git_host, aosp_remote_name=None, proprietary_remote_name=None):
    version_manifest = dict()

    manifest_text = download_url(manifest_url)
    root = ET.fromstring(manifest_text)
    remote_default = root.find('default')
    all_remotes = dict()
    for remote in root.findall('remote'):
        all_remotes[remote.get('name')] = remote

    if aosp_remote_name is None:
        aosp_remote = remote_default
    else:
        aosp_remote = root.find('.//remote[@name="{}"]'.format(aosp_remote_name))
    if proprietary_remote_name is None:
        proprietary_remote = remote_default
    else:
        proprietary_remote = root.find('.//remote[@name="{}"]'.format(proprietary_remote_name))

    aosp_branch = aosp_remote.get('revision')
    if aosp_branch is None:
        proprietary_aosp_repos = get_target_repositories(manifest_text, aosp_remote_name, False)
        aosp_branch = proprietary_aosp_repos[list(proprietary_aosp_repos.keys())[0]]['revision']
    # Strip branch name.
    aosp_branch = aosp_branch[aosp_branch.rfind('/') + 1:]

    aosp_manifest_url = aosp_manifest_url_base.format(aosp_branch)
    aosp_repos = get_target_repositories(None, None, True, aosp_manifest_url)

    proprietary_branch = proprietary_remote.get('revision')
    if proprietary_branch is None:
        proprietary_repos = get_target_repositories(manifest_text, proprietary_remote_name, False)
        proprietary_branch = proprietary_repos[list(proprietary_repos.keys())[0]]['revision']
    # Strip branch name.
    proprietary_branch = proprietary_branch[proprietary_branch.rfind('/') + 1:]

    proprietary_base_git_url = proprietary_remote.get('fetch')
    while proprietary_base_git_url is None:
        proprietary_remote = all_remotes[proprietary_remote.get('remote')]
        proprietary_base_git_url = proprietary_remote.get('fetch')
    proprietary_base_git_url = normalize_manifest_fetch_url(project_git_host, proprietary_base_git_url)
    proprietary_repos = get_target_repositories(manifest_text, proprietary_remote_name, True, manifest_url)

    version_manifest['aosp_branch'] = aosp_branch
    version_manifest['aosp_repos'] = aosp_repos
    version_manifest['proprietary_branch'] = proprietary_branch
    version_manifest['proprietary_base_git_url'] = proprietary_base_git_url
    version_manifest['proprietary_repos'] = proprietary_repos

    return version_manifest


def parse_project_configs(config_path):
    config_text = read_file(config_path)
    config_root = ET.fromstring(config_text)
    project_configs = list()
    for project in config_root.findall('project'):
        project_dict = dict()
        project_dict['name'] = project.get('name')
        project_dict['git_host'] = project.get('git_host')
        project_versions = list()
        for version in project.findall('version'):
            version_config = dict()
            version_config['branch_name'] = version.get('branch_name')
            version_config['manifest_url'] = version.get('manifest_url')
            version_config['aosp_remote_name'] = version.get('aosp_remote_name')
            if version.get('aosp_remote_name').strip() == '':
                version_config['aosp_remote_name'] = None
            version_config['proprietary_remote_name'] = version.get('proprietary_remote_name')
            if version.get('proprietary_remote_name').strip() == '':
                version_config['proprietary_remote_name'] = None
            project_versions.append(version_config)
        project_dict['versions'] = project_versions
        project_configs.append(project_dict)
    return project_configs


def get_project_manifests(config_path):
    project_configs = parse_project_configs(config_path)
    project_manifests = dict()
    for project_config in project_configs:
        project_name = project_config['name']
        project_git_host = project_config['git_host']
        project_manifest = dict()
        for version_config in project_config['versions']:
            version_manifest = get_version_manifest(version_config['manifest_url'], project_git_host,
                                                    version_config['aosp_remote_name'],
                                                    version_config['proprietary_remote_name'])
            project_manifest[version_config['branch_name']] = version_manifest
        project_manifests[project_name] = project_manifest
    return project_configs, project_manifests


def extract_mutual_repos_single_version(version_manifest):
    mutual_repos = set()
    for proprietary_repo in version_manifest['proprietary_repos']:
        if proprietary_repo in version_manifest['aosp_repos']:
            mutual_repos.add(proprietary_repo)
    return mutual_repos


def extract_mutual_repos_all_versions(project_manifest):
    mutual_repos_single_version_list = list()
    for version_name, version_manifest in project_manifest.items():
        mutual_repos_single_version_list.append(extract_mutual_repos_single_version(version_manifest))
    return set.intersection(*mutual_repos_single_version_list)


def get_first_release_for_aosp_branch(aosp_branch):
    return aosp_branch[:aosp_branch.rfind('_')] + '_r1'


def write_file(content, path):
    if '/' in path:
        directory = path[:path.rfind('/')]
        if not os.path.exists(directory):
            os.makedirs(directory)
    with open(path, 'w+') as file:
        file.write(content)


def run(config_path):
    project_configs, project_manifests = get_project_manifests(config_path)
    for project_config in project_configs:
        project_name = project_config['name']
        project_manifest = project_manifests[project_name]

        for version_index in range(len(project_config['versions'])):
            output = ''
            version_name = project_config['versions'][version_index]['branch_name']
            version_manifest = project_manifest[version_name]
            aosp_branch = version_manifest['aosp_branch']
            next_aosp_branch = 'PLEASE_REPLACE_ME_WITH_ANDROID_NEW_VERSION_FOR_THIS_COMPARISON_SCENARIO'
            if version_index < len(project_config['versions']) - 1:
                next_version_name = project_config['versions'][version_index + 1]['branch_name']
                next_version_manifest = project_manifest[next_version_name]
                next_aosp_branch = get_first_release_for_aosp_branch(next_version_manifest['aosp_branch'])

            output += 'versions:{},{},{}'.format(aosp_branch, next_aosp_branch, version_name)
            output += '\n'
            mutual_repos = extract_mutual_repos_single_version(version_manifest)

            for mutual_repo in mutual_repos:
                aosp_git_url = aosp_git_base + version_manifest['aosp_repos'][mutual_repo]['name']
                proprietary_git_url = version_manifest['proprietary_base_git_url'] + \
                                      version_manifest['proprietary_repos'][mutual_repo]['name']
                repo_name = mutual_repo.replace('/', '_')
                output += '{},{},{}'.format(repo_name, aosp_git_url, proprietary_git_url)
                output += '\n'
            write_file(output, '{}/{}_{}_{}.csv'.format(project_name, aosp_branch, next_aosp_branch, version_name))


run('repos_config.xml')
