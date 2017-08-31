import urllib.request
import xml.etree.ElementTree as ET

cm_base_url = 'https://review.lineageos.org/'
aosp_base_url = 'https://android.googlesource.com/'


def download_url(url):
    response = urllib.request.urlopen(url)
    data = response.read()
    text = data.decode('utf-8')
    return text


def get_projects(xml_string):
    all_projects = dict()
    root = ET.fromstring(xml_string)
    for project in root.findall('project'):
        all_projects[project.get('path')] = dict()
        for attr in project.keys():
            if attr != 'path':
                all_projects[project.get('path')][attr] = project.get(attr)
    return all_projects

cm_versions = {'cm-10.1': 'android-4.2_r1',
               'cm-10.2': 'android-4.3_r1',
               'cm-11.0': 'android-4.4_r1',
               'cm-12.0': 'android-5.0.0_r1',
               'cm-12.1': 'android-5.1.0_r1',
               'cm-13.0': 'android-6.0.0_r1',
               'cm-14.0': 'android-7.0.0_r1',
               'cm-14.1': 'android-7.1.0_r1'}

all_versions = [('cm-10.1', 'android-4.2_r1'),
                ('cm-10.2', 'android-4.3_r1'),
                ('cm-11.0', 'android-4.4_r1'),
                ('cm-12.0', 'android-5.0.0_r1'),
                ('cm-12.1', 'android-5.1.0_r1'),
                ('cm-13.0', 'android-6.0.0_r1'),
                ('cm-14.0', 'android-7.0.0_r1'),
                ('cm-14.1', 'android-7.1.0_r1')]


def get_corresponding_projects(cm_version):
    cm_manifest_url = 'https://raw.githubusercontent.com/LineageOS/android/' + cm_version + '/default.xml'
    aosp_manifest_url = 'https://raw.githubusercontent.com/android/platform_manifest/' + cm_versions[cm_version] + '/default.xml'

    aosp_projects = get_projects(download_url(aosp_manifest_url))
    cm_projects = get_projects(download_url(cm_manifest_url))

    corresponding_projects = list()
    for cm_project_name, cm_project in cm_projects.items():
        if 'remote' not in cm_project:
            if cm_project_name in aosp_projects:
                cm_git_url = cm_base_url + cm_project['name']
                aosp_git_url = aosp_base_url + aosp_projects[cm_project_name]['name']
                subsystem_name = cm_project_name.replace('/', '_')
                if subsystem_name == 'frameworks_base':
                    continue
                corresponding_projects.append({'subsystem_name': subsystem_name,
                                     'aosp_git_url': aosp_git_url,
                                     'cm_git_url': cm_git_url})
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


def generate_mutual_subsystems():
    all_subsystems = dict()
    subsystems_names = set()
    for cm_version in cm_versions:
        all_subsystems[cm_version] = dict()

        corresponding_projects = get_corresponding_projects(cm_version)

        for corresponding_project in corresponding_projects:
            all_subsystems[cm_version][corresponding_project['subsystem_name']] = (corresponding_project['aosp_git_url'],
                                                                                   corresponding_project['cm_git_url'])
            subsystems_names.add(corresponding_project['subsystem_name'])

    for cm_version in all_subsystems:
        print('# of subsystems in {}: {}'.format(cm_version, len(all_subsystems[cm_version])))

    mutual_subsystems_between_all_versions = dict()
    for subsystem_name in subsystems_names:
        is_mutual = True
        for cm_version, cm_subsystems in all_subsystems.items():
            if subsystem_name not in cm_subsystems:
                is_mutual = False
                break
        if is_mutual:
            mutual_subsystems_between_all_versions[subsystem_name] = all_subsystems['cm-14.0'][subsystem_name]

    with open('subsystems_mutual.csv', 'w') as out:
        for i in range(len(all_versions) - 1):
            cm_version = all_versions[i][0]
            android_old_version = all_versions[i][1]
            android_new_version = all_versions[i + 1][1]
            out.write('versions:{},{},{}'.format(android_old_version, android_new_version, cm_version))
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

generate_mutual_subsystems()
for i in range(len(all_versions) - 1):
    generate_subsystems_for_version(i)

