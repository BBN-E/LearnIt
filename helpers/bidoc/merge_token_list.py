import os,json


def main():
    input_folder = ""
    word_set = dict()
    output_path = ""
    for root,dirs,files in os.walk(input_folder):
        for file in files:
            with open(os.path.join(root,file)) as fp:
                j = json.load(fp)
                for word,cnt in j.items():
                    word_set[word] = word_set.get(word,0)+cnt
    with open(output_path,'w') as wfp:
        json.dump(word_set,wfp,indent=4,ensure_ascii=False)

if __name__ == "__main__":
    main()