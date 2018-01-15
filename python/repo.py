import urllib.request
import xml.etree.ElementTree as ET

aosp_manifest = 'https://raw.githubusercontent.com/android/platform_manifest/{}/default.xml'

project_specs_cm = {'manifest_url': 'https://raw.githubusercontent.com/LineageOS/android/{}/default.xml',
                    'versions': ['cm-10.1',
                                 'cm-10.2',
                                 'cm-11.0',
                                 'cm-12.0',
                                 'cm-12.1',
                                 'cm-13.0',
                                 'cm-14.0',
                                 'cm-14.1'
                                 ]
                    }

project_specs_paranoid = {'manifest_url': 'https://raw.githubusercontent.com/AOSPA/manifest/{}/default.xml',
                          'versions': ['cm-10.1',
                                       'cm-10.2',
                                       'cm-11.0',
                                       'cm-12.0',
                                       'cm-12.1',
                                       'cm-13.0',
                                       'cm-14.0',
                                       'cm-14.1'
                                       ]
                          }


def read_file(path):
    with open(path) as file:
        return file.read()


def download_url(url):
    response = urllib.request.urlopen(url)
    data = response.read()
    text = data.decode('utf-8')
    return text


def parse_project_configs(config_path):
    config_text = read_file(config_path)
    config_root = ET.fromstring(config_text)
    project_configs = list()
    for project in config_root.findall('project'):
        project_dict = dict()
        project_dict['name'] = project.get('name')
        project_versions = list()
        for version in project.findall('version'):
            version_dict = dict()
            version_dict['branch_name'] = version.get('branch_name')
            version_dict['manifest_url'] = version.get('manifest_url')
            if version.get('aosp_remote_name') != '':
                version_dict['aosp_remote_name'] = version.get('aosp_remote_name')
            if version.get('proprietary_remote_name') != '':
                version_dict['proprietary_remote_name'] = version.get('proprietary_remote_name')
            project_versions.append(version_dict)
        project_dict['versions'] = project_versions
        project_configs.append(project_dict)
    return project_configs


def get_repositories(xml_string):
    all_repositories = dict()
    root = ET.fromstring(xml_string)
    for project in root.findall('project'):
        all_repositories[project.get('path')] = dict()
        for attr in project.keys():
            if attr != 'path':
                all_repositories[project.get('path')][attr] = project.get(attr)
    return all_repositories


def generate_project_specs(project_configs):
    project_specs = dict()
    for project_config in project_configs:
        project_specs

    project_specs['versions_data'] = dict()
    for version in project_specs['versions']:
        manifest_xml_string = download_url(project_specs['manifest_url'].format(version))
        root_element = ET.fromstring(manifest_xml_string)
        base_aosp_url = root_element.find('.//remote[@name="aosp"]').get('fetch')
        base_aosp_branch = root_element.find('.//remote[@name="aosp"]').get('revision')

        default_branch = root_element.find('default').get('revision')
        default_remote_name = root_element.find('default').get('remote')
        default_remote_url = root_element.find('.//remote[@name="' + default_remote_name + '"]').get(
            'fetch')  # fetch?

        version_data = dict()
        version_data['base_aosp'] = dict()
        version_data['base_aosp']['url'] = base_aosp_url
        version_data['base_aosp']['branch'] = base_aosp_branch
        version_data['base_aosp']['manifest'] = aosp_manifest.format(base_aosp_branch)
        version_data['base_aosp']['repositories'] = get_repositories(download_url(version_data['base_aosp']['manifest']))
        version_data['default'] = dict()
        version_data['default']['url'] = default_remote_url
        version_data['default']['branch'] = default_branch
        version_data['default']['manifest'] = project_specs['manifest_url'].format(version)
        version_data['default']['repositories'] = get_repositories(manifest_xml_string)
        project_specs['versions_data'][version] = version_data


def get_corresponding_projects(version_data):
    aosp_repositories = version_data['base_aosp']['repositories']
    project_repositories = version_data['default']['repositories']

    corresponding_projects = list()
    for project_repository_name, project_repository in project_repositories.items():
        if 'remote' not in project_repository:
            if project_repository_name in aosp_repositories:
                project_git_url = version_data['default']['url'] + project_repository['name']
                aosp_git_url = version_data['base_aosp']['url'] + aosp_repositories[project_repository_name]['name']
                subsystem_name = project_repository_name.replace('/', '_')
                if subsystem_name == 'frameworks_base':  # TODO: Does this need to be hardcoded?
                    continue
                corresponding_projects.append({'subsystem_name': subsystem_name,
                                               'aosp_git_url': aosp_git_url,
                                               'project_git_url': project_git_url})
                # all_subsystems[cm_version][subsystem_name] = (aosp_git_url, cm_git_url)
                # subsystems_names.add(subsystem_name)
            else:
                pass
                # print("Couldn't match " + cm_project_name)
                # with open('subsystems_' + cm_version + '.csv', 'w') as out:
                #     for subsystem in subsystems:
                #         out.write('{},{},{}'.format(*subsystem))
                #         out.write('\n')
    return corresponding_projects


def generate_mutual_subsystems(project_specs):
    populate_project_specs(project_specs)
    all_subsystems = dict()
    subsystems_names = set()
    for project_version in project_specs['versions']:
        all_subsystems[project_version] = dict()

        corresponding_projects = get_corresponding_projects(project_specs['versions_data'][project_version])

        for corresponding_project in corresponding_projects:
            all_subsystems[project_version][corresponding_project['subsystem_name']] = (
                corresponding_project['aosp_git_url'],
                corresponding_project['project_git_url'])
            subsystems_names.add(corresponding_project['subsystem_name'])

    for project_version in all_subsystems:
        print('# of subsystems in {}: {}'.format(project_version, len(all_subsystems[project_version])))

    mutual_subsystems_between_all_versions = dict()
    for subsystem_name in subsystems_names:
        is_mutual = True
        for project_version, project_subsystems in all_subsystems.items():
            if subsystem_name not in project_subsystems:
                is_mutual = False
                break
        if is_mutual:
            mutual_subsystems_between_all_versions[subsystem_name] = all_subsystems['cm-14.0'][subsystem_name] # TODO: Hardcoded stuff

    with open('subsystems_mutual.csv', 'w') as out:
        for i in range(len(all_versions) - 1):
            project_version = all_versions[i][0]
            android_old_version = all_versions[i][1]
            android_new_version = all_versions[i + 1][1]
            out.write('versions:{},{},{}'.format(android_old_version, android_new_version, project_version))
            out.write('\n')
        for subsystem_name, (aosp_git_url, cm_git_url) in mutual_subsystems_between_all_versions.items():
            out.write(
                '{},{},{}'.format(subsystem_name, aosp_git_url, cm_git_url.replace('review.lineageos.org/CyanogenMod/'
                                                                                   ,
                                                                                   'review.lineageos.org/LineageOS/')))
            out.write('\n')


def generate_subsystems_for_version(cm_version_order):
    cm_version = all_versions[cm_version_order][0]
    android_old_version = all_versions[cm_version_order][1]
    android_new_version = all_versions[cm_version_order + 1][1]

    corresponding_projects = get_corresponding_projects(cm_version)

    versions_name = '{}_{}_{}'.format(android_old_version, android_new_version, cm_version)
    with open('subsystems_{}.csv'.format(versions_name), 'w') as out:
        out.write('versions:{},{},{}'.format(android_old_version, android_new_version, cm_version))
        out.write('\n')
        for corresponding_project in corresponding_projects:
            subsystem_name = corresponding_project['subsystem_name']
            aosp_git_url = corresponding_project['aosp_git_url']
            cm_git_url = corresponding_project['cm_git_url']
            out.write(
                '{},{},{}'.format(subsystem_name, aosp_git_url, cm_git_url.replace('review.lineageos.org/CyanogenMod/'
                                                                                   ,
                                                                                   'review.lineageos.org/LineageOS/')))
            out.write('\n')


populate_project_specs(project_specs_cm)
print(project_specs_cm)
# generate_mutual_subsystems()
# for i in range(len(all_versions) - 1):
#     generate_subsystems_for_version(i)
