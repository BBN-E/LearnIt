import os,sys


current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir))
sys.path.append(os.path.realpath(os.path.join(current_script_path, os.path.pardir)))

from document_preparation_toolkit.hume_corpus import make_sgm_file

def main(input_folder,output_folder):
    metadata_list = list()
    sgms_list = list()
    for root,dirs,files in os.walk(input_folder):
        for file in files:
            if file.endswith(".txt"):
                with open(os.path.join(root,file)) as fp:
                    make_sgm_file(fp.read(), file[:-4], output_folder, "NA", os.path.join(root,file),"ENG_NW_TMP", "UNKNOWN", "UNKNOWN", metadata_list,sgms_list)

    with open(os.path.join(output_folder,'sgms.list'),'w') as fp:
        for i in sgms_list:
            fp.write("{}\n".format(i))

    with open(os.path.join(output_folder,'metadata.txt'),'w') as fp:
        for i in metadata_list:
            fp.write("{}\n".format(metadata_list))

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--input_folder',required=True)
    parser.add_argument('--output_folder',required=True)
    args = parser.parse_args()
    main(args.input_folder,args.output_folder)