#!/bin/bash


for dirID in $(ls -1 /nfs/mercury-04/u41/learnit/gigaword_1.5M/serifxml.add_extended_events)
do
    dir=/nfs/mercury-04/u41/learnit/gigaword_1.5M/serifxml.add_extended_events/$dirID
    list=/nfs/mercury-04/u41/learnit/gigaword_1.5M/source_lists.add_extended_events/$dirID
    echo $dir
    echo $list

    find $dir/*.xml > $list
done


