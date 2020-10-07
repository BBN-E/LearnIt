#!/bin/env perl

use strict;
use warnings;

use lib "/d4m/ears/releases/runjobs4/R2019_03_29/lib";
use runjobs4;

use Cwd 'abs_path';

use File::Basename;
use File::Path;
use File::Copy;
use JSON;

package main;

my $JOB_NAME = "causeex_m17_test.v4";
my $input_serifxml_list = "/home/hqiu/ld100/Hume_pipeline/Hume/expts/causeex_m17_5files_test.v1/grounded_serifxml.list";

my $QUEUE_PRIO = '5'; # Default queue priority
my ($exp_root, $exp) = startjobs("queue_mem_limit" => '8G', "max_memory_over" => '0.5G');
my $processing_dir = make_output_dir("$exp_root/expts/$JOB_NAME");
my $learnit_root = abs_path("$exp_root");
my $LINUX_QUEUE = "nongale-sl6";
my $source_lists_dir = make_output_dir("$processing_dir/source_lists");
my $BATCH_SIZE = 2;

# set target
my $tasks = [
    {
        "name"             => "relation_cause_effect",
        "learnit_target"   => "binary_event_event",
        "labeler_sequence" => [
            {
                "labeler_class_name" => "NewTargetAndScoreTableGoodPatternLabeler",
                "args"               => "$learnit_root/inputs/extractors/Cause-Effect/Cause-Effect_20181026181840.json"
            },
            {
                "labeler_class_name" => "DownSamplingLabelMappingsLabeler",
                "args"               => "1 true"
            }
        ]
    },
    {
        "name"             => "event_argument_has_location",
        "learnit_target"   => "binary_event_entity_or_value",
        "labeler_sequence" => [
            {
                "labeler_class_name" => "LabelEverythingLabeler",
                "args"               => "has_location FROZEN_GOOD"
            },
            {
                "labeler_class_name" => "RoleFillerTypeConstraint",
                "args"               => "all GPE,LOC false true"
            }
        ]
    }
];


#######################################################################
my %learnit_extractor_targets_to_batch_to_job_id = ();
for my $task (@{$tasks}) {
    $learnit_extractor_targets_to_batch_to_job_id{$task->{"learnit_target"}} = ();
}

my ($NUM_JOBS, $split_serifxml_list_jobid) = split_file_for_processing("make_learnit_extractor_batch_files", $input_serifxml_list, $source_lists_dir . "/", $BATCH_SIZE);

# First, generate mappings

for my $learnit_target (keys %learnit_extractor_targets_to_batch_to_job_id) {
    my $mappings_output_dir = make_output_dir("$processing_dir/mappings/$learnit_target");
    for (my $n = 0; $n < $NUM_JOBS; $n++) {
        my $job_batch_num = sprintf("%05d", $n);
        my $learnit_extractor_job_name = "$JOB_NAME/mappings/$learnit_target/$job_batch_num";
        my $batch_file = "$source_lists_dir/$job_batch_num";
        my $output_learnit_mappings_path = "$mappings_output_dir/$job_batch_num.sjson";
        my $learnit_extractor_job_id = runjobs(
            [ $split_serifxml_list_jobid ], $learnit_extractor_job_name,
            {
                SGE_VIRTUAL_FREE => "12G",
                BATCH_QUEUE      => $LINUX_QUEUE,
                learnit_root     => $learnit_root,
                source_lists     => $source_lists_dir,
                corpus_name      => $JOB_NAME
            },
            [ "$learnit_root/neolearnit/target/appassembler/bin/InstanceExtractor", "learnit_minimal.par", "$learnit_target $batch_file $output_learnit_mappings_path" ]
        );
        $learnit_extractor_targets_to_batch_to_job_id{$learnit_target}{$job_batch_num} = $learnit_extractor_job_id;
    }
}

# Start working on labeler

my $labeled_mappings_dir = make_output_dir("$processing_dir/labeled_mappings");
for my $task (@{$tasks}) {
    my $task_name = $task->{"name"};
    my $learnit_target = $task->{"learnit_target"};
    make_output_dir("$labeled_mappings_dir/$learnit_target");
    my @label_sequence = @{$task->{"labeler_sequence"}};
    my @last_labeler_task_list = ();
    my %job_id_to_labeled_mappings_path = ();

    for (my $n = 0; $n < $NUM_JOBS; $n++) {
        my $job_batch_num = sprintf("%05d", $n);
        my $output_labeled_mappings_file_name = "EMPTY_EXTRACTOR";
        my $mappings_pointer = "$processing_dir/mappings/$learnit_target/$job_batch_num.sjson";
        my $labeler_idx = 0;
        my $last_labeler_job_id = $learnit_extractor_targets_to_batch_to_job_id{$learnit_target}{$job_batch_num};
        for my $labeler (@label_sequence) {
            my $input_labeled_mappings_file_name = $output_labeled_mappings_file_name;
            $output_labeled_mappings_file_name = "$labeled_mappings_dir/$learnit_target/$task_name" . "_$labeler_idx" . "_$job_batch_num.sjson";
            my $labeler_job_name = "$JOB_NAME/labelers/$learnit_target/$task_name" . "_$labeler_idx" . "_$job_batch_num";
            my $learnit_labeler_job_id = runjobs(
                [ $last_labeler_job_id ],
                $labeler_job_name,
                {
                    SGE_VIRTUAL_FREE => "12G",
                    BATCH_QUEUE      => $LINUX_QUEUE,
                    learnit_root     => $learnit_root,
                    source_lists     => $source_lists_dir,
                    corpus_name      => $JOB_NAME
                },
                [ "$learnit_root/neolearnit/target/appassembler/bin/LabelerFactory", "learnit_minimal.par", "$mappings_pointer $input_labeled_mappings_file_name $output_labeled_mappings_file_name " . $labeler->{"labeler_class_name"} . " " . $labeler->{"args"} ]
            );
            $last_labeler_job_id = $learnit_labeler_job_id;
            $job_id_to_labeled_mappings_path{$learnit_labeler_job_id} = $output_labeled_mappings_file_name;
            $labeler_idx++;
        }
        push @last_labeler_task_list, $last_labeler_job_id;
    }

    # Merge Label mappings
    my $merged_labeled_mappings_output_dir = make_output_dir("$processing_dir/merged_labeled_mappings/$task_name");
    my $labeled_mappings_list_path = "$merged_labeled_mappings_output_dir/labeled_mappings.list";
    my $merged_labeled_mappings_output_path = "$merged_labeled_mappings_output_dir/merged_labeled_mappings.sjson";
    open OUT, ">$labeled_mappings_list_path";
    for my $last_labeler_job_id (@last_labeler_task_list) {
        print OUT "$job_id_to_labeled_mappings_path{$last_labeler_job_id}" . "\n";
    }
    close OUT;


    my $merge_labeled_mappings_job_id = runjobs(
        \@last_labeler_task_list,
        "$JOB_NAME/labelers/$task_name" . "_merge_labeled_mappings",
        {
            SGE_VIRTUAL_FREE => "12G",
            BATCH_QUEUE      => $LINUX_QUEUE,
        },
        [ "$learnit_root/neolearnit/target/appassembler/bin/MergeLabeledMappingsFromList $labeled_mappings_list_path $merged_labeled_mappings_output_path" ]
    );

    # Serialize out
    my $serialization_output_dir = make_output_dir("$processing_dir/serialization_out/$task_name");
    my $serialization_job_id = runjobs(
        [ $merge_labeled_mappings_job_id ],
        "$JOB_NAME/serialization/$task_name",
        {
            BATCH_QUEUE      => $LINUX_QUEUE,
            learnit_root     => $learnit_root,
            source_lists     => $source_lists_dir,
            SGE_VIRTUAL_FREE => "12G",
            corpus_name      => $JOB_NAME
        },
        [ "$learnit_root/neolearnit/target/appassembler/bin/SerifAnnotationDataObserver", "learnit_minimal.par", "$merged_labeled_mappings_output_path $serialization_output_dir" ]
    );
}


# run labeler
# labeled_mapping = run_labeler(mappings, task => "type", task => "labeler_sequence")


# merge labeled mapping
# merge_labeled_mapping # this guy depends on all above-mentioned batches to finish

# serialization
# unified_json = run_serailizatrion(lableed_mappnigs)

# convert to the correct output format
# output = convert_to_output_type(unified_json, task => "output_type") # output can be a directory for AMT, or a JSON for training OpenNRE

# }


endjobs();

sub split_file_for_processing {
    my $split_jobname = $_[0];
    my $bf = $_[1];
    my $bp = $_[2];
    my $bs = $_[3];
    open my $fh, "<", $bf or die "could not open $bf: $!";
    my $num_files = 0;
    $num_files++ while <$fh>;
    my $njobs = int($num_files / $bs) + 1;
    if ($num_files % $bs == 0) {
        $njobs--;
    }
    print "File $bf will be broken into $njobs batches of approximate size $bs\n";
    my $jobid = runjobs([], "$JOB_NAME/$split_jobname",
        {
            BATCH_QUEUE => $LINUX_QUEUE,
            SCRIPT      => 1,
        },
        "/usr/bin/split -d -a 5 -l $bs $bf $bp");

    return($njobs, $jobid);
}

sub make_output_dir {
    my $dir = $_[0];
    mkpath($dir);
    return abs_path($dir);
}