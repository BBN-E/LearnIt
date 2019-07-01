#!/bin/bash

port=5025

learnit_root=/home/hqiu/ld100/CauseEx-pipeline-WM/learnit

# require cd to learnit directory to have "static" sub-directory for UI
cd $learnit_root

## WM starter
#sjson=/nfs/mercury-04/u41/learnit/WM_starter/source_mappings/all_event_event_pairs/list000.sjson
#params=$learnit_root/params/learnit/runs/wm_starter.params

# WM m6
#sjson=/nfs/mercury-04/u41/learnit/WM_m6/source_mappings/all_event_event_pairs/list000.sjson
#params=$learnit_root/params/learnit/runs/wm_m6.params

# CauseEx-M5
# sjson=/nfs/mercury-04/u41/learnit/CauseEx-M5/source_mappings/all_event_event_pairs/list000.sjson
# params=$learnit_root/params/learnit/runs/causeex-m5.params

## Gigaword
# sjson=/nfs/raid87/u15/data/learnit/EnglishGigawordV5/source_mappings/everything.merged.filter_min.s100p50.filter_max.p26k.sjson
# params=$learnit_root/params/learnit/runs/english_gigaword_event_to_event.params

#sjson=/nfs/mercury-04/u41/learnit/1000_files/source_mappings/all_event_event_pairs-1/freq_1_1/mappings.master.sjson
#sjson=/home/hqiu/Public/Gigaword_1.5M_Mappings/all_event_event_pairs-1/freq_1_1/00001.sjson

sjson=/home/hqiu/massive/learnit_data/wm_m12/source_mappings/all_event_event_pairs-1/freq_1_1/mappings.master.sjson

params=$learnit_root/params/learnit/runs/wm_m12_v1.params

export JAVA_OPTS=-Xmx192G; sh $learnit_root/neolearnit/target/appassembler/bin/OnDemandReMain $params $sjson $port

