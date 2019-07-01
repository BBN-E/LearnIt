#!/bin/bash

port=5022

learnit_root=/home/hqiu/ld100/learnit_master

# require cd to learnit directory to have "static" sub-directory for UI
cd $learnit_root

## WM starter
#sjson=/nfs/mercury-04/u41/learnit/WM_starter/source_mappings/all_event_event_pairs/list000.sjson
#params=$learnit_root/params/learnit/runs/wm_starter.params

# WM m6
# sjson=/nfs/mercury-04/u41/learnit/WM_m6/source_mappings/all_event_event_pairs/list000.sjson
# params=$learnit_root/params/learnit/runs/wm_m6.params

# CauseEx-M5
# sjson=/nfs/mercury-04/u41/learnit/CauseEx-M5/source_mappings/all_event_event_pairs/list000.sjson
# params=$learnit_root/params/learnit/runs/causeex-m5.params

## Gigaword
# sjson=/nfs/raid87/u15/data/learnit/EnglishGigawordV5/source_mappings/everything.merged.filter_min.s100p50.filter_max.p26k.sjson
# params=$learnit_root/params/learnit/runs/english_gigaword_event_to_event.params

# WM debug 10 doc set
#sjson=/nfs/raid87/u14/users/msrivast/WM_m6_debug_10docs/source_mappings/all_event_event_pairs-1/freq_1_1/list000.sjson
#params=$learnit_root/params/learnit/runs/wm_m6_10doc.params
#similarity_dir_id=all_event_event_pairs-1
#echo $learnit_root/neolearnit/target/appassembler/bin/OnDemandReMain $params $sjson $similarity_dir_id $port
#export JAVA_OPTS=-Xmx75G; sh $learnit_root/neolearnit/target/appassembler/bin/OnDemandReMain $params $sjson $similarity_dir_id $port

# WM M6 demo
sjson=/nfs/mercury-04/u41/learnit/WM_m6.demo/source_mappings/all_event_event_pairs-1/freq_1_1/list000.sjson
params=$learnit_root/params/learnit/runs/wm_m6_demo.params
similarity_dir_id=all_event_event_pairs-1
echo $learnit_root/neolearnit/target/appassembler/bin/OnDemandReMain $params $sjson $similarity_dir_id $port
export JAVA_OPTS=-Xmx75G; sh $learnit_root/neolearnit/target/appassembler/bin/OnDemandReMain $params $sjson $similarity_dir_id $port

