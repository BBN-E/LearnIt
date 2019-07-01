import sys, os, re, codecs, json, glob
import random
from random import randint

from collections import defaultdict
from collections import Counter
from sets import Set

rm_to_sampling_ratio = defaultdict()
triple_set = Set()
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

#def generate_triple_set(json_data):
#    for relation_mention in json_data:
#        if relation_mention is not None:
#            head = relation_mention['head']
#            tail = relation_mention['tail']
#            relation = relation_mention['relation']
#            sentence = relation_mention['sentence']
#
#            head_id = head['id']
#            tail_id = tail['id']
#
#            triple_id = head_id + "-" + relation + "-" + tail_id + "-" + sentence
#            triple_set.add(triple_id)

def generate_triples(json_data):
    for relation_mention in json_data:
        if relation_mention is not None:
            head = relation_mention['head']
            tail = relation_mention['tail']
            relation = relation_mention['relation']
            sentence = relation_mention['sentence']

            head_id = head['id']
            tail_id = tail['id']
            triple_id = head_id + "-" + relation + "-" + tail_id + "-" + sentence

            if triple_id not in triple_set:
                relation_mentions.append(relation_mention)
                triple_set.add(triple_id)
            
    return relation_mentions

def main(list_input_json, output_json):
    f = open(list_input_json, "r")
    o = codecs.open(output_json, 'w', encoding='utf8')

    idx=0
    for line in f.readlines():
        line = line.strip()
        print "counting " + str(idx) + " " + line
        json_data = read_json_data(line)
        generate_triples(json_data)
        idx=idx+1
    f.close()

    print "writing json..."
    f.close()
    o.write(json.dumps(relation_mentions, sort_keys=True, indent=4, cls=json.JSONEncoder, ensure_ascii=False))
    o.close()

if __name__ == "__main__":
    list_input_json, output_json = sys.argv[1:]

    main(list_input_json, output_json)
