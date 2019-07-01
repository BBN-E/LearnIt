import json, os, codecs, sys

in_dir = sys.argv[1]
output_file = sys.argv[2]
complete_list = []
for filename in os.listdir(in_dir):
    if filename.endswith(".json"):
        print "Reading "+filename
        complete_list.extend(json.load(codecs.open(os.path.join(in_dir,filename),'r','utf-8')))
print "Writing output file to "+output_file
json.dump(complete_list,open(output_file,'w'),indent=4, separators=(',', ': '))
print "Done!"
