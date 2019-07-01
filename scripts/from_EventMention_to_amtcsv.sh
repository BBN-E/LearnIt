#!/bin/bash


serifxml_list_with_eventmention_added= /home/hqiu/Public/serifxml_list/handeled_by_kbp_and_nn_event/1000_files.list
learnit_root=/home/hqiu/ld100/learnit_working
scratch_space=/home/hqiu/massive/tmp
amtcsv_output_prefix=/home/hqiu/massive/tmp/learnitdecoding3

### DO NOT CHANGE BARRIER
dummy_config_file=$learnit_root/params/learnit/runs/1000_files.params
### END OF DO NOT CHANGE BARRIER




# require cd to learnit directory to have "static" sub-directory for UI
cd $learnit_root

sh ./neolearnit/target/appassembler/bin/LabelExtractor $dummy_config_file all_event_event_pairs $serifxml_list_with_eventmention_added $scratch_space/master.sjson
echo $scratch_space/master.sjson > $scratch_space/mappings.list
sh ./neolearnit/target/appassembler/bin/ExternalSerializer $dummy_config_file $scratch_space/mappings.list $amtcsv_output_prefix 1