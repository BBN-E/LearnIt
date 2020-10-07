import os,sys
import yaml

current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir, os.path.pardir))
sys.path.append(project_root)

from internal_ontology import build_internal_ontology_tree_without_exampler,serialize_yaml


def add_source_field_if_missing(root):
    sources = root._source
    should_add = True
    for source in sources:
        if "BBN:" in source:
            should_add = False
            break
    if should_add:
        sources.append("BBN: #{}".format(root.original_key))
    for child in root.children:
        add_source_field_if_missing(child)

def main(input_yaml,output_yaml):
    ontology_root = build_internal_ontology_tree_without_exampler(input_yaml)
    add_source_field_if_missing(ontology_root)
    with open(output_yaml,'w') as wfp:
        yaml.dump(serialize_yaml(ontology_root),wfp)


if __name__ == "__main__":
    import argparse
    parser= argparse.ArgumentParser()
    parser.add_argument("--input_yaml",required=True)
    parser.add_argument("--output_yaml",required=True)
    args = parser.parse_args()

    main(args.input_yaml,args.output_yaml)