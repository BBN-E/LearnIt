import sys, os, re, codecs, json, glob
import random
from random import randint

from collections import defaultdict
from collections import Counter

MAX_NUM_INSTANCE_PER_TRIPLE = 100
# NEGATIVE_SAMPLING_RATIO = 0.25
NEGATIVE_SAMPLING_RATIO = 1.0

rm_to_sampling_ratio = defaultdict()
counter_rm = Counter()
relation_mentions = []

def read_json_data(strJsonFile):
    with codecs.open(strJsonFile, 'r', encoding='utf-8') as f:
        try:
            json_data = json.load(f)
        except ValueError as ve:
            print "While loading: " + filename
            print str(ve)
            sys.exit(1)

    return json_data

def generate_triple_count(json_data):
    for relation_mention in json_data:
        if relation_mention is not None:
            head = relation_mention['head']
            tail = relation_mention['tail']
            relation = relation_mention['relation']

            if relation=='NA':
                continue

            head_id = head['id']
            tail_id = tail['id']

            triple_id = head_id + "-" + relation + "-" + tail_id
            counter_rm.update({triple_id : 1})

def generate_triple_sampling_ratio():
    num_keys=len(counter_rm.keys())
    idx=0
    for triple_id in counter_rm.keys():
        if idx%10000==0:
            print "generate triple sampling ratio " + str(idx) + " / " + str(num_keys)
        count = counter_rm[triple_id]
        rm_to_sampling_ratio[triple_id] = MAX_NUM_INSTANCE_PER_TRIPLE*1.0/count
        idx=idx+1

def sample_triples(json_data):
    type_to_relation_mentions = defaultdict()
    for relation_mention in json_data:
        if relation_mention is not None:
            head = relation_mention['head']
            tail = relation_mention['tail']
            relation = relation_mention['relation']

            head_id = head['id']
            tail_id = tail['id']
            triple_id = head_id + "-" + relation + "-" + tail_id

            if relation not in type_to_relation_mentions:
                type_to_relation_mentions[relation] = []

            type_to_relation_mentions[relation].append(relation_mention)

    for relation in type_to_relation_mentions:
        if relation == "NA":
            sampling_ratio = NEGATIVE_SAMPLING_RATIO
        else:
            if triple_id not in rm_to_sampling_ratio:
                print "Skip because not found in rm_to_sampling_ratio: " + str(triple_id)
                sampling_ratio = 0.0
            else:
                sampling_ratio = rm_to_sampling_ratio[triple_id]
        rms = type_to_relation_mentions[relation]
        random.shuffle(rms)
        relation_mentions.extend(rms[0:int(sampling_ratio*len(rms))])

    return relation_mentions

def main(list_input_json, output_json):
    f = open(list_input_json, "r")
    o = codecs.open(output_json, 'w', encoding='utf8')

    # generate sampling ratio
    idx=0
    for line in f.readlines():
        line = line.strip()
        print "counting " + str(idx) + " " + line
        json_data = read_json_data(line)
        generate_triple_count(json_data)
        idx=idx+1
    generate_triple_sampling_ratio()
    f.close()

    # sample instances
    idx=0
    f = open(list_input_json, "r")
    for line in f.readlines():
        line = line.strip()
        print "sampling " + str(idx) + " " + line
        json_data = read_json_data(line)
        sample_triples(json_data)
        idx=idx+1
    print "writing json..."
    f.close()
    o.write(json.dumps(relation_mentions, sort_keys=True, indent=4, cls=json.JSONEncoder, ensure_ascii=False))
    o.close()

if __name__ == "__main__":
    list_input_json, output_json = sys.argv[1:]

    main(list_input_json, output_json)