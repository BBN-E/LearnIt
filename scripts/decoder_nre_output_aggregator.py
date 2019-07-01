import json, os, codecs, sys

list_nre_dir = sys.argv[1]
output_file = sys.argv[2]
complete_list = []

def read_lines(in_file):
    with open(in_file) as f:
        content = f.readlines()
    lines = [x.strip() for x in content]
    return lines

nre_dirs = read_lines(list_nre_dir)

for nre_dir in nre_dirs:
    json_file=nre_dir + "/bag_predictions.json"
    print "Reading " + json_file
    json_objs=json.load(codecs.open(json_file,'r','utf-8'))
    for i in range(0,len(json_objs)):
        json_obj = json_objs[i]
        relation_type = json_obj["semantic_class"]
        if relation_type == "NA":
            continue

        complete_list.append(json_obj)

print "Writing output file to "+output_file
json.dump(complete_list,open(output_file,'w'),indent=4, separators=(',', ': '))
print "Done!"
