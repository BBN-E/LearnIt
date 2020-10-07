#!/bin/bash

# env JAVA_OPTS="-Xmx75G"; sh /nfs/raid88/u10/users/bmin/exp_envs/wm/learnit/neolearnit/target/appassembler/bin/InstanceExtractor /nfs/raid88/u10/users/bmin/exp_envs/wm/learnit/params/learnit/runs/bilingual_chinese_gigaword_sample.params unary_event /nfs/raid88/u10/data/learnit/ChineseGigawordV5sample/data/source_lists/bidoc_source_lists/batch_00000 /nfs/raid88/u10/data/learnit/ChineseGigawordV5sample/data/source_mappings/all_mention_no_coref_pairs-1/freq_1_1/batch_00000.sjson
env JAVA_OPTS="-Xmx75G"; sh /nfs/raid88/u10/users/bmin/exp_envs/wm/learnit/neolearnit/target/appassembler/bin/InstanceExtractor /nfs/raid88/u10/users/bmin/exp_envs/wm/learnit/params/learnit/runs/bilingual_chinese-english.example.params unary_event /nfs/raid88/u10/users/bmin/projects/d2d/learnit_data/source_lists//batch_00000 /nfs/raid88/u10/users/bmin/projects/d2d/learnit_data/source_mappings/unary_event-1/freq_1_1/batch_00000.sjson

