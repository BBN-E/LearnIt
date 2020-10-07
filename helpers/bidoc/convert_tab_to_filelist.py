import json
import os
import argparse
from pycube.utils import json_segment


def _main(args):
    tab_path = args.tab_filepath
    output_learnit_data_root = args.output_learnit_data_root
    os.makedirs(os.path.join(output_learnit_data_root,'alignment'),exist_ok=True)
    os.makedirs(os.path.join(output_learnit_data_root,'source_lists'),exist_ok=True)
    #src_language = "english"
    src_language = args.src_language
    #dst_language = "arabic"
    dst_language = args.dst_language
    output_buffer = list()
    with open(tab_path) as fp:
        for i in fp:
            i = i.strip()
            doc_id, alignment_json, src_xml, dst_xml = i.split("\t")
            alignment_lines = []
            for segment in json_segment.read_json_from_files([alignment_json]):
                docid = segment['DOCUMENT_ID']
                alignment = segment[args.alignment_field]
                alignment = alignment.replace("-",":")
                alignment_lines.append(alignment.rstrip())

            output_alignment_file_path = os.path.join(
                output_learnit_data_root,
                'alignment',
                '{}.alignment'.format(
                    doc_id
                )
            )
            alignment_output = "\n".join(alignment_lines)

            with open(output_alignment_file_path,'w') as wfp:
                wfp.write(alignment_output)

            output_buffer.append(
                "docid:{} {}:{} {}:{} alignment:{}".format(
                    docid, 
                    src_language, 
                    src_xml, 
                    dst_language, 
                    dst_xml,                                       
                    output_alignment_file_path
                )
            )
    with open(os.path.join(output_learnit_data_root,'source_lists',"x00000"),'w') as wfp:
        for i in output_buffer:
            wfp.write("{}\n".format(i))

def _parser_setup():
    parser = argparse.ArgumentParser(
        description="TBD."
    )
    parser.add_argument(
        '--tab_filepath',
        required=True,
    )
    parser.add_argument(
        '--output_learnit_data_root',
        required=True,
    )
    parser.add_argument(
        '--src_language',
        required=True,
    )
    parser.add_argument(
        '--dst_language',
        required=True,
    )
    parser.add_argument(
        '--alignment_field',
        required='True'
    )
    return parser
    


if __name__ == "__main__":
   _main(_parser_setup().parse_args())
