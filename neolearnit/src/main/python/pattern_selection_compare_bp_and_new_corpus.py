import sys, os
import random

from collections import defaultdict
from collections import Counter

# number of types to print out
num_types_printout = 3
min_conf=0.95

def generate_type_dist(input_pattern_freq_file, min_freq, output_file):
    # pattern to type distribution
    pt2type_counter = dict()

    with open(input_pattern_freq_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                items = line.split('\t')

                count = int(items[0][:items[0].index(" ")])
                type = items[1]
                pattern = items[2]

                if count >= int(min_freq):
                    print(str(count) + "\t" + type + "\t" + pattern)
                    if pattern not in pt2type_counter:
                        pt2type_counter[pattern] = Counter()
                    type_counter = pt2type_counter[pattern]
                    type_counter[type] += count
                else:
                    break

    # serialize type counter
    with open(output_file, "w") as o:

        for pt in pt2type_counter:
            type_counter = pt2type_counter[pt]
            str_tranked_type_list = get_ranked_type_list(type_counter, num_types_printout)
            print(str_tranked_type_list + "\t" + pt)

            o.write(str_tranked_type_list + "\t" + pt + "\n")

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

def parse_string_to_counter(type_count_tuple):
    type_counter = Counter()

    # items = type_count_tuple.split("\t")
    num_types = int(len(type_count_tuple)/3)
    total_count = 0
    for i in range(0,num_types):
        type = type_count_tuple[3*i]
        count = int(type_count_tuple[3*i+1])
        percentage = float(type_count_tuple[3*i+2])

        type_counter[type]+=percentage
        total_count+=count

    return total_count, type_counter

def read_pt2type_counter(input_file_pt2type_counter):
    # pattern to type distribution
    pt2type_counter = dict()
    pt2total_count = dict()

    with open(input_file_pt2type_counter, 'r') as f:
        for line in f:
            line = line.strip()
            if line:
                items = line.split('\t')

                type_counter = Counter()

                pattern = items[-1]
                type_count_tuple = items[0:-1]

                # print(pattern + "\t" + type_count_tuple)

                total_count, type_counter = parse_string_to_counter(type_count_tuple)
                pt2type_counter[pattern] = type_counter
                pt2total_count[pattern] = total_count
    return pt2type_counter, pt2total_count



def get_best_type(type_counter):
    type_count_pair=type_counter.most_common(1)[0]
    ev_type = type_count_pair[0]
    conf = float(type_count_pair[1])
    if ev_type != "Other" and conf >= min_conf:
        return ev_type
    else:
        return None

if __name__ == "__main__":
    str_min_conf, bp_input_pattern_freq_file, bp_type_dist_file, bp_min_freq, new_corpus_input_pattern_freq_file, new_corpus_type_dist_file, new_corpus_min_freq, output_file = sys.argv[1:]

    min_conf = float(str_min_conf)

    # generate pattern type distributions
    generate_type_dist(bp_input_pattern_freq_file, bp_min_freq, bp_type_dist_file)
    generate_type_dist(new_corpus_input_pattern_freq_file, new_corpus_min_freq, new_corpus_type_dist_file)

    # reads into memory
    bp_pt2type_counter, bp_pt2total_count = read_pt2type_counter(bp_type_dist_file)
    ir700k_pt2type_counter, ir700k_pt2total_count = read_pt2type_counter(new_corpus_type_dist_file)

    # print out diff
    with open(output_file, "w") as o:
        for pattern in ir700k_pt2type_counter:
            if pattern not in bp_pt2type_counter:
                total_count = ir700k_pt2total_count[pattern]
                best_type = get_best_type(ir700k_pt2type_counter[pattern])
                if best_type is not None:
                    print (str(total_count) + "\t" + pattern + "\t" + best_type)
                    o.write(str(total_count) + "\t" + pattern + "\t" + best_type + "\n")
