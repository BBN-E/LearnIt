import os,sys
from collections import defaultdict

def cleanSlot(txt):
    return txt.lower()\
        .replace(".","")\
        .replace("'"," '")\
        .replace("-"," - ")\
        .replace("&","&amp;")\
        .replace("<","&lt;")\
        .replace(">","&gt;")\
        .replace('"',"&quot;")\
        .replace("'","&apos;")

def readPairs(fn):
    contents = defaultdict(list)
    fin = open(fn,'r')
    for l in fin:
        parts = l.strip().split('\t')
        contents['kbp_'+parts[0]].append([cleanSlot(parts[1]),cleanSlot(parts[2])])
    fin.close()
    return contents

def writeRelation(name, seeds):
    fout = open(name+".seeds.xml",'w')
    fout.write('<?xml version="1.0" encoding="UTF-8"?>\n')
    fout.write('<seeds target="'+name.replace("kbp_","")+\
               '" source="slot-filling" iteration="0" '+\
               'score="1" confidence="1" active="True">\n')
    for s in seeds:
        fout.write('\t<seed slot0="'+s[0].lower()+'" slot1="'+s[1].lower()+'" />\n')
    fout.write("</seeds>\n")
    fout.close()

contents = readPairs('seed_pairs_for_learnit_from_sf_2012_and_2013_assessment.uniq')

for rel in contents:
    writeRelation(rel, contents[rel])

