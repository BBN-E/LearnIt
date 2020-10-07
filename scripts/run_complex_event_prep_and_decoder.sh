# S1: Generate patterns
perl sequences/run_learnit.pl --target=all_with_whitelist --params=/nfs/raid88/u10/users/bmin/repositories/learnit.complex_extractor/params/learnit/runs/wm_m3s_201811.params --mode=mappings-only --epoch 1 -sge
# S2: Run pattern writing server
./neolearnit/target/appassembler/bin/OnDemandReMain params/learnit/runs/wm_m3s_201811.params /nfs/raid88/u10/data/learnit/wm_m3s_migration_201811/source_mappings/all_with_whitelist-1/freq_1_1/00057.sjson 5022
# S3: Copy extractors & targets
cp -r /home/hqiu/ld100/learnit/inputs/extractors/Factor/ .&
cp -r /home/hqiu/ld100/learnit/inputs/extractors/SourceLocation/ .&
cp /home/hqiu/ld100/learnit/inputs/targets/json/Factor.json inputs/targets/json/
cp /home/hqiu/ld100/learnit/inputs/targets/json/SourceLocation.json inputs/targets/json/
# S4: Run decoder
./neolearnit/target/appassembler/bin/TargetAndScoreTableGoodSeedAndPatternLabeler params/learnit/runs/wm_m3s_201811.params /nfs/raid88/u10/data/learnit/wm_m3s_migration_201811/source_mappings/all_with_whitelist-1/freq_1_1/00057.sjson /tmp/label.json
mkdir /tmp/out_serifxml
./neolearnit/target/appassembler/bin/SerifXMLSerializer params/learnit/runs/wm_m3s_201811.params /nfs/raid88/u10/users/hqiu/learnit_data/wm_m3s_migration_201811/source_lists/00057 /tmp/label.json /tmp/out_serifxml/



perl sequences/run_learnit.pl --target=all_with_whitelist --params=/nfs/raid88/u10/users/bmin/exp_envs/wm/learnit/params/learnit/runs/wm_dart_082919_small_test.params --mode=mappings-only --epoch 1 -sge
./neolearnit/target/appassembler/bin/OnDemandReMain params/learnit/runs/wm_dart_082919_small_test.params /nfs/raid88/u10/data/learnit/wm_dart_082919_small_test/source_mappings/all_with_whitelist-1/freq_1_1/00000.sjson 5022

/nfs/raid88/u10/data/learnit/wm_dart.082919/source_mappings/all_with_whitelist-1/freq_1_1/mappings.master.sjson
