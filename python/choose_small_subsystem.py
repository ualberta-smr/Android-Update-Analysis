import re
import numpy as np
import os


def is_right_format(lines):
    if len(lines) != 16:
        return False

    if len(lines[0].split(',')) != 3:
        return False

    for i in range(2, 14):
        if len(lines[i].split(',')) != 13:
            return False

    return True


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


results = read_data()

cell_colors = [['n', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'r', 'n'],
               ['g', 'y', 'g', 'r', 'r', 'g', 'g', 'g', 'g', 'g', 'g', 'r', 'n'],
               ['g', 'g', 'y', 'r', 'g', 'g', 'g', 'g', 'g', 'g', 'g', 'r', 'n'],
               ['g', 'r', 'r', 'y', 'r', 'r', 'r', 'r', 'r', 'r', 'g', 'r', 'n'],
               ['g', 'r', 'g', 'r', 'y', 'g', 'g', 'r', 'r', 'r', 'r', 'r', 'n'],
               ['g', 'g', 'g', 'r', 'g', 'y', 'g', 'y', 'g', 'g', 'g', 'r', 'n'],
               ['g', 'g', 'g', 'r', 'g', 'g', 'y', 'r', 'r', 'g', 'g', 'r', 'n'],
               ['g', 'g', 'g', 'r', 'r', 'y', 'r', 'y', 'r', 'r', 'r', 'r', 'n'],
               ['g', 'g', 'g', 'r', 'r', 'g', 'r', 'r', 'y', 'r', 'r', 'r', 'n'],
               ['g', 'g', 'g', 'r', 'r', 'g', 'g', 'r', 'r', 'y', 'r', 'r', 'n'],
               ['g', 'g', 'g', 'g', 'r', 'g', 'g', 'r', 'r', 'r', 'y', 'r', 'n'],
               ['r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'r', 'n']]

total_count = 0
for subsystem_result in results['CM']:
    table_array = np.array(subsystem_result['table'])
    sum_of_all = table_array[:, :-1].sum()
    sum_of_non_id_id = sum_of_all - table_array[0, 0]
    sum_of_body_body = table_array[10, 10]
    sum_of_id_green_both = table_array[0, 1:-2].sum() + table_array[1:-1, 0].sum()
    sum_of_greens = 0

    for row in range(table_array.shape[0]):
        for col in range(table_array.shape[1]):
            if cell_colors[row][col] == 'g':
                sum_of_greens += table_array[row, col]

    sum_of_non_id_greens = sum_of_greens - sum_of_id_green_both
    if 1 < sum_of_non_id_greens:# and sum_of_non_id_id < 50:
        total_count += 1
        if subsystem_result['subsystem'] == 'packages_apps_Stk':
            print("fo")
        print(str(sum_of_non_id_greens) + "/" + str(sum_of_non_id_id) + "\t" + subsystem_result[
            'subsystem'] + ',' + subsystem_result['ao'] + ',' + subsystem_result['an'] + ',' + subsystem_result[
                  'mo_version'])
print("Total # of subsystems printed: " + str(total_count))
