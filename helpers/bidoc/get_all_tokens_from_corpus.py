import os,json,multiprocessing

import serifxml3

def single_document_worker(serifxml_path):
    serif_doc = serifxml3.Document(serifxml_path)
    ret = dict()
    for sent in serif_doc.sentences:
        for token in sent.token_sequence:
            ret[token.text] = ret.get(token.text,0)+1
    return ret


def main(source_lists_folder,language,output_path):
    doc_id_to_entries = dict()
    if os.path.isdir(source_lists_folder):
        for root,dirs,files in os.walk(source_lists_folder):
            for file in files:
                with open(os.path.join(root,file)) as fp:
                    for i in fp:
                        i = i.strip()
                        entries = i.split(" ")
                        entries = {j.split(":")[0]:j.split(":")[1] for j in entries}
                        doc_id_to_entries[entries['docid']] = entries
    else:
        with open(source_lists_folder) as fp:
            for i in fp:
                i = i.strip()
                entries = i.split(" ")
                entries = {j.split(":")[0]:j.split(":")[1] for j in entries}
                doc_id_to_entries[entries['docid']] = entries
    manager = multiprocessing.Manager()
    ret = dict()
    with manager.Pool() as pool:
        workers = list()
        for entries in doc_id_to_entries.values():
            serif_path = entries[language]
            workers.append(pool.apply_async(single_document_worker,args=(serif_path,)))
        for idx,i in enumerate(workers):
            i.wait()
            for word,cnt in i.get().items():
                ret[word] = ret.get(word,0)+cnt
    with open(output_path,'w') as fp:
        json.dump(ret,fp,indent=4,ensure_ascii=False)

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--source_lists_folder",required=True)
    parser.add_argument("--language",required=True)
    parser.add_argument("--output_path",required=True)
    args = parser.parse_args()
    main(args.source_lists_folder,args.language,args.output_path)