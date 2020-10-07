import yaml

class OntologyTreeNode(object):
    def __init__(self,**kwargs):
        self.__dict__.update(kwargs)

class InternalOntologyTreeNode(OntologyTreeNode):
    pass

def dfs_visit_build_internal_ontology_treenode(root,node_name_to_nodes_mapping,current_node_key):
    children = list()
    key_dict = dict()
    for dct in root:
        for key, value in dct.items():
            if key.startswith("_"):
                key_dict[key] = value
            else:
                children.append(dfs_visit_build_internal_ontology_treenode(value,node_name_to_nodes_mapping,key))
    new_internal_ontology_node = InternalOntologyTreeNode(**key_dict)
    new_internal_ontology_node.parent = None
    new_internal_ontology_node.children = children
    new_internal_ontology_node.exemplars = set()
    for i in children:
        i.parent = new_internal_ontology_node
    new_internal_ontology_node.original_key = current_node_key
    node_name_to_nodes_mapping.setdefault(current_node_key,set()).add(new_internal_ontology_node)
    return new_internal_ontology_node

def serialize_yaml(internal_ontology_root):
    ret_arr = list()
    for k,v in internal_ontology_root.__dict__.items():
        if k.startswith("_"):
            ret_arr.append({k:v})
    for child in internal_ontology_root.children:
        ret_dict,child_root_name = serialize_yaml(child)
        ret_arr.extend(ret_dict)
    return [{internal_ontology_root.original_key:ret_arr}],internal_ontology_root.original_key

def main(in_yaml,out_yaml):
    with open(in_yaml,'r') as fp:
        y = yaml.load(fp)
    node_name_to_nodes_mapping = dict()
    yaml_root = y[0]['Factor']
    ontology_tree_root = dfs_visit_build_internal_ontology_treenode(yaml_root,node_name_to_nodes_mapping,"Factor")

    def sort_childs(root):
        root.children = sorted(root.children,key=lambda x:x.original_key)
        for child in root.children:
            sort_childs(child)
    sort_childs(ontology_tree_root)
    yaml_root = serialize_yaml(ontology_tree_root)[0]
    with open(out_yaml,'w') as fp:
        yaml.dump(yaml_root,fp)



if __name__ == "__main__":
    in_yaml = ""
    out_yaml = ""
    main(in_yaml,out_yaml)
