import os
import sys


def main(source_mapping_list_dir):
    for root,dirs,files in os.walk(source_mapping_list_dir):
        for file in files:
            buffer = list()
            with open(os.path.join(root,file),'r') as fp:
                for i in fp:
                    i = i.strip()
                    if "mappings.master.sjson" in i:
                        continue
                    buffer.append(i)
            with open(os.path.join(root,file),'w') as fp:
                for i in buffer:
                    fp.write("{}\n".format(i))


if __name__ == "__main__":
    source_mapping_list_dir = sys.argv[1]
    main(source_mapping_list_dir)

