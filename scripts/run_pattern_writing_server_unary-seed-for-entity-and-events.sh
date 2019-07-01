#!/bin/bash

port=5025

learnit_root=/nfs/ld100/u10/bmin/repositories/learnit.unary-seed-for-entity-and-events/learnit

# require cd to learnit directory to have "static" sub-directory for UI
cd $learnit_root

sjson=/nfs/mercury-04/u41/learnit/test-unary-mentions/source_mappings/everything_no_constraints/list000.sjson

params=$learnit_root/params/learnit/runs/test-unary-mentions.params


export JAVA_OPTS=-Xmx192G; sh $learnit_root/neolearnit/target/appassembler/bin/OnDemandReMain $params $sjson $port

