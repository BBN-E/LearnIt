from __future__ import print_function

import codecs
import os
import sys
from collections import Counter


def main():
    print('Reading various .decoder.output.relations files from ' + sys.argv[1])
    files = os.listdir(sys.argv[1])
    output_file = open(sys.argv[2],'w')
    files = [os.path.join(sys.argv[1],f) for f in files if f.endswith('.decoder.output.relations')]
    counter = Counter()
    for f in files:
        for l in codecs.open(f,'r','utf-8').readlines():
            rel_name = l.split("\t")[0]
            counter[rel_name]+=1
   
    output_file.write("RelationName\tCount\tCoverage\n")
    total_count = sum(counter.values())
    for (rel_name,count) in counter.most_common():
        output_file.write("\t".join([str(rel_name),str(count),str(float(count)/float(total_count))])+"\n")
    output_file.close()
    print('Done! Scoring output written to ' + sys.argv[2])
	
if __name__ == '__main__':
    main()
