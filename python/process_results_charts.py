import os
import re
import numpy as np
import copy
import statistics as stats

# comparison_scenarios = ['android-4.2_r1,android-4.3_r1,cm-10.1',
#                         'android-4.3_r1,android-4.4_r1,cm-10.2',
#                         'android-4.4_r1,android-5.0.0_r1,cm-11.0',
#                         'android-5.0.0_r1,android-5.1.0_r1,cm-12.0',
#                         'android-5.1.0_r1,android-6.0.0_r1,cm-12.1',
#                         'android-6.0.0_r1,android-7.0.0_r1,cm-13.0',
#                         'android-7.0.0_r1,android-7.1.0_r1,cm-14.0']
#
# comparison_scenario_short_names = {'android-4.2_r1,android-4.3_r1,cm-10.1': 'CS1',
#                                    'android-4.3_r1,android-4.4_r1,cm-10.2': 'CS2',
#                                    'android-4.4_r1,android-5.0.0_r1,cm-11.0': 'CS3',
#                                    'android-5.0.0_r1,android-5.1.0_r1,cm-12.0': 'CS4',
#                                    'android-5.1.0_r1,android-6.0.0_r1,cm-12.1': 'CS5',
#                                    'android-6.0.0_r1,android-7.0.0_r1,cm-13.0': 'CS6',
#                                    'android-7.0.0_r1,android-7.1.0_r1,cm-14.0': 'CS7'}

change_types = ['identical',
                'refactored_move',
                'refactored_rename',
                'refactored_inline',
                'refactored_extract',
                'argument_rename',
                'argument_reorder',
                'argument_add',
                'argument_remove',
                'argument_type_change',
                'body',
                'deleted']

short_cs_names = {'android-4.2.2_r1,android-4.3.1_r1,cm-10.1': 'CS1',
                  'android-4.3.1_r1,android-4.4.4_r1,cm-10.2': 'CS2',
                  'android-4.4.4_r2,android-5.0.2_r1,cm-11.0': 'CS3',
                  'android-5.0.2_r1,android-5.1.1_r1,cm-12.0': 'CS4',
                  'android-5.1.1_r37,android-6.0.1_r1,cm-12.1': 'CS5',
                  'android-6.0.1_r81,android-7.0.0_r1,cm-13.0': 'CS6',
                  'android-7.0.0_r14,android-7.1.2_r1,cm-14.0': 'CS7',
                  'android-7.1.2_r36,android-8.0.0_r1,cm-14.1': 'CS8',

                  'android-4.4.4_r1,android-5.0.2_r1,kitkat': 'CS3',
                  'android-5.0.2_r1,android-6.0.1_r1,lollipop': 'CS4',
                  'android-6.0.1_r62,android-7.0.0_r1,marshmallow': 'CS6',
                  'android-7.0.0_r6,android-7.1.2_r1,nougat': 'CS7'}

def is_right_format(lines):
    if len(lines) != 16:
        return False

    if len(lines[0].split(',')) != 3:
        return False

    for i in range(2, 14):
        if len(lines[i].split(',')) != 13:
            return False

    return True


def pretty_print_cell_colors():
    while (True):
        line = input()
        colors = ["'{}'".format(color) for color in line.split('\t')]
        print('[' + ', '.join(colors) + '],')


def determine_table_color(row, column):
    # cell_colors = [['n', 'g', 'g', 'g', 'r', 'n'],
    #                ['n', 'y', 'y', 'y', 'r', 'n'],
    #                ['n', 'y', 'r', 'y', 'r', 'n'],
    #                ['n', 'y', 'y', 'y', 'r', 'n'],
    #                ['n', 'r', 'r', 'r', 'n', 'n']]
    cell_colors = [['n', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'r', 'n'],
                   ['n', 'y', 'g', 'r', 'r', 'g', 'g', 'g', 'g', 'g', 'g', 'r', 'n'],
                   ['n', 'g', 'y', 'r', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'r', 'n'],
                   ['n', 'r', 'r', 'y', 'r', 'r', 'r', 'r', 'r', 'r', 'g', 'r', 'n'],
                   ['n', 'r', 'g', 'r', 'y', 'g', 'g', 'r', 'r', 'r', 'r', 'r', 'n'],
                   ['n', 'g', 'g', 'r', 'g', 'y', 'g', 'y', 'g', 'g', 'g', 'r', 'n'],
                   ['n', 'g', 'g', 'r', 'g', 'g', 'y', 'r', 'r', 'g', 'g', 'r', 'n'],
                   ['n', 'g', 'g', 'r', 'r', 'y', 'r', 'y', 'r', 'r', 'r', 'r', 'n'],
                   ['n', 'g', 'g', 'r', 'r', 'g', 'r', 'r', 'y', 'r', 'r', 'r', 'n'],
                   ['n', 'g', 'g', 'r', 'r', 'g', 'g', 'r', 'r', 'y', 'r', 'r', 'n'],
                   ['n', 'g', 'g', 'g', 'r', 'g', 'g', 'r', 'r', 'r', 'y', 'r', 'n'],
                   ['n', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'n']]
    return cell_colors[row][column]


def read_data():
    path = './results/'
    files = [path + f for f in os.listdir(path) if os.path.isfile(path + f) and f[-3:] == 'csv']

    results = dict()
    for file in files:
        file_name = file[file.rfind('/') + 1:]
        groups = re.split(r'(^.+)_(android-.+_r\d+)_(android-.+_r\d+)_(.+)_(.+).csv$', file_name)
        if len(groups) == 7:
            new_result = dict()
            new_result['subsystem'] = groups[1]
            new_result['ao'] = groups[2]
            new_result['an'] = groups[3]
            new_result['mo_name'] = groups[4]
            new_result['mo_version'] = groups[5]

            with open(file) as input_stream:
                lines = [line.strip() for line in input_stream.readlines()]
                if is_right_format(lines):
                    (new_result['ao_methods'], new_result['an_methods'], new_result['mo_methods']) = \
                        (int(number) for number in lines[0].split(','))
                    (new_result['an_new_methods'], new_result['mo_new_methods'], new_result['mutual_new_methods']) = \
                        (number for number in lines[15].split(','))
                    new_result['an_new_methods'] = int(new_result['an_new_methods'])
                    new_result['mo_new_methods'] = int(new_result['mo_new_methods'])
                    [new_result['mutual_new_methods'], new_result['mutual_new_methods_duplicates']] = \
                        [int(number) for number in re.split('(\d+)\((\d+)\)', new_result['mutual_new_methods'])[1:3]]
                    table = list()
                    for line in lines[2:14]:
                        table_line = list()
                        for item in line.split(','):
                            if '(' in item:
                                item = re.split(r'(\d+)\(\d+\)', item)[0] + re.split(r'(\d+)\(\d+\)', item)[1]
                            table_line.append(int(item))
                        table.append(table_line)
                    new_result['table'] = table

                    if new_result['mo_name'] not in results:
                        results[new_result['mo_name']] = list()
                    results[new_result['mo_name']].append(new_result)
    return results





def print_colours_stat(results):
    for project in results.keys():
        sum_of_tables = None
        for subsystem_result in results[project]:
            if sum_of_tables is None:
                sum_of_tables = np.array(subsystem_result['table'])
            else:
                sum_of_tables += np.array(subsystem_result['table'])

        categories = dict()
        for row in range(sum_of_tables.shape[0]):
            for col in range(sum_of_tables.shape[1]):
                if determine_table_color(row, col) in categories:
                    categories[determine_table_color(row, col)] += sum_of_tables[row, col]
                else:
                    categories[determine_table_color(row, col)] = sum_of_tables[row, col]

        all_mo_changes = sum_of_tables.sum(axis=0)[1:-1].sum()
        print("Project: {}  --  Total changes in modified: {} -- g:{},{}%  r:{},{}%  y:{},{}%".format(project,
                                                                                                      all_mo_changes,
                                                                                                      categories['g'], (
                                                                                                              categories[
                                                                                                                  'g'] / all_mo_changes) * 100,
                                                                                                      categories['r'], (
                                                                                                              categories[
                                                                                                                  'r'] / all_mo_changes) * 100,
                                                                                                      categories['y'], (
                                                                                                              categories[
                                                                                                                  'y'] / all_mo_changes) * 100))


def print_bar_chart_data(results):

    for project in results.keys():
        print(project + ":")
        bar_chart_result = dict()
        for subsystem_result in results[project]:
            comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['mo_version'])
            if comparison_scenario not in bar_chart_result:
                bar_chart_result[comparison_scenario] = dict()
            table = np.array(subsystem_result['table'])
            for row in range(table.shape[0]):
                for col in range(table.shape[1]):
                    cell_color = determine_table_color(row, col)
                    bar_chart_result[comparison_scenario][cell_color] = bar_chart_result[comparison_scenario].get(
                        cell_color, 0) + table[row][col]

        three_colors_list = dict()
        three_colors_list['g'] = list()
        three_colors_list['r'] = list()
        three_colors_list['y'] = list()

        colors_list = ['g', 'r', 'y']
        for comparison_scenario in bar_chart_result:
            colors_sum = 0
            for color in colors_list:
                colors_sum += bar_chart_result[comparison_scenario].get(color, 0)

            for color in colors_list:
                this_color_count = bar_chart_result[comparison_scenario].get(color, 0)
                bar_chart_result[comparison_scenario][color] = dict()
                bar_chart_result[comparison_scenario][color]['count'] = this_color_count
                bar_chart_result[comparison_scenario][color]['proportion'] = this_color_count / colors_sum
                three_colors_list[color].append(this_color_count / colors_sum)

        # print(stats.mean(three_colors_list['g']))
        # print(stats.mean(three_colors_list['r']))
        # print(stats.mean(three_colors_list['y']))

        color_labels = {'r': 'Conflict',
                        'g': 'No Conflict',
                        'y': 'Potential Conflict'}
        print("name,color,proportion,count,label_y")
        for comparison_scenario in bar_chart_result:
            for color in colors_list:
                proportion = bar_chart_result[comparison_scenario][color]['proportion']
                count = bar_chart_result[comparison_scenario][color]['count']
                label_y = proportion / 2
                if color == 'r':
                    label_y += (1 - proportion)
                elif color == 'g':
                    label_y += bar_chart_result[comparison_scenario]['y']['proportion']
                short_comparison_name = short_cs_names[comparison_scenario]
                print('{},{},{},{},{}'.format(short_comparison_name, color_labels[color], proportion, count, label_y))


def print_trends_plot_data(results):
    colors = {'r': '#EA0D30', 'g': '#48D373', 'y': '#FCC168', 'n': '#48D373'}

    for project in results.keys():
        print(project + ":")
        aggregated_tables = dict()
        for subsystem_result in results[project]:
            comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'],
                                                    subsystem_result['mo_version'])
            if comparison_scenario not in aggregated_tables:
                aggregated_tables[comparison_scenario] = np.matrix(subsystem_result['table'])[:, :-1]
            else:
                aggregated_tables[comparison_scenario] += np.matrix(subsystem_result['table'])[:, :-1]

        print('versions,an_type,mo_type,proportion,count,color')
        for comparison_scenario, aggregated_table in aggregated_tables.items():
            short_comparison_name = short_cs_names[comparison_scenario]
            # Don't count deleted-deleted
            # aggregated_table[11, 11] = 0
            sum_of_mo_changes = aggregated_table[:, 1:].sum()
            # print('sum of mo changes for {}: {}'.format(short_comparison_name, sum_of_mo_changes))
            for row in range(aggregated_table.shape[0]):
                for column in range(1, aggregated_table.shape[1]):
                    proportion = aggregated_table[row, column] / sum_of_mo_changes
                    print('{},{},{},{},{},{}'.format(short_comparison_name, change_types[row], change_types[column],
                                                     proportion, aggregated_table[row, column],
                                                     colors[determine_table_color(row, column)]))


def get_color_shade(color, intensity):
    intensity = 1 - intensity
    return '{:02X}'.format(int((255 - color[0]) * intensity) + color[0]) +\
           '{:02X}'.format(int((255 - color[1]) * intensity) + color[1]) +\
           '{:02X}'.format(int((255 - color[2]) * intensity) + color[2])


def print_heat_map(results):
    for project in results.keys():
        print(project + ":")
        mo_changeset_tables = None
        for subsystem_result in results[project]:
            if mo_changeset_tables is None:
                mo_changeset_tables = np.matrix(subsystem_result['table'])[:, :-1]
            else:
                mo_changeset_tables += np.matrix(subsystem_result['table'])[:, :-1]

        mo_changes = mo_changeset_tables.sum(axis=0)[:, 1:].sum()
        flat = mo_changeset_tables[:, 1:].flatten()
        flat.sort()
        second_max = int(flat[:, -2])
        max_no_changes = mo_changeset_tables[:, 1:].max()
        normalize_factor = 1 / (second_max / mo_changes)

        darkest_colors = {'r': (0xEA, 0x0D, 0x30),
                          'g': (0x48, 0xD3, 0x73),
                          'y': (0xFC, 0xC1, 0x68)
                          }

        for row in range(mo_changeset_tables.shape[0]):
            output_row = '\\multicolumn{1}{|c|}{} & ' + change_types[row].replace('_', '\\_') + " & "
            for column in range(1, mo_changeset_tables.shape[1]):
                real_proportion = mo_changeset_tables[row, column] / mo_changes
                normalized_proportion = min(1, real_proportion * normalize_factor)

                text_color = determine_table_color(row, column)
                back_color = get_color_shade(darkest_colors[determine_table_color(row, column)], normalized_proportion)
                output_row += '\cellcolor[HTML]{' + back_color +'}' + str(mo_changeset_tables[row, column]) + '\% & '
            output_row = output_row[:-2] + '\\\\ \\hhline{|~|-|-|-|-|-|-|-|-|-|-|-|-}'
            print(output_row)


def extract_most_changed_subsystems(results):
    for project in results.keys():
        print(project + ":")
        subsystem_results_by_comparison_scenario = dict()
        for subsystem_result in results[project]:
            comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'],
                                                    subsystem_result['mo_version'])

            changes_table = subsystem_result['table']
            sum_of_columns = np.sum(changes_table, axis=0)
            sum_of_mo_changes = sum(sum_of_columns[1:-1])
            sum_of_an_changes = sum([l[-1] for l in changes_table[1:]])
            new_subsystem_entry = {'subsystem': subsystem_result['subsystem'],
                                   'sum_of_mo_changes': sum_of_mo_changes,
                                   'sum_of_an_changes': sum_of_an_changes}

            if comparison_scenario not in subsystem_results_by_comparison_scenario:
                subsystem_results_by_comparison_scenario[comparison_scenario] = list()

            subsystem_results_by_comparison_scenario[comparison_scenario].append(new_subsystem_entry)

        for comparison_scenario in subsystem_results_by_comparison_scenario:

            unsorted_subsystems = subsystem_results_by_comparison_scenario[comparison_scenario]

            mutually_changed_subsystems = {x['subsystem'] for x in unsorted_subsystems if x['sum_of_mo_changes'] > 0
                                    and x['sum_of_an_changes'] > 0}

            top_k = 5
            an_sorted_subsystems_dict = sorted(unsorted_subsystems, key=lambda k: k['sum_of_an_changes'],
                                               reverse=True)[:top_k]
            mo_sorted_subsystems_dict = sorted(unsorted_subsystems, key=lambda k: k['sum_of_mo_changes'],
                                               reverse=True)[:top_k]
            an_sorted_subsystems_set = {x['subsystem'] for x in an_sorted_subsystems_dict}
            mo_sorted_subsystems_set = {x['subsystem'] for x in mo_sorted_subsystems_dict}
            subsystems_info = {x['subsystem']: (str(x['sum_of_an_changes']), str(x['sum_of_mo_changes'])) for x in
                               unsorted_subsystems}

            output_row = '\multirow{5}{*}{' + short_cs_names[comparison_scenario] + '} & '
            output_row += '\multirow{5}{*}{' + str(len(mutually_changed_subsystems)) + '}\n'
            for i in range(top_k):
                suffix = ''
                if i == top_k - 1:
                    suffix = ' \hline'
                an_subsystem = an_sorted_subsystems_dict[i]['subsystem']
                mo_subsystem = mo_sorted_subsystems_dict[i]['subsystem']
                if an_subsystem in mo_sorted_subsystems_set:
                    an_subsystem = '\\textbf{' + an_subsystem + '}'
                if mo_subsystem in an_sorted_subsystems_set:
                    mo_subsystem = '\\textbf{' + mo_subsystem + '}'
                an_subsystem = an_subsystem + ' (' + subsystems_info[an_sorted_subsystems_dict[i]['subsystem']][0] + ')'
                mo_subsystem = mo_subsystem + ' (' + subsystems_info[mo_sorted_subsystems_dict[i]['subsystem']][1] + ')'
                if i > 0:
                    output_row += ' &'
                output_row += ' & {} & {} \\\\{}'.format(an_subsystem.replace('_', '\_'),
                                                 mo_subsystem.replace('_', '\_'),
                                                 suffix + '\n')
            print(output_row)
#
#
# def print_subsystems_stats(subsystem_results):
#     subsystem_results_by_comparison_scenario = dict()
#     for comparison_scenario in comparison_scenarios:
#         subsystem_results_by_comparison_scenario[comparison_scenario] = set()
#
#     for subsystem_result in subsystem_results:
#         comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
#         subsystem_results_by_comparison_scenario[comparison_scenario].add(subsystem_result['subsystem'])
#
#     intersection_set = copy.copy(subsystem_results_by_comparison_scenario[comparison_scenarios[0]])
#     for comparison_scenario in comparison_scenarios:
#         print(comparison_scenario + ': ' + str(len(subsystem_results_by_comparison_scenario[comparison_scenario])))
#         intersection_set = intersection_set.intersection(subsystem_results_by_comparison_scenario[comparison_scenario])
#     print('Mutual subsystems: ' + str(len(intersection_set)))
#
#
# def print_box_plot_data(subsystem_results):
#     print('subsystem,versions,an_type,cm_type,proportion')
#     for subsystem_result in subsystem_results:
#         comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
#         short_comparison_name = get_comparison_scenario_short_name(comparison_scenario)
#         sum_of_cm_changes = sum(np.sum(subsystem_result['table'], axis=0)[1:-1])
#         if sum_of_cm_changes != 0:
#             for row in range(len(change_types)):
#                 for column in range(1, len(change_types)):
#                     proportion = subsystem_result['table'][row][
#                                      column] / sum_of_cm_changes  # subsystem_result['ao_methods']
#                     print(
#                         '{},{},{},{},{}'.format(subsystem_result['subsystem'], short_comparison_name, change_types[row],
#                                                 change_types[column], proportion))
#
#
#
#
# def average_stats(subsystem_results):
#     aggregated_tables = dict()
#     for comparison_scenario in comparison_scenarios:
#         aggregated_tables[comparison_scenario] = np.zeros((5, 5), dtype=np.int)
#
#     for subsystem_result in subsystem_results:
#         comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
#         aggregated_tables[comparison_scenario] += np.matrix(subsystem_result['table'])[:, :-1]
#
#     id_body_proportion = list()
#     body_body_proportion = list()
#     for comparison_scenario, aggregated_table in aggregated_tables.items():
#         # Don't count deleted-deleted
#         aggregated_table[4, 4] = 0
#         sum_of_cm_changes = aggregated_table[:, 1:].sum()
#         id_body_proportion.append(aggregated_table[0, 3] / sum_of_cm_changes)
#         body_body_proportion.append(aggregated_table[3, 3] / sum_of_cm_changes)
#
#     print('ID_BODY: Average: {}, Mean: {}'.format(stats.mean(id_body_proportion), stats.median(id_body_proportion)))
#     print(
#         'BODY_BODY: Average: {}, Mean: {}'.format(stats.mean(body_body_proportion), stats.median(body_body_proportion)))
#


def print_most_changed_sub(results, row, column):
    for project in results.keys():
        print(project + ":")
        subsystem_results_by_comparison_scenario = dict()

        max_sub_count = 0
        for subsystem_result in results[project]:
            comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'],
                                                    subsystem_result['mo_version'])

            changes_table = subsystem_result['table']
            if changes_table[row][column] > max_sub_count and subsystem_result['subsystem'] != 'packages_apps_Bluetooth':
                max_sub_count = changes_table[row][column]
                max_cs = short_cs_names[comparison_scenario]
                max_subsystem = subsystem_result

    print('{}, {}, {}'.format(max_cs, max_subsystem['subsystem'], max_sub_count))


def run():
    # pretty_print_cell_colors()

    results = read_data()

    # print_colours_stat(results)
    print_bar_chart_data(results)
    # print_trends_plot_data(results)
    # print_heat_map(results)
    # extract_most_changed_subsystems(results)
    # print_most_changed_sub(results, 10, 10)
    # print_subsystems_stats(results)
    # print_box_plot_data(results)
    # average_stats(results)


if __name__ == '__main__':
    run()
