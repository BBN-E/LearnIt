import yaml,json,os,sys,collections

from model import InternalOntologyTreeNode

def dfs_visit_build_internal_ontology_treenode(root):
    children = list()
    buf = list()
    for yaml_ontology_node in root:
        for node_key,node_entries in yaml_ontology_node.items():
            key_dict = dict()
            for d in node_entries:
                for key, value in d.items():
                    if key.startswith("_"):
                        key_dict[key] = value
                    else:
                        child_node = dfs_visit_build_internal_ontology_treenode([{key:value}])
                        children.append(child_node)
            new_internal_ontology_node = InternalOntologyTreeNode(**key_dict)
            new_internal_ontology_node.parent = None
            new_internal_ontology_node.children = children
            new_internal_ontology_node.original_key = node_key
            for child in children:
                child.parent = new_internal_ontology_node
            buf.append(new_internal_ontology_node)
    return buf[0]



def build_internal_ontology_tree_without_exampler(yaml_path):
    with open(yaml_path,'r') as fp:
        y = yaml.load(fp)
    ontology_tree_root = dfs_visit_build_internal_ontology_treenode(y)
    return ontology_tree_root



def serialize_yaml(internal_ontology_root):
    ret_arr = list()
    for k,v in internal_ontology_root.__dict__.items():
        if k.startswith("_"):
            ret_arr.append({k:v})
    for child in internal_ontology_root.children:
        ret_dict = serialize_yaml(child)
        ret_arr.extend(ret_dict)
    return [{internal_ontology_root.original_key:ret_arr}]

