#!/bin/bash

# compares event-event relation extrators for various targets given the timestamps

learnit_params=$1
t1=$2
t2=$3
learnit_root=${learnit_params/\/params\/learnit\/*//}
bin=$learnit_root/neolearnit/target/appassembler/bin/ExtractorsComparator
ex_dir=$learnit_root/inputs/extractors

output=/tmp/ex_comp_output
rm -f $output
TARGETS=$ex_dir/*

for target in $TARGETS
do
   target=`basename $target`
   echo $bin $leanrnit_params $ex_dir/${target}/${target}_$t1.json $ex_dir/${target}/${target}_$t2.json
   `$bin $learnit_params $ex_dir/${target}/${target}_$t1.json $ex_dir/${target}/${target}_$t2.json >> $output` || { echo 'extractors comparison command failed'; exit 1;} 
done

cat $output|grep -v "=="|grep -v "SLF4J"|sort
