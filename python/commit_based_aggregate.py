import re
import os
import numpy as np


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
    path = './results_commitbased/'
    files = [path + f for f in os.listdir(path) if os.path.isfile(path + f) and f[-3:] == 'csv']

    results = dict()
    for file in files:
        file_name = file[file.rfind('/') + 1:]
        groups = re.split(r'^(.+)-(.+)-([\da-fA-F]+)-(n?c).csv$', file_name)
        auto_merge = groups[4]

        with open(file) as input_stream:
            lines = [line.strip() for line in input_stream.readlines()]
            if is_right_format(lines):
                new_result = dict()
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
                new_result['table'] = np.array(table)

                if auto_merge not in results:
                    results[auto_merge] = dict()
                    results[auto_merge]['ao_methods'] = 0
                    results[auto_merge]['an_methods'] = 0
                    results[auto_merge]['mo_methods'] = 0
                    results[auto_merge]['an_new_methods'] = 0
                    results[auto_merge]['mo_new_methods'] = 0
                    results[auto_merge]['mutual_new_methods'] = 0
                    results[auto_merge]['mutual_new_methods_duplicates'] = 0
                    results[auto_merge]['table'] = np.zeros([12, 13], dtype=int)

                results[auto_merge]['ao_methods'] += new_result['ao_methods']
                results[auto_merge]['an_methods'] += new_result['an_methods']
                results[auto_merge]['mo_methods'] += new_result['mo_methods']
                results[auto_merge]['an_new_methods'] += new_result['an_new_methods']
                results[auto_merge]['mo_new_methods'] += new_result['mo_new_methods']
                results[auto_merge]['mutual_new_methods'] += new_result['mutual_new_methods']
                results[auto_merge]['mutual_new_methods_duplicates'] += new_result['mutual_new_methods_duplicates']
                results[auto_merge]['table'] += new_result['table']

    return results


results = read_data()
for result_key in results:
    print(result_key)
    result = results[result_key]
    print('{},{},{}'.format(result['ao_methods'], result['an_methods'], result['mo_methods']))
    print('IDENTICAL,REFACTORED_MOVE,REFACTORED_RENAME,REFACTORED_INLINE,REFACTORED_EXTRACT,REFACTORED_ARGUMENTS_RENAME,REFACTORED_ARGUMENTS_REORDER,ARGUMENTS_CHANGE_ADD,ARGUMENTS_CHANGE_REMOVE,ARGUMENTS_CHANGE_TYPE_CHANGE,BODY_CHANGE_ONLY,NOT_FOUND')
    for row in result['table'].tolist():
        print(*row)
    print('New methods in new AOSP,New methods in cm,New methods in both')
    print('{},{},{}'.format(result['an_new_methods'], result['mo_new_methods'], result['mutual_new_methods'] + result['mutual_new_methods_duplicates']))
