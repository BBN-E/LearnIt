# first argument is directory containing python script
# second is the Python module name to run
# third is the parameter file
export APPS=/nfs/mercury-06/u14/apps
source $APPS/tensorflow-env/bin/activate
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$APPS/cuda-8.0/lib64:$APPS/lib/x86_64-linux-gnu/:/nfs/mercury-06/u14/apps/usr/lib64/:$APPS/cuda/lib64:/opt/Python-2.7.8-x86_64_ucs4/lib/
export PYTHONPATH=$PYTHONPATH:/d4m/home/rgabbard/repos/buetext/python-packages
cd $1
$APPS/lib/x86_64-linux-gnu/ld-2.17.so $APPS/tensorflow-env/bin/python -m $2 $3
