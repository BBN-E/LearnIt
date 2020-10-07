import glob,os


def main(unix_style_pathname:str,output_list_path:str):
    with open(output_list_path,'w') as fp:
        for path in glob.glob(unix_style_pathname):
            fp.write("{}\n".format(path))


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--unix_style_pathname', required=True,type=str)
    parser.add_argument('--output_list_path',required=True,type=str)
    args = parser.parse_args()
    unix_style_pathname = args.unix_style_pathname
    output_list_path = args.output_list_path
    main(unix_style_pathname, output_list_path)