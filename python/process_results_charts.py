import os
import re
import numpy as np
import copy
import statistics as stats

comparison_scenarios = ['android-4.2_r1,android-4.3_r1,cm-10.1',
                        'android-4.3_r1,android-4.4_r1,cm-10.2',
                        'android-4.4_r1,android-5.0.0_r1,cm-11.0',
                        'android-5.0.0_r1,android-5.1.0_r1,cm-12.0',
                        'android-5.1.0_r1,android-6.0.0_r1,cm-12.1',
                        'android-6.0.0_r1,android-7.0.0_r1,cm-13.0',
                        'android-7.0.0_r1,android-7.1.0_r1,cm-14.0']

comparison_scenario_short_names = {'android-4.2_r1,android-4.3_r1,cm-10.1': 'CS1',
                                   'android-4.3_r1,android-4.4_r1,cm-10.2': 'CS2',
                                   'android-4.4_r1,android-5.0.0_r1,cm-11.0': 'CS3',
                                   'android-5.0.0_r1,android-5.1.0_r1,cm-12.0': 'CS4',
                                   'android-5.1.0_r1,android-6.0.0_r1,cm-12.1': 'CS5',
                                   'android-6.0.0_r1,android-7.0.0_r1,cm-13.0': 'CS6',
                                   'android-7.0.0_r1,android-7.1.0_r1,cm-14.0': 'CS7'}

change_types = ['identical', 'refactored', 'argument', 'body', 'deleted']


def is_right_format(lines):
    if len(lines) != 7:
        return False

    if len(lines[0].split(',')) != 3:
        return False

    for i in range(1, 6):
        if len(lines[i].split(',')) != 6:
            return False

    return True


def get_comparison_scenario_short_name(comparison_scenario):
    return comparison_scenario_short_names[comparison_scenario]
    # comparison_scenario_name = re.sub(r'^android-(.+)_r.+,android-(.+)_r.+,cm-(\d+\.\d)$',
    #                                   r'A\1_A\2_CM\3',
    #                                   comparison_scenario)
    # return comparison_scenario_name


def determine_table_color(row, column):
    cell_colors = [['n', 'g', 'g', 'g', 'r', 'n'],
                   ['n', 'y', 'y', 'y', 'r', 'n'],
                   ['n', 'y', 'r', 'y', 'r', 'n'],
                   ['n', 'y', 'y', 'y', 'r', 'n'],
                   ['n', 'r', 'r', 'r', 'n', 'n']]
    return cell_colors[row][column]


def print_bar_chart_data(results):
    bar_chart_result = dict()
    for comparison_scenario in comparison_scenarios:
        bar_chart_result[comparison_scenario] = dict()

    for result in results:
        comparison_scenario = '{},{},{}'.format(result['ao'], result['an'], result['cm'])
        if comparison_scenario in comparison_scenarios:  # Changes this
            table = result['table']
            for row in range(0, len(table)):
                for column in range(0, len(table[row])):
                    count = table[row][column]
                    cell_color = determine_table_color(row, column)
                    bar_chart_result[comparison_scenario][cell_color] = bar_chart_result[comparison_scenario].get(
                        cell_color, 0) + count

    three_colors_list = dict()
    three_colors_list['g'] = list()
    three_colors_list['r'] = list()
    three_colors_list['y'] = list()

    colors_list = ['g', 'r', 'y']
    for comparison_scenario in bar_chart_result:
        sum = 0
        for color in colors_list:
            sum += bar_chart_result[comparison_scenario].get(color, 0)

        for color in colors_list:
            this_color_count = bar_chart_result[comparison_scenario].get(color, 0)
            bar_chart_result[comparison_scenario][color] = dict()
            bar_chart_result[comparison_scenario][color]['count'] = this_color_count
            bar_chart_result[comparison_scenario][color]['proportion'] = this_color_count / sum
            three_colors_list[color].append(this_color_count / sum)

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
            comparison_scenario_name = get_comparison_scenario_short_name(comparison_scenario)
            label_y = proportion / 2
            if color == 'r':
                label_y += (1 - proportion)
            elif color == 'g':
                label_y += bar_chart_result[comparison_scenario]['y']['proportion']
            print('{},{},{},{},{}'.format(comparison_scenario_name, color_labels[color], proportion, count, label_y))


def read_data():
    path = './results/'
    files = [path + f for f in os.listdir(path) if os.path.isfile(path + f) and f[-3:] == 'csv']

    results = list()
    for file in files:
        file_name = file[file.rfind('/') + 1:]
        groups = re.split(r'(^.+)_(android-.+_r\d+)_(android-.+_r\d+)_(cm-.+).csv$', file_name)
        if len(groups) == 6:
            new_result = dict()
            new_result['subsystem'] = groups[1]
            new_result['ao'] = groups[2]
            new_result['an'] = groups[3]
            new_result['cm'] = groups[4]
            with open(file) as input_stream:
                lines = [line.strip() for line in input_stream.readlines()]
                if is_right_format(lines):
                    (new_result['ao_methods'], new_result['an_methods'], new_result['cm_methods']) = \
                        (int(number) for number in lines[0].split(','))
                    (new_result['an_new_methods'], new_result['cm_new_methods'], new_result['mutual_new_methods']) = \
                        (number for number in lines[6].split(','))
                    new_result['an_new_methods'] = int(new_result['an_new_methods'])
                    new_result['cm_new_methods'] = int(new_result['cm_new_methods'])
                    [new_result['mutual_new_methods'], new_result['mutual_new_methods_duplicates']] = \
                        [int(number) for number in re.split('(\d+)\((\d+)\)', new_result['mutual_new_methods'])[1:3]]
                    table = list()
                    for line in lines[1:6]:
                        table_line = list()
                        for item in line.split(','):
                            if '(' in item:
                                item = re.split(r'(\d+)\(\d+\)', item)[1]
                            table_line.append(int(item))
                        table.append(table_line)
                    new_result['table'] = table
                    results.append(new_result)
    return results


def extract_most_changed_subsystems(subsystem_results):
    subsystem_results_by_comparison_scenario = dict()
    for comparison_scenario in comparison_scenarios:
        subsystem_results_by_comparison_scenario[comparison_scenario] = list()

    for subsystem_result in subsystem_results:
        comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
        changes_table = subsystem_result['table']
        sum_of_columns = np.sum(changes_table, axis=0)
        sum_of_cm_changes = sum(sum_of_columns[1:-1])
        sum_of_an_changes = sum([l[-1] for l in changes_table[1:]])
        new_subsystem_entry = {'subsystem': subsystem_result['subsystem'],
                               'sum_of_cm_changes': sum_of_cm_changes,
                               'sum_of_an_changes': sum_of_an_changes}
        subsystem_results_by_comparison_scenario[comparison_scenario].append(new_subsystem_entry)

    for comparison_scenario in subsystem_results_by_comparison_scenario:
        top_k = 5
        unsorted_subsystems = subsystem_results_by_comparison_scenario[comparison_scenario]
        an_sorted_subsystems_dict = sorted(unsorted_subsystems, key=lambda k: k['sum_of_an_changes'],
                                           reverse=True)[:top_k]
        cm_sorted_subsystems_dict = sorted(unsorted_subsystems, key=lambda k: k['sum_of_cm_changes'],
                                           reverse=True)[:top_k]
        an_sorted_subsystems_set = {x['subsystem'] for x in an_sorted_subsystems_dict}
        cm_sorted_subsystems_set = {x['subsystem'] for x in cm_sorted_subsystems_dict}
        subsystems_info = {x['subsystem']: (str(x['sum_of_an_changes']), str(x['sum_of_cm_changes'])) for x in
                           unsorted_subsystems}
        print(comparison_scenario)
        for i in range(top_k):
            suffix = ''
            if i == top_k - 1:
                suffix = ' \hline'
            an_subsystem = an_sorted_subsystems_dict[i]['subsystem']
            cm_subsystem = cm_sorted_subsystems_dict[i]['subsystem']
            if an_subsystem in cm_sorted_subsystems_set:
                an_subsystem = '\\textbf{' + an_subsystem + '}'
            if cm_subsystem in an_sorted_subsystems_set:
                cm_subsystem = '\\textbf{' + cm_subsystem + '}'
            an_subsystem = an_subsystem + ' (' + subsystems_info[an_sorted_subsystems_dict[i]['subsystem']][0] + ')'
            cm_subsystem = cm_subsystem + ' (' + subsystems_info[cm_sorted_subsystems_dict[i]['subsystem']][1] + ')'
            print(' & {} & {} \\\\{}'.format(an_subsystem.replace('_', '\_'),
                                             cm_subsystem.replace('_', '\_'),
                                             suffix))


def print_subsystems_stats(subsystem_results):
    subsystem_results_by_comparison_scenario = dict()
    for comparison_scenario in comparison_scenarios:
        subsystem_results_by_comparison_scenario[comparison_scenario] = set()

    for subsystem_result in subsystem_results:
        comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
        subsystem_results_by_comparison_scenario[comparison_scenario].add(subsystem_result['subsystem'])

    intersection_set = copy.copy(subsystem_results_by_comparison_scenario[comparison_scenarios[0]])
    for comparison_scenario in comparison_scenarios:
        print(comparison_scenario + ': ' + str(len(subsystem_results_by_comparison_scenario[comparison_scenario])))
        intersection_set = intersection_set.intersection(subsystem_results_by_comparison_scenario[comparison_scenario])
    print('Mutual subsystems: ' + str(len(intersection_set)))


def print_box_plot_data(subsystem_results):
    print('subsystem,versions,an_type,cm_type,proportion')
    for subsystem_result in subsystem_results:
        comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
        short_comparison_name = get_comparison_scenario_short_name(comparison_scenario)
        sum_of_cm_changes = sum(np.sum(subsystem_result['table'], axis=0)[1:-1])
        if sum_of_cm_changes != 0:
            for row in range(len(change_types)):
                for column in range(1, len(change_types)):
                    proportion = subsystem_result['table'][row][
                                     column] / sum_of_cm_changes  # subsystem_result['ao_methods']
                    print(
                        '{},{},{},{},{}'.format(subsystem_result['subsystem'], short_comparison_name, change_types[row],
                                                change_types[column], proportion))


def print_trends_plot_data(subsystem_results):
    colors = {'r': '#EA0D30', 'g': '#48D373', 'y': '#FCC168', 'n': '#48D373'}
    aggregated_tables = dict()
    for comparison_scenario in comparison_scenarios:
        aggregated_tables[comparison_scenario] = np.zeros((5, 5), dtype=np.int)

    # max_cat = (0, '')
    for subsystem_result in subsystem_results:
        comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
        aggregated_tables[comparison_scenario] += np.matrix(subsystem_result['table'])[:, :-1]

        # if comparison_scenario == comparison_scenarios[0]:
        #     if max_cat[0] < subsystem_result['table'][3][3]:
        #         max_cat = (subsystem_result['table'][3][3], subsystem_result['subsystem'])

    print('versions,an_type,cm_type,proportion,count,color')
    for comparison_scenario, aggregated_table in aggregated_tables.items():
        short_comparison_name = get_comparison_scenario_short_name(comparison_scenario)
        # Don't count deleted-deleted
        aggregated_table[4, 4] = 0
        sum_of_cm_changes = aggregated_table[:, 1:].sum()
        # print('sum of cm changes for {}: {}'.format(short_comparison_name, sum_of_cm_changes))
        for row in range(aggregated_table.shape[0]):
            for column in range(1, aggregated_table.shape[1]):
                proportion = aggregated_table[row, column] / sum_of_cm_changes
                print('{},{},{},{},{},{}'.format(short_comparison_name, change_types[row], change_types[column],
                                                 proportion, aggregated_table[row, column],
                                                 colors[determine_table_color(row, column)]))


def average_stats(subsystem_results):
    aggregated_tables = dict()
    for comparison_scenario in comparison_scenarios:
        aggregated_tables[comparison_scenario] = np.zeros((5, 5), dtype=np.int)

    for subsystem_result in subsystem_results:
        comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
        aggregated_tables[comparison_scenario] += np.matrix(subsystem_result['table'])[:, :-1]

    id_body_proportion = list()
    body_body_proportion = list()
    for comparison_scenario, aggregated_table in aggregated_tables.items():
        # Don't count deleted-deleted
        aggregated_table[4, 4] = 0
        sum_of_cm_changes = aggregated_table[:, 1:].sum()
        id_body_proportion.append(aggregated_table[0, 3] / sum_of_cm_changes)
        body_body_proportion.append(aggregated_table[3, 3] / sum_of_cm_changes)

    print('ID_BODY: Average: {}, Mean: {}'.format(stats.mean(id_body_proportion), stats.median(id_body_proportion)))
    print(
        'BODY_BODY: Average: {}, Mean: {}'.format(stats.mean(body_body_proportion), stats.median(body_body_proportion)))


def print_most_changed_sub(subsystem_results, row, column):
    aggregated_tables = dict()
    for comparison_scenario in comparison_scenarios:
        aggregated_tables[comparison_scenario] = np.zeros((5, 5), dtype=np.int)

    # max_cat = (0, '')
    for subsystem_result in subsystem_results:
        comparison_scenario = '{},{},{}'.format(subsystem_result['ao'], subsystem_result['an'], subsystem_result['cm'])
        aggregated_tables[comparison_scenario] += np.matrix(subsystem_result['table'])[:, :-1]


def run():
    results = read_data()

    # print_bar_chart_data(results)
    # extract_most_changed_subsystems(results)
    # print_subsystems_stats(results)
    # print_box_plot_data(results)
    print_trends_plot_data(results)
    # average_stats(results)


if __name__ == '__main__':
    run()
