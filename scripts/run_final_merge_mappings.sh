#!/bin/bash

# Note: run this on a single server with 200GB memory
env JAVA_OPTS="-Xmx200G"; sh /nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/neolearnit/target/appassembler/bin/MergeMappings /nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/params/learnit/runs/gigaword_1.5M.params /nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/list_sjson /nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/final.sjson 50 -1 -1 -1>& log.merge.test&

# Note: run this on a single server with 200GB memory
env JAVA_OPTS="-Xmx200G"; sh /nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/neolearnit/target/appassembler/bin/MergeMappings /nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/params/learnit/runs/gigaword_1.5M.params /nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/list_sjson /nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/final.sjson 200 -1 -1 -1 > log.merge.train&

