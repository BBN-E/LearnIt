import os

def main(input_list_path,pending_prefix,output_list_path):
    resolved_path = list()
    with open(input_list_path) as fp:
        for i in fp:
            i = i.strip()
            file_name = os.path.basename(i)
            resolved_path.append(os.path.join(pending_prefix,file_name))
    with open(output_list_path,'w') as fp:
        for i in resolved_path:
            fp.write("{}\n".format(i))


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--input_list_path', required=True,type=str)
    parser.add_argument('--pending_prefix', required=True,type=str)
    parser.add_argument('--output_list_path',required=True,type=str)
    args = parser.parse_args()
    input_list_path = args.input_list_path
    pending_prefix = args.pending_prefix
    output_list_path = args.output_list_path
    main(input_list_path,pending_prefix,output_list_path)
