#!/bin/bash

bin=./neolearnit/target/appassembler/bin/FindLearnitPatternFromLabeledInstances

#params=./params/learnit/runs/better_700k_46733.params
#idx=9
#mappingsList=/nfs/raid88/u10/users/hqiu/learnit_data/better.700k.46733/source_mappings_lists/unary_event-1/freq_1_1/batch_$idx
#LabeledMappingsList=/nfs/raid88/u10/users/bmin/temp/batch_$idx\.labeled.list
#log=$LabeledMappingsList\.patterns.log

params=./params/learnit/runs/better_bpjson_46733.params
mappingsList=/nfs/raid88/u10/users/hqiu/learnit_data/better.bp.46733.v2/source_mappings_lists/unary_event-1/freq_1_1/batch_0
LabeledMappingsList=/nfs/raid88/u10/users/bmin/temp/bp_json_labeled.list
log=$LabeledMappingsList\.patterns.log

nohup $bin $params $mappingsList $LabeledMappingsList >& $log
