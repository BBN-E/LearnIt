import sys, os, re, codecs, json, glob
import random
from random import randint

from collections import defaultdict
from collections import Counter
from sets import Set

words=Set()

def read_json_data(strJsonFile):
    with codecs.open(strJsonFile, 'r', encoding='utf-8') as f:
        try:
            json_data = json.load(f)
        except ValueError as ve:
            print "While loading: " + filename
            print str(ve)
            sys.exit(1)

    return json_data


def main(input_json):
    json_data = read_json_data(input_json)
    for j in json_data:
        for w in j['head']['word'].split(" "):
            words.add(w)
        for w in j['tail']['word'].split(" "):
            words.add(w)
        for w in j['sentence'].split(" "):
            words.add(w)

    for w in words:
        print w


    ## sample instances
    #print "writing json..."
    #f.close()
    #o.write(json.dumps(relation_mentions, sort_keys=True, indent=4, cls=json.JSONEncoder, ensure_ascii=False))
    #o.close()

if __name__ == "__main__":
    input_json = sys.argv[1]

    main(input_json)

