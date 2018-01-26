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


path = './results/'
files = [path + f for f in os.listdir(path) if os.path.isfile(path + f) and f[-3:] == 'csv']
remove_counter = 0
for file in files:
    with open(file) as input_stream:
        lines = input_stream.readlines()
        if is_right_format(lines):
            versions_methods = lines[0].strip().split(',')
            if '0' in versions_methods:
                # os.remove(file)
                print("Removed {}".format(file))
                remove_counter += 1
print("Removed {} files in total".format(remove_counter))

