cd ../

# perl sequences/generate_source_mappings.pl -sge --relation=everything --params=$PWD/params/learnit/runs/wm_starter.params
perl sequences/generate_source_mappings.pl -sge --relation=all_event_event_pairs --params=/nfs/ld100/u10/bmin/repositories/akbc-readonly/akbc-v2/params/learnit/runs/wm_starter.params


## Add verbs as event mentions (triggers)
#listFiles=/nfs/mercury-04/u41/learnit/WM_starter/serifxml.original.list
#outDir=/tmp/serifxml
#/nfs/ld100/u10/bmin/repositories/akbc-readonly/akbc-v2/application/target/appassembler/bin/AddEventMentionByKeywords $listFiles $outDir
