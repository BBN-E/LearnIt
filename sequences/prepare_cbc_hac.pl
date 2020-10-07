use strict;
use warnings;

#!/bin/env perl

use strict;
use warnings;

use lib "/d4m/ears/releases/runjobs4/R2019_03_29/lib";
use runjobs4;

use Cwd 'abs_path';

use File::Basename;
use File::Path;
use File::Copy;

package main;


##############################
my ($exp_root, $exp) = startjobs("queue_mem_limit" => '8G', "max_memory_over" => '0.5G');
if (scalar(@ARGV) != 1) {
    die "run.pl takes in one argument -- a config file";
}
my $config_file = $ARGV[0];
my $params = load_params($config_file); # $params is a hash reference
my $JOB_NAME = get_param($params, "JOB_NAME");
my $serifxml_list_for_generating_positive_and_negative_event_mentions = get_param($params, "serifxml_list_for_generating_positive_and_negative_event_mentions");
my $label_mappings_for_extracting_positive_event_mentions = get_param($params, "label_mappings_for_extracting_positive_event_mentions");


##############################
my $max_sentence_length = 50;

my $PYTHON3 = "env LD_LIBRARY_PATH=/home/hqiu/lib:\$LD_LIBRARY_PATH /home/hqiu/venv/customized-event/bin/python3";

my $ANACONDA_ROOT = "/home/hqiu/ld100/anaconda"; # CPU
my $CONDA_ENV = "py3-tf1.14"; # CPU
my $LINUX_QUEUE = "nongale-sl6";
my $GPU_QUEUE = $LINUX_QUEUE;

# Uncomment below if you want to use GPU

$ANACONDA_ROOT = "/nfs/deft-dev100/u10/criley/miniconda2"; # GPU
$CONDA_ENV = "bert"; # GPU
$GPU_QUEUE = "allGPUs-sl69-non-k10s"; # GPU


max_jobs("$JOB_NAME/step3_run_bert_" => 200,);

# Location of all the output of this sequence
my $processing_dir = make_output_dir("$exp_root/expts/$JOB_NAME");


my $CUR_ROOT = abs_path("exp_root/..");
my $HAT_ROOT = "$CUR_ROOT/HAT/backend";
my $bert_folder = "/nfs/raid88/u10/users/hqiu/runjob/expts/siamese-mention-similarity/generate_siamese_for_wm/bert_txt_in_batch";

##############################


if (exists $stages{"generate-hac-data"}) {
    # Calculate centroid
    my $calculate_centroid_jobid = runjobs(
        [],
        "$JOB_NAME/calculate_centroid",
        {
            BATCH_QUEUE =>$LINUX_QUEUE
        },
        ["$PYTHON3 $HAT_ROOT/tmp/dump_annotated_data_for_cbc.py "]
    );
    # Shrink bert EMB based on frequency
    my $shrink_bert_freq_jobid = runjobs(
        [],
        "$JOB_NAME/init_shrink_bert",
        {
            BATCH_QUEUE => $LINUX_QUEUE
        },
        ["$PYTHON3 $HAT_ROOT/tmp/preparing_cbc_pairwise.py "]
    );
    ##################################

    # Shrink bert EMB based on example.json

    my $shrink_bert_keyword_jobid = runjobs(
        [],
        {
            BATCH_QUEUE => $LINUX_QUEUE
        },
        ["$PYTHON3 $HAT_ROOT/tmp/preparing_cbc_pairwise.py "]
    );

    # Generate par file for pairwise

    # Calculate pairwise

    # Calculate pairwise

    # Do CBC


}


