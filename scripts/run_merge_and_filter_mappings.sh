#!/bin/bash

cd ..

params=params/learnit/runs/gigaword_1.5M.params
mappings_list=resources/mappings_for_nre_test.list
final_mappings=/tmp/opennre/test/test.sjson

./neolearnit/target/appassembler/bin/MergeMappings $params $mappings_list $final_mappings

