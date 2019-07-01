#!/bin/bash

port=5022

learnit_root=/nfs/ld100/u10/bmin/repositories/learnit.master/
# /home/hqiu/ld100/CauseEx-pipeline-WM/learnit

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
#sjson=/nfs/raid87/u15/data/learnit/EnglishGigawordV5/source_mappings/everything.merged.filter_min.s100p50.filter_max.p26k.sjson
#params=$learnit_root/params/learnit/runs/english_gigaword_event_to_event.params

# sjson=/home/hqiu/massive/learnit_data/gigaword_1.5M/source_mappings/all_event_event_pairs-1/freq_1_1/mappings.master.sjson.before_filter
# params=$learnit_root/params/learnit/runs/gigaword_1.5M.params
##sjson=/home/hqiu/Public/Gigaword_1.5M_Mappings/all_event_event_pairs-1/freq_1_1/00001.sjson

sjson=/nfs/raid87/u10/users/bmin/Runjobs/expts/learnit.master/gigaword_1.5M.v5/mappings/final.sjson
# sjson=/nfs/mercury-07/u36/copied_from_mercury-04/u41/learnit/gigaword_1.5M//source_mappings/binary_event_event/00000.sjson
# sjson=/nfs/raid87/u10/users/bmin/Runjobs/expts/learnit.master/gigaword_1.5M.v5/mappings/list00000.sjson
params=$learnit_root/params/learnit/runs/gigaword_1.5M.params

export JAVA_OPTS=-Xmx192G; sh $learnit_root/neolearnit/target/appassembler/bin/OnDemandReMain $params $sjson all_event_event_pairs-1 $port

