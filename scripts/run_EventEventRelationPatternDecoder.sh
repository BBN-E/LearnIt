#!/bin/bash

set -e

paramFile=$1
jsonFile=$2 # This can also be a list file containing paths to several json mappings files
learnit_root=$3
output_json_file=$4
useOnlyPropPatterns=$5
MIN_FREQ_EVENT_PAIRS=$6
USE_TRIPLE_WHITE_LIST=$7
strFileTripleRelationEventPairs=$8
learnit_pattern_dir=$9
logFile=${10}
decode_seed=$11


bin=$learnit_root/neolearnit/target/appassembler/bin/EventEventRelationPatternDecoder

# WM
#paramFile=$learnit_root/params/learnit/runs/wm_starter.params
#jsonFile=/nfs/mercury-04/u41/learnit/WM_starter/source_mappings/all_event_event_pairs/list000.sjson

## CauseEx-M5
#paramFile=$learnit_root/params/learnit/runs/causeex-m5.params
#jsonFile=/nfs/mercury-04/u41/learnit/CauseEx-M5/source_mappings/all_event_event_pairs/list000.sjson

# WM m6
#jsonFile=/nfs/mercury-04/u41/learnit/WM_m6/source_mappings/all_event_event_pairs/list000.sjson
#paramFile=$learnit_root/params/learnit/runs/wm_m6.params
echo 'Running decoding command: '
echo $bin $paramFile $jsonFile $output_json_file $useOnlyPropPatterns $MIN_FREQ_EVENT_PAIRS $USE_TRIPLE_WHITE_LIST $strFileTripleRelationEventPairs $learnit_pattern_dir

echo 'Writing to log file: ' $logFile

$bin $paramFile $jsonFile $output_json_file $useOnlyPropPatterns $MIN_FREQ_EVENT_PAIRS $USE_TRIPLE_WHITE_LIST $strFileTripleRelationEventPairs $learnit_pattern_dir $decode_seed > "${logFile}"

# generate relations in plain-text
`cat $logFile|grep -v "=="|grep -v "SLF4J"|grep -v "\.json"|grep -v "targetPath"|grep -v "nohup:"|grep -v "Copied JSON"|grep -v "# lines read"|grep -v "docid:"|sort|uniq > $logFile\.decoder.output.relations` || {echo 'creating relations file failed; exit 1;'}
