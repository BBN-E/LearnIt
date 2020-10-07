import sys, os
import random

from collections import defaultdict
from collections import Counter

min_conf=0.95

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

def main(bp_pt2type_counter, ir700k_pt2type_counter, output_file):
    bp_pt2type_counter, bp_pt2total_count = read_pt2type_counter(bp_pt2type_counter)
    ir700k_pt2type_counter, ir700k_pt2total_count = read_pt2type_counter(ir700k_pt2type_counter)

    with open(output_file, "w") as o:
        for pattern in ir700k_pt2type_counter:
            if pattern not in bp_pt2type_counter:
                total_count = ir700k_pt2total_count[pattern]
                best_type = get_best_type(ir700k_pt2type_counter[pattern])
                if best_type is not None:
                    print (str(total_count) + "\t" + pattern + "\t" + best_type)
                    o.write(str(total_count) + "\t" + pattern + "\t" + best_type + "\n")

if __name__ == "__main__":
    bp_pt2type_counter, ir700k_pt2type_counter, output_file = sys.argv[1:]

    main(bp_pt2type_counter, ir700k_pt2type_counter, output_file)
