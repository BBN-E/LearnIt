import os,sys,json



def my_tokenizer(opennre_json):
    resolved_token = list()
    tokenized_arr = opennre_json['sentence'].split(" ")
    assert " ".join(tokenized_arr[opennre_json['slot0StartIdx']:opennre_json['slot0EndIdx']+1]) == opennre_json['head']['word']
    assert " ".join(tokenized_arr[opennre_json['slot1StartIdx']:opennre_json['slot1EndIdx']+1]) == opennre_json['tail']['word']
    for idx,token in enumerate(tokenized_arr):
        current_str = token.replace("\n"," ").replace("\r"," ")
        if idx == opennre_json['slot0StartIdx']:
            current_str = "<span class=\"slot0\">" + current_str
        if idx == opennre_json['slot1StartIdx']:
            current_str = "<span class=\"slot1\">" + current_str
        if idx == opennre_json['slot0EndIdx']:
            current_str = current_str + "</span>"
        if idx == opennre_json['slot1EndIdx']:
            current_str = current_str + "</span>"
        resolved_token.append(current_str)
    return " ".join(resolved_token)

def print_single_json(file_path):
    with open(file_path) as fp:
        en = json.load(fp)
    for i in en:
        sentence_text = my_tokenizer(i)
        if i['semantic_class'] != "NA":
            print("{}:{}\t{}".format(i['semantic_class'],i['confidence'],sentence_text))

if __name__ == "__main__":
    root_folder = "/home/hqiu/ld100/Hume_pipeline/Hume/expts/wm_dart.090419/event_event_relations/mappings"
    for root,dirs,files in os.walk(root_folder):
        for file in files:
            if file == "bag_predictions.json":
                print_single_json(os.path.join(root,file))