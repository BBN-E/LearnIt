import os,sys,json

def build_doc_id_to_path_map(filelist,suffix=""):
    ret = dict()
    with open(filelist) as fp:
        for i in fp:
            i = i.strip()
            doc_id = os.path.basename(i)
            doc_id = doc_id[:-len(suffix)]
            ret[doc_id] = i
    return ret

def main(source_list_path,target_list_path,alignment_list_path,output_path):
    doc_id_to_source_path = build_doc_id_to_path_map(source_list_path,".segments.xml")
    doc_id_to_target_path = build_doc_id_to_path_map(target_list_path,".segments.xml")
    doc_id_to_alignment_path = build_doc_id_to_path_map(alignment_list_path,".alignments")
    with open(output_path,'w') as wfp:
        for doc_id in doc_id_to_alignment_path.keys():
            entries = {}
            if doc_id not in doc_id_to_source_path:
                print("Missing {} in english list".format(doc_id,doc_id_to_alignment_path[doc_id]))
                continue
            if doc_id not in doc_id_to_target_path:
                print("Missing {} in arabic list".format(doc_id,doc_id_to_alignment_path[doc_id]))
                continue
            entries['english'] = doc_id_to_source_path[doc_id]
            entries['arabic'] = doc_id_to_target_path[doc_id]
            entries['alignment'] = doc_id_to_alignment_path[doc_id]
            entries['docid'] = doc_id
            wfp.write("{}\n".format(" ".join(":".join((k, v)) for k, v in entries.items())))

if __name__ == "__main__":
    source_list_path = ""
    target_list_path = ""
    alignment_list_path = ""
    output_path = ""
    main(source_list_path,target_list_path,alignment_list_path,output_path)