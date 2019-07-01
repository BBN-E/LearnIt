import sys, os, re, codecs, json, glob
import random
from random import randint

from collections import defaultdict
from collections import Counter

NEGATIVE_SAMPLING_RATIO = 0.005

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
            sampling_ratio = 1.0
        rms = type_to_relation_mentions[relation]
        random.shuffle(rms)
        relation_mentions.extend(rms[0:int(sampling_ratio*len(rms))])

    return relation_mentions

def main(input_json, output_json):
    o = codecs.open(output_json, 'w', encoding='utf8')

    # sample instances
    json_data = read_json_data(input_json)
    sample_triples(json_data)

    print "writing json..."
    o.write(json.dumps(relation_mentions, sort_keys=True, indent=4, cls=json.JSONEncoder, ensure_ascii=False))
    o.close()

if __name__ == "__main__":
    input_json, output_json = sys.argv[1:]

    main(input_json, output_json)
