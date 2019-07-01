#!/bin/bash

find /nfs/mercury-04/u41/learnit/gigaword_1.5M/source_mappings/all_event_event_pairs/*.sjson|shuffle_lines.pl > /tmp/list_mappings_all_for_nre

head -n2500 /tmp/list_mappings_all_for_nre > ../resources/mappings_for_nre_train.list
tail -n500 /tmp/list_mappings_all_for_nre > ../resources/mappings_for_nre_test.list

