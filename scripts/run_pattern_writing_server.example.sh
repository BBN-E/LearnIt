#!/bin/bash

port=5022

learnit_root=/nfs/ld100/u10/bmin/repositories/learnit

# require cd to learnit directory to have "static" sub-directory for UI
cd $learnit_root

## WM starter
#sjson=/nfs/mercury-04/u41/learnit/WM_starter/source_mappings/all_event_event_pairs/list000.sjson
#params=$learnit_root/params/learnit/runs/wm_starter.params

# WM m6
sjson=/nfs/mercury-04/u41/learnit/WM_m6/source_mappings/all_event_event_pairs/list000.sjson
params=$learnit_root/params/learnit/runs/wm_m6.params

# CauseEx-M5
# sjson=/nfs/mercury-04/u41/learnit/CauseEx-M5/source_mappings/all_event_event_pairs/list000.sjson
# params=$learnit_root/params/learnit/runs/causeex-m5.params

## Gigaword
# sjson=/nfs/raid87/u15/data/learnit/EnglishGigawordV5/source_mappings/everything.merged.filter_min.s100p50.filter_max.p26k.sjson
# params=$learnit_root/params/learnit/runs/english_gigaword_event_to_event.params

export JAVA_OPTS=-Xmx100G; sh $learnit_root/neolearnit/target/appassembler/bin/OnDemandReMain $params $sjson $port

