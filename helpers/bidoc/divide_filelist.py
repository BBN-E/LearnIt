import os,sys

def main(folder_path,suffix,output_path):
    care_fields = {"english","arabic","alignment"}
    care_fields_to_file_path_set = dict()
    for root,dirs,files in os.walk(folder_path):
        for file in files:
            with open(os.path.join(root,file)) as rfp:
                for i in rfp:
                    i = i.strip()
                    fields = i.split(" ")
                    entries = {i.split(":")[0]: i.split(":")[1] for i in fields}
                    for care_field in care_fields:
                        if entries.get(care_field,None) is not None:
                            care_fields_to_file_path_set.setdefault(care_field,set()).add(entries[care_field])
    os.makedirs(output_path,exist_ok=True)
    for care_field in care_fields_to_file_path_set.keys():
        with open(os.path.join(output_path,"{}{}.list".format(care_field,suffix)),'w') as wfp:
            for path in care_fields_to_file_path_set[care_field]:
                wfp.write("{}\n".format(path))
if __name__ == "__main__":
    folder_path = ""
    suffix = ""
    output_path = ""
    main(folder_path,suffix,output_path)