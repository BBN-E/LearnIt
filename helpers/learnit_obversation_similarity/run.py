

import os,sys,glob,collections,copy

learnit_root = os.path.realpath(os.path.join(__file__,os.pardir,os.pardir,os.pardir))

sys.path.append(os.path.join(learnit_root,"helpers"))
sys.path.append(os.path.join(learnit_root,"scripts"))
import json
import logging
import numpy as np

import pickle
from bert_emb import BertEmbCache

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

MinimalInstanceIdentifier = collections.namedtuple('MinimalInstanceIdentifier',['docId','sentId','slot0Start','slot0End','slot1Start','slot1End'])

def list_spliter_by_batch_size(my_list, batch_size):
    return [my_list[i * batch_size:(i + 1) * batch_size] for i in range((len(my_list) + batch_size - 1) // batch_size)]


def list_spliter_by_num_of_batches(my_list, num_of_batches):
    batch_size = (len(my_list) + num_of_batches - 1) // num_of_batches
    if batch_size == 1 and len(my_list) < num_of_batches:
        raise ValueError(
            "You gave a very large number of batches which is not reachable. This program won't support empty batches")
    return list_spliter_by_batch_size(my_list, batch_size)


def merge_embedding_file(input_emb_list,output_prefix):
    merged_pattern_id_list = list()
    merged_emb_list = list()
    with open(input_emb_list) as fp:
        for i in fp:
            i = i.strip()
            with open(i,'rb') as embfp:
                [word_list, embeddings_list] = pickle.load(embfp)
                merged_pattern_id_list.extend(word_list)
                merged_emb_list.extend(embeddings_list)
    with open(output_prefix,'wb') as wfp:
        pickle.dump([merged_pattern_id_list,merged_emb_list], wfp)

def generate_docid_based_emb_extraction_list(learnitobversation_instances_json_path, num_of_batches, output_prefix):
    with open(learnitobversation_instances_json_path) as fp:
        entry_list = json.load(fp)
    doc_id_to_learnit_obversation_to_instances = dict()
    for learnitobversation_instances_entry in entry_list:
        instances = learnitobversation_instances_entry['chosenInstances']
        for instance in instances:
            doc_id = instance['docId']
            doc_id_to_learnit_obversation_to_instances.setdefault(doc_id,dict()).setdefault(learnitobversation_instances_entry['learnItObservation']['toIDString'],list()).append(learnitobversation_instances_entry)

    prune_doc_id_to_learnit_obversation_instance = dict()
    for doc_id,learnit_obversation_instances in doc_id_to_learnit_obversation_to_instances.items():
        for learnit_obversation_id,original_selected_ins in learnit_obversation_instances.items():
            prune_instances = dict()
            for original_selected_in in original_selected_ins:
                for instance in original_selected_in['chosenInstances']:
                    if instance['docId'] != doc_id:
                        continue
                    mini_instance_identifier = MinimalInstanceIdentifier(instance['docId'],instance['sentId'],instance['slot0Start'],instance['slot0End'],instance['slot1Start'],instance['slot1End'])
                    prune_instances[mini_instance_identifier] = instance
            new_learnit_obversation_ins = copy.deepcopy(original_selected_ins[0])
            new_learnit_obversation_ins['chosenInstances'] = list(prune_instances.values())
            prune_doc_id_to_learnit_obversation_instance.setdefault(doc_id,list()).append(new_learnit_obversation_ins)

    for batch_id,batches in enumerate(list_spliter_by_num_of_batches(list(prune_doc_id_to_learnit_obversation_instance.keys()),num_of_batches)):
        current_dir = os.path.join(output_prefix,str(batch_id))
        os.makedirs(current_dir,exist_ok=True)
        for doc_id in batches:
            with open(os.path.join(current_dir,"{}.json".format(doc_id)),'w') as fp:
                json.dump(prune_doc_id_to_learnit_obversation_instance[doc_id],fp,indent=4,sort_keys=True,ensure_ascii=False)

def further_down_sample_representating_instances_for_learnit_obversation(learnitobversation_instances_json_path_list,output_prefix,cutoff):
    assert isinstance(cutoff,int)
    id_to_minimal_instance_id_to_instances = dict()
    id_to_learnit_obversation = dict()
    id_to_aux = dict()
    with open(learnitobversation_instances_json_path_list) as fp:
        for i in fp:
            i = i.strip()
            with open(i) as fp2:
                entries = json.load(fp2)
                for entry in entries:
                    instances = entry['chosenInstances']
                    learnit_obversation = entry['learnItObservation']
                    aux = entry['aux']
                    id_string = learnit_obversation['toIDString']
                    for instance in instances:
                        doc_id = instance['docId']
                        sent_idx = instance['sentId']
                        slot0_start = instance['slot0Start']
                        slot0_end = instance['slot0End']
                        slot1_start = instance['slot1Start']
                        slot1_end = instance['slot1End']
                        id_to_minimal_instance_id_to_instances.setdefault(id_string,dict()).setdefault(MinimalInstanceIdentifier(doc_id,sent_idx,slot0_start,slot0_end,slot1_start,slot1_end),list()).append(instance)
                    id_to_learnit_obversation[id_string] = learnit_obversation
                    id_to_aux[id_string] = aux
    new_entries = list()
    for id_string in id_to_learnit_obversation.keys():
        instances = list()
        for ins_key in id_to_minimal_instance_id_to_instances[id_string].keys():
            instances.append(id_to_minimal_instance_id_to_instances[id_string][ins_key][0])
        learnit_obversation = id_to_learnit_obversation[id_string]
        aux = id_to_aux[id_string]
        new_entries.append({'learnItObservation':learnit_obversation,'chosenInstances':instances[:cutoff],'aux':aux})
    with open(output_prefix,'w') as fp:
        json.dump(new_entries,fp,indent=4,ensure_ascii=False,sort_keys=True)


def divide_learnit_obversation_list_into_batches(number_of_batches, input_file, output_prefix, suffix):
    with open(input_file) as fp:
        entry_list = json.load(fp)
    split_batches = list_spliter_by_num_of_batches(entry_list, number_of_batches)

    for idx, i in enumerate(split_batches):
        with open(output_prefix + str(idx) + suffix, 'w') as fp:
            json.dump(i, fp, ensure_ascii=False)

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode",required=True)
    parser.add_argument("--num_of_batches",required=False,type=int)
    parser.add_argument("--input_learnit_obversation_instance_json_file",required=False,type=str)
    parser.add_argument("--input_learnit_obversation_instance_json_file_list",required=False,type=str)
    parser.add_argument("--input_bert_idx_index_file",required=False,type=str)
    parser.add_argument("--output_prefix",required=True,type=str)
    parser.add_argument("--bert_npz_list",required=False,type=str)
    parser.add_argument("--number_of_instances_per_learnit_obsersation",required=False,type=int)

    args = parser.parse_args()

    if args.mode == "generate_docid_based_emb_extraction_list":
        generate_docid_based_emb_extraction_list(args.input_learnit_obversation_instance_json_file, args.num_of_batches, args.output_prefix)
    elif args.mode == "further_down_sample_representating_instances_for_learnit_obversation":
        further_down_sample_representating_instances_for_learnit_obversation(args.input_learnit_obversation_instance_json_file_list,args.output_prefix,args.number_of_instances_per_learnit_obsersation)
    elif args.mode == "divide_pattern_list_into_batches":
        divide_learnit_obversation_list_into_batches(args.num_of_batches, args.input_learnit_obversation_instance_json_file,
                                         args.output_prefix,".json")
    else:
        raise NotImplemented("Unsupported mode {}".format(args.mode))