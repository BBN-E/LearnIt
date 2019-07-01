#!/bin/bash

# Runs given commands using a version of Python set up for Tensorflow 1.0
# The paths here are all hardcoded to directories owned by rgabbard
# Once we have a stable GPU setup we should install a better global
# version of Tensorflow and do away with this
# 
# This script is useful because the various libraries necessary for running 
# Tensorflow make your terminal useless for pretty much anything else.  It's
# also handy for running Tensorflow programs from runjobs scripts.
#
# All arguments given to this script are passed to Python

# export TFBASE=/nfs/mercury-06/u14/apps
# # switch to virtualenv in which I installed TF
# source $TFBASE/tensorflow-1.0-env/bin/activate
# # TF needs GPU libraries and also a newer version of glibc
# export LD_LIBRARY_PATH=$TFBASE/cuda-8.0/lib64:$TFBASE/lib/x86_64-linux-gnu/:/nfs/mercury-06/u14/apps/usr/lib64/:$TFBASE/cuda/lib64:/opt/Python-2.7.8-x86_64_ucs4/lib/:$TFBASE/cuda-8.0/extras/CUPTI/lib64/:$LD_LIBRARY_PATH

# export PYTHONPATH=""
# #export PYTHONPATH=/nfs/mercury-11/u105/zjiang/projects/DEFT/ere-tf-evaluation-seq/buetext/python-packages:/nfs/mercury-11/u105/zjiang/projects/DEFT/ere-tf-evaluation-seq/deep-kbp.tf-jserif/deep-relation/src/main/python:/nfs/mercury-11/u105/zjiang/projects/DEFT/ere-tf-evaluation-seq/deep-kbp.tf-jserif/tensorflow-bridge/src/main/python/:/nfs/mercury-11/u105/zjiang/projects/DEFT/ere-tf-evaluation-seq/deep-kbp.tf-jserif/relation_extraction

# export PYTHONPATH=/nfs/mercury-04/u42/bmin/repositories/git/local/buetext/python-packages:/nfs/ld100/u10/bmin/repositories/learnit.master/tensorbridge/src/main/python/

# $TFBASE/lib/x86_64-linux-gnu/ld-2.17.so $TFBASE/tensorflow-1.0-env/bin/python "$@"

# export PYTHONPATH=/nfs/mercury-04/u42/bmin/repositories/git/local/buetext/python-packages:/nfs/ld100/u10/bmin/repositories/learnit.master/tensorbridge/src/main/python/:/nfs/mercury-07/u26/bmin/repo/deepinsight/grounding/src/
export PYTHONPATH=/nfs/mercury-04/u42/bmin/repositories/git/local/buetext/python-packages:/nfs/ld100/u10/bmin/repositories/learnit.master/tensorbridge/src/main/python/:/nfs/mercury-07/u26/bmin/repo/deepinsight/relations/src/

/nfs/ld100/u10/bmin/applications/virtualenv/anaconda-py2-tf1-pip/envs/py2-tf1-conda/bin/python "$@"