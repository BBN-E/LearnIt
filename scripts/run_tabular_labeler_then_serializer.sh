#!/bin/bash

mappings_list=/nfs/raid88/u10/users/hqiu/learnit_data/better.700k.46733/source_mappings_lists/unary_event-1/freq_1_1/batch_0
output_labeled_mappings=/nfs/raid88/u10/users/bmin/temp/batch_0_labeled.mappings
tabular_patterns_file=/nfs/raid88/u10/users/bmin/temp/diff_pattern_dist.txt
min_freq=200
max_instances_to_label=10
output_dir1=/nfs/raid88/u10/users/bmin/temp/serifxml_tmp/
output_dir2=/nfs/raid88/u10/users/bmin/temp/shrinked_serifxml_tmp/


./neolearnit/target/appassembler/bin/TabularPatternListLabeler ./params/learnit/runs/better_700k_46733.params $mappings_list $output_labeled_mappings $tabular_patterns_file $min_freq $max_instances_to_label

mkdir -p output_dir1

./neolearnit/target/appassembler/bin/SerializeLabeledMappingsToNLPLingo ./params/learnit/runs/better_700k_46733.params $output_labeled_mappings $output_dir1

mkdir -p output_dir2

python3 /home/criley/raid/data/Hume/filter_serif_sentence.from_span.py $output_dir1/AllInOne/argument.span_serif_list $output_dir2/ $output_dir2/argument.span_serif_list
