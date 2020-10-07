#!/usr/bin/env bash

set -e
set -o pipefail
set -u


HUME_REPO="/home/hqiu/ld100/Hume_pipeline_timeline/Hume"
LEARNIT_REPO="/home/hqiu/ld100/Hume_pipeline_timeline/learnit"
ORIGINAL_CONFIG="${HUME_REPO}/config/runs/wm_thanksgiving_timeline.par.sample"
TARGET_CONFIG="${HUME_REPO}/config/runs/wm_thanksgiving_timeline.par"
LEARNIT_PARAM="${LEARNIT_REPO}/params/learnit/runs/wm_thanksgiving_021920.params"
RUNJOB_HUME_JOBPREFIX=`echo ${HUME_REPO} | rev | cut -d/ -f1 | rev`
JOB_NAME="wm_thanksgiving.timeline.02202020"
ONTOLOGY_ROOT_LEARNIT="${LEARNIT_REPO}/inputs/ontologies/wm/hume/internal_ontology"
EXTRACTOR_PATH_IN_HUME="${HUME_REPO}/resource/learnit_patterns/event_and_event_arg"

cd $LEARNIT_REPO

# Step 1 Dump extractor
rm -r ${EXTRACTOR_PATH_IN_HUME}
mkdir -p ${EXTRACTOR_PATH_IN_HUME}
./neolearnit/target/appassembler/bin/ExportLatestTargetAndScoreTableBasedOnYAML ${LEARNIT_PARAM} ${ONTOLOGY_ROOT_LEARNIT}/unary_event_ontology_hume.yaml ${EXTRACTOR_PATH_IN_HUME}
./neolearnit/target/appassembler/bin/ExportLatestTargetAndScoreTableBasedOnYAML ${LEARNIT_PARAM} ${ONTOLOGY_ROOT_LEARNIT}/binary_event_entity_or_value_mention.yaml ${EXTRACTOR_PATH_IN_HUME}

# Step 2 Run hume

cd $HUME_REPO
sg_kill -u $USER -n ${RUNJOB_HUME_JOBPREFIX}
sed -e "s/wm_thanksgiving.timeline.021920.v1/${JOB_NAME}/g" ${ORIGINAL_CONFIG} > ${TARGET_CONFIG}
rm -rf $RAID/ckpts/${RUNJOB_HUME_JOBPREFIX}/${JOB_NAME}
rm -rf $RAID/etemplates/${RUNJOB_HUME_JOBPREFIX}/${JOB_NAME}
rm -rf $RAID/expts/${RUNJOB_HUME_JOBPREFIX}/${JOB_NAME}
rm -rf $RAID/logfiles/${RUNJOB_HUME_JOBPREFIX}/${JOB_NAME}
perl sequences/run.pl ${TARGET_CONFIG} -sge

exit $?