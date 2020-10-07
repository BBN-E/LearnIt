package runjobs_mapping_quick;
use strict;
use warnings FATAL => 'all';

use lib "/d4m/ears/releases/runjobs4/R2019_03_29/lib";


use Cwd 'abs_path';

my $textopen_root;
my $learnit_root;
BEGIN{
    $textopen_root = "/home/hqiu/ld100/Hume_pipeline_2/text-open";
    $learnit_root  = abs_path(__FILE__ . "/../../");
    unshift(@INC, abs_path(__FILE__ . "/../"));
    unshift(@INC, "$textopen_root/src/perl/text_open/lib");
    unshift(@INC, "/d4m/ears/releases/runjobs4/R2019_03_29/lib");
}

use constants;
use Utils;
use runjobs4;


my $QUEUE_PRIO = '5'; # Default queue priority
my ($exp_root, $exp) = startjobs("queue_mem_limit" => '8G', "max_memory_over" => '0.5G', "queue_priority" => $QUEUE_PRIO);
max_jobs(100);
my $LINUX_QUEUE = "nongale-sl6";

### Please Change this
my $JOB_NAME = "cord19_mappings_merge_p3";
my $BATCH_SIZE = 5;
my $input_mappings_list = "/home/hqiu/ld100/learnit/expts/cord19_mappings_merge_p2/general_program/mappings.list";
my $PARAMS = "$learnit_root/params/learnit/runs/cord_19.params";
(my $processing_dir,undef) = Utils::make_output_dir("$exp_root/expts/$JOB_NAME","$JOB_NAME/mkdir_output",[]);
### End please Change this


{
    my $stage_name = "general_program";
    (my $stage_processing_dir,undef) = Utils::make_output_dir("$processing_dir/$stage_name","$JOB_NAME/$stage_name/mkdir_output",[]);
    (my $batch_file_directory,undef) = Utils::make_output_dir("$stage_processing_dir/batch_files","$JOB_NAME/$stage_name/mkdir_output_batch",[]);

    my ($NUM_JOBS, $split_generic_events_jobid) = Utils::split_file_for_processing("$JOB_NAME/$stage_name/make_batch_files", $input_mappings_list, "$batch_file_directory/batch_file_", $BATCH_SIZE);
    my @generic_events_split_jobs = ();

    for (my $n = 0; $n < $NUM_JOBS; $n++) {
        my $job_batch_num = sprintf("%05d", $n);
        my $batch_file = "$batch_file_directory/batch_file_$job_batch_num";
        (my $output_serifxml_dir,undef) = Utils::make_output_dir("$stage_processing_dir/$job_batch_num/output/","$JOB_NAME/$stage_name/$job_batch_num/mkdir_output_batch",[]);
        my $add_event_mentions_from_propositions_jobid =
            runjobs(
                [ $split_generic_events_jobid ], "$JOB_NAME/$stage_name/merge_mappings_$job_batch_num",
                {
                    SGE_VIRTUAL_FREE     => "64G",
                    BATCH_QUEUE          => $LINUX_QUEUE,
                },
                [ "$learnit_root/neolearnit/target/appassembler/bin/MergeMappings $PARAMS $batch_file $output_serifxml_dir/$job_batch_num.sjson"]
            );
        # my $filter_mappings_jobid =
        #     runjobs(
        #         [ $add_event_mentions_from_propositions_jobid ], "$JOB_NAME/$stage_name/apply_serif_pattern_filter_$job_batch_num",
        #         {
        #             SGE_VIRTUAL_FREE     => "64G",
        #             BATCH_QUEUE          => $LINUX_QUEUE,
        #         },
        #         [ "$learnit_root/neolearnit/target/appassembler/bin/BadSerifPatternFilter $PARAMS unary_event $output_serifxml_dir/$job_batch_num.sjson $output_serifxml_dir/$job_batch_num.filtered.sjson"]
        #     );
        # push(@generic_events_split_jobs, $filter_mappings_jobid);
    }
}
endjobs();

1;