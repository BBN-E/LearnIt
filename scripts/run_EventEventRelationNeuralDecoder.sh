#!/bin/bash

set -e

strFileParam=$1
strFileMappings=$2
strOutFilePrefix=$3
learnit_root=$4
deepinsight_root=$5
nre_model_root=$6
anaconda_root=$7
conda_env_name=$8

# Generate data for OpenNRE
${learnit_root}/neolearnit/target/appassembler/bin/GenerateTrainingDataFromSeedsForOpenNRE $strFileParam EMPTY_EXTRACTOR $strFileMappings $strOutFilePrefix NA -1 -1 DECODING

# Run NN model

json_data_test=${strOutFilePrefix}/data.json
prediction_file=${strOutFilePrefix}/bag_predictions.json
json_rel2id=$nre_model_root/rel2id.json
word2vec=$nre_model_root/word_vec.json
# model_prefix=/nfs/raid87/u15/users/jcai/deepinsight/relations/scripts/checkpoint/causality_pcnn_att-57
model_prefix=$nre_model_root/causality_pcnn_att-57

encoder="pcnn"
selector="att"

max_length=120
batch_size=160
word_embedding_dim=300

cd $deepinsight_root/relations/scripts/

PYTHONPATH="$anaconda_root/envs/$conda_env_name/lib/python2.7/site-packages" LD_LIBRARY_PATH="$anaconda_root/envs/$conda_env_name/lib:$anaconda_root/lib:$LD_LIBRARY_PATH" $anaconda_root/envs/$conda_env_name/bin/python ../src/decode.py \
                    --json_data_test $json_data_test \
                    --prediction_file $prediction_file \
                    --json_rel2id $json_rel2id \
                    --word2vec $word2vec \
                    --model_prefix $model_prefix \
                    --encoder $encoder \
                    --selector $selector \
                    --max_length $max_length \
                    --batch_size $batch_size \
                    --word_embedding_dim $word_embedding_dim

