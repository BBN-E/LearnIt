import sys, os
import random

from collections import defaultdict
from collections import Counter

# pattern to type distribution
pt2type_counter = dict()
# number of types to print out
num_types_printout = 3

def process_triple(count, type, pattern):
    print(str(count) + "\t" + type + "\t" + pattern)
    if pattern not in pt2type_counter:
        pt2type_counter[pattern] = Counter()
    type_counter = pt2type_counter[pattern]
    type_counter[type]+=count

def read_input_freq_file(input_pattern_freq_file, min_freq):
    with open(input_pattern_freq_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                items = line.split('\t')

                count = int(items[0][:items[0].index(" ")])
                type = items[1]
                pattern = items[2]

                if count >= int(min_freq):
                    process_triple(count, type, pattern)
                else:
                    return

def get_ranked_type_list(type_counter, num_types_printout):
    all_count=0
    for type in type_counter:
        all_count += type_counter[type]

    str_tranked_type_list=""
    for type_count_pair in type_counter.most_common(num_types_printout):
        type = type_count_pair[0]
        count = type_count_pair[1]
        str_tranked_type_list = str_tranked_type_list + type + "\t" + str(count) + "\t" + str(count*1.0/all_count) + "\t"

    return str_tranked_type_list.strip()

def print_type_counter(output_file):
    with open(output_file, "w") as o:

        for pt in pt2type_counter:
            type_counter = pt2type_counter[pt]
            str_tranked_type_list = get_ranked_type_list(type_counter, num_types_printout)
            print(str_tranked_type_list + "\t" + pt)

            o.write(str_tranked_type_list + "\t" + pt + "\n")

def main(input_pattern_freq_file, output_file, min_freq):
    read_input_freq_file(input_pattern_freq_file, min_freq)

    print_type_counter(output_file)

if __name__ == "__main__":
    input_pattern_freq_file, output_file, min_freq = sys.argv[1:]

    main(input_pattern_freq_file, output_file, min_freq)
