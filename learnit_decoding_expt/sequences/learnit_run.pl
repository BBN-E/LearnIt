#!/usr/bin/perl
use strict;
use warnings FATAL => 'all';
use Cwd 'abs_path';


use lib "/d4m/ears/releases/runjobs4/R2019_03_29/lib/";
use lib "/home/hqiu/ld100/Hume_pipeline_2/text-open/src/perl/text_open/lib";
use lib abs_path(__FILE__ . "/../../../sequences/");
my $learnit_root =  abs_path(__FILE__ . "/../../../");
use runjobs4;
use Utils;
use LearnItDecoding;


####### Configure area. Current only support unary_event and binary_event_event.
my $unary_event_extractor_dir = "/home/hqiu/tmp/unary_event_extractors";
my $binary_event_event_extractor_dir = "/home/hqiu/tmp/binary_extractors";
my $input_serif_list = "/nfs/raid88/u10/users/hqiu/runjob/expts/Hume/wm_thanksgiving.121019.121419/generic_events_serifxml_out.list.10doc";
my $stage_to_run = "learnit";
####### End configure area

my @stages = split(/,/,$stage_to_run);
@stages = grep (s/\s*//g, @stages); # remove leading/trailing whitespaces from stage names
my ($expt_dir, $exp) = runjobs4::startjobs();
my $processing_dir = "$expt_dir/expts/";


my $fake_dependent_jobs = [];

foreach my $stage (@stages) {
    if ($stage eq "learnit") {
        my $learnit_decoding_obj = LearnItDecoding->new(
            TEXT_OPEN   => "/home/hqiu/ld100/Hume_pipeline_2/text-open",
            PYTHON3     => "/opt/Python-3.8.1-x86_64/bin/python3.8",
            BATCH_QUEUE => "",
            MEM_LIMIT   => "32G"
        );

        my ($unary_event_jobs,$unary_event_serif_list) = $learnit_decoding_obj->LearnItDecoding(
            dependant_job_ids          => $fake_dependent_jobs,
            job_prefix                 => "unary_event_test",
            runjobs_template_path      => "$learnit_root/templates/learnit_minimal.par",
            runjobs_template_hash      => {},
            target                     => "unary_event",
            input_serifxml_list        => $input_serif_list,
            num_of_jobs                => 1,
            stage_processing_dir       => $processing_dir,
            target_and_score_table_dir => $unary_event_extractor_dir
        );

        my ($binary_event_event_jobs,$binary_event_event_serif_list) = $learnit_decoding_obj->LearnItDecoding(
            dependant_job_ids          => $unary_event_jobs,
            job_prefix                 => "binary_event_event_test",
            runjobs_template_path      => "$learnit_root/templates/learnit_minimal.par",
            runjobs_template_hash      => {},
            target                     => "binary_event_event",
            input_serifxml_list        => $unary_event_serif_list,
            num_of_jobs                => 1,
            stage_processing_dir       => $processing_dir,
            target_and_score_table_dir => $binary_event_event_extractor_dir
        );
    }
}


runjobs4::endjobs();

1;
