#!/bin/env perl

use strict;
use warnings FATAL => 'all';

use Cwd 'abs_path';

use File::Basename;
use File::Path;

use Getopt::Long;

my $textopen_root;
my $learnit_root;

BEGIN{
    $textopen_root = "/home/hqiu/ld100/Hume_pipeline_2/text-open";
    $learnit_root = Cwd::abs_path(__FILE__ . "/../..");
    unshift(@INC, abs_path(__FILE__ . "/../../sequences"));
    unshift(@INC, "$textopen_root/src/perl/text_open/lib");
    unshift(@INC, "/d4m/ears/releases/runjobs4/R2019_03_29/lib");
}

use ParameterFiles;
use constants;
use Utils;
# use CSerif;
use runjobs4;

package main;

my $PYTHON3 = "/nfs/raid87/u11/users/hqiu/miniconda_prod/envs/py3-ml-general/bin/python3";
my $CREATE_FILE_LIST_SCRIPT = "$PYTHON3 $learnit_root/neolearnit/src/main/python/create_filelist.py";
sub usage() {
    print "USAGE:\n\n";
    print "perl run_learnit.pl --target <target-name> --params <abs-path-to-params-file> --epoch N\n\n";
    exit(1);
}

# Package declaration:
package main;

if (!defined($ENV{SGE_ROOT})) {
    print "WARNING: SGE_ROOT not defined; using a default value!\n";
    $ENV{SGE_ROOT} = "/opt/sge-6.2u5";
}

###### VAR SETTING #############

# Command-line arguments:
our @SAVED_ARGV = @ARGV;
Getopt::Long::Configure("pass_through");
GetOptions(
    "target=s" => \$main::TARGET,
    "params=s" => \$main::PARAMS,
    "epoch=s"  => \$main::EPOCH
); #optional for yield view/seed initialization
Getopt::Long::Configure("no_pass_through");

defined(our $PARAMS) && defined(our $TARGET) && defined(our $EPOCH) || usage();

print "Using param file $PARAMS\n";
our $params = ParameterFiles::load_param_file($PARAMS);

#WINDOW QUEUE
# my $path_convert = \&File::PathConvert::unix2win;
my $path_convert = (sub {shift;});
my $BATCH_SIZE = 20;

our $QUEUE = $params->{"linux_queue"};
our $QUEUE_PRIORITY = $params->{"queue_priority"};
my @stages = split(/,/, $params->{"stages_to_run"});
@stages = grep (s/\s*//g, @stages); # remove leading/trailing whitespaces from stage names


####### START RUNJOBS ###########
my ($expt_dir, $exp) = startjobs();


my $JAVA_PREFIX = $params->{"java_command"};
my $MAVEN_BIN = "/opt/apache-maven-3.0.4/bin/mvn";

my $CORPUS_DIR = $params->{"corpus_dir"};

my $expt_name = $params->{"corpus_name"};
my $job_prefix = "$expt_name-$EPOCH/$TARGET";
max_jobs("$job_prefix/"=>100,); # Max number of jobs to run at once

run_on_queue_now_local("make_expt_dirs-$TARGET", "mkdir -p $expt_dir/expts/$job_prefix");

$learnit_root = $params->{"learnit_root"};
my $hume_root = "/home/hqiu/ld100/Hume_pipeline_2/Hume";

my $seed_frequency = $params->{"min_seed_frequency_per_batch"};
my $pattern_frequency = $params->{"min_pattern_frequency_per_batch"};
my $frequency_combination = $seed_frequency . '_' . $pattern_frequency;
my $mappings_dir = $params->{"mappings_dir"} . "/$TARGET-$EPOCH/freq_$frequency_combination";
my $mappings_lists_dir = $params->{"mappings_lists_dir"} . "/$TARGET-$EPOCH/freq_$frequency_combination";
# max_jobs("$job_prefix/learnit_observation_emb/3_extract_pattern_instance_bert_emb" => 8,);


foreach my $stage (@stages) {
    if ($stage eq "generate-serif") {
        run_serif(
            $params->{"input_sgm_list"},
            $params->{"awake_db"},
            $params->{"serif_cause_effect_patterns_dir"},
            $params->{"project_specific_serif_par"},
            $params->{"project_specific_serif_data_root"},
            $params->{"serif_output_dir"},
            $params->{"corpus_dir"}
        );
        dojobs();
    }
    if ($stage eq "mappings-only") {
        extract_mappings($CORPUS_DIR, $mappings_dir, $mappings_lists_dir);
        create_aggregate_mappings_file($mappings_lists_dir, $mappings_dir . "/mappings.master.sjson");
        dojobs();
    }
    if ($stage eq "run-specific-program-on-mappings-list") {
        run_specific_program_on_mappings_list(
            dependant_job_ids => []
        );
        dojobs();
    }
    if ($stage eq "label-mappings-and-serialize-to-serifxml") {
        my $serialization_dir = $params->{"serialization_dir"};
        my $labeled_mappings_dir = Utils::make_output_dir("$serialization_dir/labeled_mappings");
        my $labeled_mappings_list = "$serialization_dir/labeled_mappings.list";
        my $merged_labeled_mapping_path = "$serialization_dir/labeled_mappings.master.sjson";
        my $output_serif_dir = Utils::make_output_dir("$serialization_dir/serif");

        my @labeled_mappings_list = label_mappings_using_extractors
            (
                $mappings_lists_dir,
                $params->{"extractor_dir"},
                $labeled_mappings_dir
            );

        open OUT, ">$labeled_mappings_list";
        foreach my $labeled_path (@labeled_mappings_list) {
            print OUT $labeled_path . "\n";
        }
        close OUT;
        merge_labeled_mappings($labeled_mappings_list, $merged_labeled_mapping_path);
        dojobs();
        serialize_labeled_mappings($merged_labeled_mapping_path, $output_serif_dir);
    }
    if ($stage eq "similarities-only") {
        calculate_seed_or_pattern_similarity(
            dependant_job_ids                               => [],
            focus_learnit_obversation_obj                   => "LearnitPattern",
            number_of_batches                               => 100,
            mappings_lists_dir                              => $mappings_lists_dir,
            job_prefix                                      => $job_prefix,
            params                                          => $params,
            number_of_instances_per_learnit_obversation_cap => 10
        );
        # calculate_seed_or_pattern_similarity(
        #     dependant_job_ids                               => [],
        #     focus_learnit_obversation_obj                   => "Seed",
        #     number_of_batches                               => 100,
        #     mappings_lists_dir                              => $mappings_lists_dir,
        #     job_prefix                                      => $job_prefix,
        #     params                                          => $params,
        #     number_of_instances_per_learnit_obversation_cap => 10
        # );
        dojobs();
    }
    if ($stage eq "scoring-only") {
        score_mappings($mappings_lists_dir,
            $params->{"extractors_list"},
            $params->{"scoring_output_dir"});
        dojobs();
    }
    if ($stage eq "build-dictionary") {
        build_dictionary(
            dependant_job_ids => [],
            source_list_dir   => $CORPUS_DIR
        );
        dojobs();
    }
}

endjobs();



sub extract_mappings {
    (
        my $source_list_dir,
        my $source_mappings_dir,
        my $source_mapping_lists
    ) = @_;

    run_on_queue_now_local("mappings/prepare/make-mappings-dirs-$frequency_combination",
        "mkdir -p $source_mappings_dir; mkdir -p $source_mapping_lists");

    my @batch_source_lists = glob "$source_list_dir/*";
    if (!@batch_source_lists) {
        print "Found empty directory for source lists! Cannot extract mappings.\n";
        exit(1);
    }
    foreach my $batch_list (@batch_source_lists) {
        my ($batch_name, $batch_dir) = fileparse($batch_list);

        my $output_file = "$source_mappings_dir/$batch_name.sjson";
        my $extract_thread_job = run_on_queue("mappings/extract-${batch_name}-" . $frequency_combination,
            "24G",
            make_java_command("24G", $params->{"learnit_root"} . "/neolearnit/target/appassembler/bin/", "InstanceExtractor", "$PARAMS $TARGET $batch_list $output_file"));
        my $filter_thread_job = run_on_queue_with_prev_job
            (
                "mappings/filter-extract-${batch_name}-" . $frequency_combination,
                $extract_thread_job,
                "24G",
                make_java_command("24G", $params->{"learnit_root"} . "/neolearnit/target/appassembler/bin/", "FilterMappings", "$PARAMS normal $output_file $output_file"));
        # my $attach_combo_pattern_job = run_on_queue_with_prev_job(
        #     "mappings/attach-combo-pattern-${batch_name}-" . $frequency_combination,
        #     $filter_thread_job,
        #     "48G",
        #     make_java_command("48G", $params->{"learnit_root"} . "/neolearnit/target/appassembler/bin/", "AttachLowerRankPatternAtHigherRankTarget", "$PARAMS $output_file $output_file"));
    }

    dojobs();

    my $num_list_batches = $params->{"mappings_list_batches"};
    run_on_queue_now_local("mappings/prepare/make-mappings-sublists-balanced-" . $frequency_combination,
        "$PYTHON3 $expt_dir/scripts/split_batches_by_size.py $source_mappings_dir $num_list_batches ${source_mapping_lists}");
    run_on_queue_now_local("mappings/prepare/filter-mappings-sublists-" . $frequency_combination,
        "$PYTHON3 $expt_dir/scripts/source_mappings_list_filter.py ${source_mapping_lists}"
    );
    return $source_mapping_lists;
}

sub label_mappings_using_extractors {
    (
        my $mappings_lists_dir_local,
        my $extractor_folder,
        my $output_labeled_mappings_path
    ) = @_;
    run_on_queue_now_local("labeling/prepare/make-labeling-dirs",
        "mkdir -p $output_labeled_mappings_path");

    my @batch_source_lists = glob "$mappings_lists_dir_local/*";
    if (!@batch_source_lists) {
        print "Found empty directory for source lists! Cannot extract mappings.\n";
        exit(1);
    }
    my @output_mappings_list_path = ();
    foreach my $batch_list (@batch_source_lists) {
        my ($batch_name, $batch_dir) = fileparse($batch_list);
        my @mappings_list = read_file_into_list($batch_list);
        for my $mappings_path (@mappings_list) {
            my ($mappings_file_name, $mappings_dir) = fileparse($mappings_path);
            my $output_batch_labeled_mappings_path = "$output_labeled_mappings_path/$mappings_file_name.labeled";
            my $learnit_label_jobid = runjobs(
                [], "$job_prefix/labeling/label-${batch_name}-" . $frequency_combination,
                {
                    SGE_VIRTUAL_FREE => "32G",
                    BATCH_QUEUE      => $QUEUE
                },
                [ "$learnit_root/neolearnit/target/appassembler/bin/TargetAndScoreTableLabeler $PARAMS $extractor_folder $mappings_path $output_batch_labeled_mappings_path" ]
            );
            push(@output_mappings_list_path, $output_batch_labeled_mappings_path);
        }
    }
    dojobs();
    return @output_mappings_list_path;
}

sub merge_labeled_mappings {
    my $labeled_mappings_list_path = $_[0];
    my $merged_labeled_mappings_output_path = $_[1];
    my $merge_labeled_mappings_job_id = runjobs(
        [],
        "$job_prefix/labeling/merge_labeled_mappings",
        {
            SGE_VIRTUAL_FREE => "12G",
            BATCH_QUEUE      => $QUEUE,
        },
        [ "$learnit_root/neolearnit/target/appassembler/bin/MergeLabeledMappingsFromList $labeled_mappings_list_path $merged_labeled_mappings_output_path" ]
    );
    dojobs();
}

sub serialize_labeled_mappings {
    my $labeled_mappings_path = $_[0];
    my $output_serif_path = $_[1];

    my $learnit_decoding_mode_to_target_name = {
        "unary_entity"             => "UnaryMention",
        "unary_event_or_event_arg" => "EventAndEventArgument",
        "unary_event"              => "EventAndEventArgument",
        "binary_event_event"       => "EventEventRelation",
    };

    my @batch_source_lists = glob "$CORPUS_DIR/*";
    if (!@batch_source_lists) {
        print "Found empty directory for source lists! Cannot extract mappings.\n";
        exit(1);
    }

    foreach my $batch_list (@batch_source_lists) {
        my ($batch_name, $batch_dir) = fileparse($batch_list);
        my $batch_output = Utils::make_output_dir("$output_serif_path/$batch_name");
        my $learnit_serifxml_jobid = runjobs(
            [], "$job_prefix/serialize/label-$batch_name-$learnit_decoding_mode_to_target_name->{$TARGET}",
            {
                SGE_VIRTUAL_FREE => "32G",
                BATCH_QUEUE      => $QUEUE
            },
            [ "$learnit_root/neolearnit/target/appassembler/bin/SerifXMLSerializer $PARAMS $batch_list $labeled_mappings_path $learnit_decoding_mode_to_target_name->{$TARGET} $batch_output" ]
        );
    }
    dojobs();
    foreach my $batch_list (@batch_source_lists) {
        my ($batch_name, $batch_dir) = fileparse($batch_list);
        my $batch_output = "$output_serif_path/$batch_name";
        generate_file_list([], "$job_prefix/serialize/generate-final-batch-$batch_name-$learnit_decoding_mode_to_target_name->{$TARGET}"
            , "$batch_output/*.xml", "$batch_dir/$batch_name"
        );
    }
}

sub run_serif {
    my $input_sgm_list = $_[0];
    my $awake_db = $_[1];
    my $serif_cause_effect_patterns_dir = $_[2];
    my $project_specific_serif_par = $_[3];
    my $project_specific_serif_data_root = $_[4];
    my $serif_output_path = $_[5];
    my $source_lists_folder = $_[6];

    print "Serif stage\n";
    # Run Serif in parallel
    my $processing_dir = $serif_output_path;
    my $tmp_output_file_list_dir_pending_split = Utils::make_output_dir("$processing_dir/tmp_output_file_list_dir_pending_split");
    my $serif_server_mode_endpoint = "None";
    my $stage_name = "serif";
    my $batch_file_dir = Utils::make_output_dir("$processing_dir/$stage_name/batch_files");
    my $stage_output_folder = Utils::make_output_dir("$processing_dir/$stage_name");
    my ($NUM_JOBS, $split_serif_jobid) = Utils::split_file_for_processing("$job_prefix/$stage_name/make_serif_batch_files", $input_sgm_list, "$batch_file_dir/", $BATCH_SIZE);
    my @serif_jobs = ();
    for (my $n = 0; $n < $NUM_JOBS; $n++) {
        my $job_batch_num = sprintf("%05d", $n);
        my $serif_job_name = "$job_prefix/$stage_name/$job_batch_num";
        my $experiment_dir = "$processing_dir/$stage_name/$job_batch_num";
        my $batch_file = "$batch_file_dir/$job_batch_num";
        my @serif_jobs_in_batch = CSerif::CSerif(
            [ $split_serif_jobid ],
            {
                project_specific_serif_par       => $project_specific_serif_par,
                batch_file                       => $batch_file,
                batch_output_dir                 => $experiment_dir,
                awake_db                         => $awake_db,
                job_name                         => $serif_job_name,
                serif_cause_effect_patterns_dir  => $serif_cause_effect_patterns_dir,
                project_specific_serif_data_root => $project_specific_serif_data_root,
                LINUX_QUEUE                      => $QUEUE,
                serif_cause_effect_output_dir    => "$processing_dir/$stage_name/causal_json"
            }
        );
        for my $serif_job (@serif_jobs_in_batch) {
            push(@serif_jobs, $serif_job);
        }
    }

    dojobs();
    Utils::make_output_dir($source_lists_folder);
    Utils::collect_file_list_with_batch_size(\@serif_jobs, "$job_prefix/$stage_name/generate_batch_lists", "$processing_dir/$stage_name/*/output/*.xml", "$source_lists_folder/", "", $BATCH_SIZE);
    # run_on_queue_now_local("serif/split_final_batch",
    #     "$PYTHON3 $expt_dir/scripts/split_batches_by_num_lines.py $tmp_output_file_list_dir_pending_split $BATCH_SIZE $source_lists_folder");
}

sub create_aggregate_mappings_file {
    (
        my $mappings_lists_dir,
        my $output_mappings_file
    ) = @_;

    run_on_queue_now_local("mappings/create-aggregate-mappings-" . $frequency_combination,
        make_java_command("150G", $params->{"learnit_root"} . "/neolearnit/target/appassembler/bin/", "MergeMappingsForDemo", "$PARAMS $mappings_lists_dir $output_mappings_file"));
    run_on_queue_now_local("mappings/filter-aggregate-mappings-" . $frequency_combination,
        make_java_command("128G", $params->{"learnit_root"} . "/neolearnit/target/appassembler/bin/", "FilterMappings", "$PARAMS aggregated $output_mappings_file $output_mappings_file"));

}

# score_mappings needs to be fixed

sub score_mappings {
    (
        my $mappings_lists_dir,
        my $extractors_list,
        my $scoring_output_dir
    ) = @_;

    # Decoder/Scorer no longer uses an extractor list. However, that can change in near future
    # Currently, this method is not being supported. This can be fixed as soon as extractor-reading part
    # in decoder/scorer is updated.

    print "Scoring is currently not supported.";
    return;

    run_on_queue_now_local("scoring/prepare/make-scoring-dirs",
        "mkdir -p $scoring_output_dir");

    my @batch_mappings_lists = glob "$mappings_lists_dir/*";
    foreach my $batch_list (@batch_mappings_lists) {
        my ($batch_name, $batch_dir) = fileparse($batch_list);
        my $output_json_file = "$scoring_output_dir/$batch_name.decoded.json";
        my $output_file = "$scoring_output_dir/$batch_name.decoder.output";

        run_on_queue("scoring/decode-${batch_name}",
            "75G",
            "sh $expt_dir/scripts/run_EventEventRelationPatternDecoder.sh $PARAMS $batch_list $extractors_list $learnit_root $output_json_file $output_file");
    }

    dojobs();

    run_on_queue_now_local("scoring/aggregate_decoding_output",
        "$PYTHON3 $expt_dir/scripts/decoder_output_aggregator.py $scoring_output_dir $scoring_output_dir/final_event_event_relations.json");

    run_on_queue_now_local("scoring/score-mappings",
        "$PYTHON3 $expt_dir/scripts/score_extracted_event_event_relations.py $scoring_output_dir $scoring_output_dir/event_event_relations.decoder.score");

    return;
}

sub make_java_command {
    (
        my $jvm_xmx_size,
        my $appassembler_dir,
        my $class,
        my $args
    ) = @_;

    #       return "/opt/jdk1.6.0_04-x86_64/bin/java -Xmx32G -cp $JAVA_CP $class $args";
    return "env JAVA_OPTS=\"-Xmx$jvm_xmx_size\"; sh $appassembler_dir$class $args";
}

sub change_dir_and_make_java_command {
    (
        my $dir_to_run,
        my $jvm_xmx_size,
        my $appassembler_dir,
        my $class,
        my $args
    ) = @_;

    return "cd $dir_to_run; env JAVA_OPTS=\"-Xmx$jvm_xmx_size\"; sh $appassembler_dir$class $args";
}

sub run_on_queue_with_prev_job {
    (
        my $job_name,
        my $prev_job,
        my $sge_vm_free,
        my $command
    ) = @_;
    return runjobs([ $prev_job ], "$job_prefix/$job_name", { BATCH_QUEUE => $QUEUE, QUEUE_PRIO => $QUEUE_PRIORITY, SGE_VIRTUAL_FREE => "$sge_vm_free" }, $command);
}

sub run_on_queue {
    (
        my $job_name,
        my $sge_vm_free,
        my $command
    ) = @_;

    return runjobs([], "$job_prefix/$job_name", { BATCH_QUEUE => $QUEUE, QUEUE_PRIO => $QUEUE_PRIORITY, SGE_VIRTUAL_FREE => "$sge_vm_free" }, $command);
}


sub run_on_queue_now_local {
    (
        my $job_name,
        my $command
    ) = @_;

    runjobs([], "$job_prefix/$job_name", { SCRIPT => 1, SGE_VIRTUAL_FREE => "150G" }, $command);
    dojobs();
}


sub read_file_into_list {
    my $file_path = $_[0];

    open my $handle, '<', $file_path or die "Cannot open $file_path: $!";;
    chomp(my @lines = <$handle>);
    close $handle;

    return @lines;
}


sub run_specific_program_on_mappings_list {
    my ($self, %args) = @_;

    my $dependant_job_ids = $args{dependant_job_ids};

    my @batch_mappings_lists = glob "$mappings_lists_dir/*";
    if (!@batch_mappings_lists) {
        die "Found empty directory for source mappings lists!\n";
    }
    foreach my $source_mappings_list (@batch_mappings_lists) {
        my ($batch_name, $batch_dir) = fileparse($source_mappings_list);
        my $stage_output_folder = Utils::make_output_dir("$expt_dir/expts/$job_prefix/specific_program");
        my $learnit_specific_program_jobid = runjobs(
            [], "$job_prefix/specific_program/run-extract-${batch_name}-" . $frequency_combination,
            {
                SGE_VIRTUAL_FREE => "48G",
                BATCH_QUEUE      => $QUEUE
            },
            [ "$learnit_root/neolearnit/target/appassembler/bin/LabelByPreExistingSerifEventMentionType $PARAMS $source_mappings_list $stage_output_folder/$batch_name.labeled.sjson" ]
        );

    }
}

sub build_dictionary {
    my (%args) = @_;

    my $dependant_job_ids = $args{dependant_job_ids};
    my $source_list_dir = $args{source_list_dir};

    my @batch_source_lists = glob "$source_list_dir/*";
    my $stage_output_folder = Utils::make_output_dir("$expt_dir/expts/$job_prefix/tokens");
    foreach my $batch_list (@batch_source_lists) {
        my ($batch_name, $batch_dir) = fileparse($batch_list);
        my $learnit_extract_token_jobid = runjobs(
            $dependant_job_ids, "$job_prefix/extract_token/extract_token-${batch_name}-" . $frequency_combination,
            {
                SGE_VIRTUAL_FREE => "16G",
                BATCH_QUEUE      => $QUEUE
            },
            [ "env PYTHONPATH=/home/hqiu/ld100/Hume_pipeline_2/text-open/src/python $PYTHON3 $learnit_root/helpers/bidoc/get_all_tokens_from_corpus.py --source_lists_folder $batch_list --language arabic --output_path $stage_output_folder/$batch_name.json" ]
        );
    }
}


sub calculate_seed_or_pattern_similarity {
    my (%args) = @_;
    my $dependant_job_ids = $args{dependant_job_ids};
    my $focus_learnit_obversation_obj = $args{focus_learnit_obversation_obj};
    my $number_of_batches = $args{number_of_batches};
    my $mappings_lists_dir = $args{mappings_lists_dir};
    my $job_prefix = $args{job_prefix};
    my $params = $args{params};
    my $number_of_instances_per_learnit_obversation_cap = $args{number_of_instances_per_learnit_obversation_cap};
    my $stage_job_prefix = "$job_prefix/similarity/$focus_learnit_obversation_obj/freq_$frequency_combination";
    (my $stage_output_dir,undef) = Utils::make_output_dir($params->{similarities_dir} . "/$TARGET-$EPOCH/freq_$frequency_combination/$focus_learnit_obversation_obj","$job_prefix/mkdir_master",[]);

    my @extract_instance_from_learnit_obversation_jobs = ();
    {
        # Step 1 dump learnit obversation out
        my @batch_mappings_lists = glob "$mappings_lists_dir/*";
        if (!@batch_mappings_lists) {
            die "Found empty directory for source mappings lists!\n";
        }
        foreach my $source_mappings_list (@batch_mappings_lists) {
            my ($batch_name, $batch_dir) = fileparse($source_mappings_list);
            (my $mini_stage_output_folder,undef) = Utils::make_output_dir($stage_output_dir . "/sample_instances/mini_batch","$stage_job_prefix/sample_instances/mkdir_${batch_name}",[]);
            my $sample_instances_jobid = runjobs(
                $dependant_job_ids, "$stage_job_prefix/sample_instances/${batch_name}",
                {
                    SGE_VIRTUAL_FREE => "48G",
                    BATCH_QUEUE      => $QUEUE
                },
                [ "$learnit_root/neolearnit/target/appassembler/bin/DumpLearnItObversationWithSelectedInstances $PARAMS $source_mappings_list $mini_stage_output_folder/$batch_name.json $focus_learnit_obversation_obj $number_of_instances_per_learnit_obversation_cap" ]
            );
            push(@extract_instance_from_learnit_obversation_jobs, $sample_instances_jobid);
        }
    }

    my $aggr_sampled_instances_jobid;
    my $aggr_sampled_instances_json_path = $stage_output_dir . "/sample_instances/aggr.json";
    {
        my $generate_sample_instance_filelist_jobid = generate_file_list(\@extract_instance_from_learnit_obversation_jobs, "$stage_job_prefix/sample_instances/generate_batch_lists", "$stage_output_dir/sample_instances/mini_batch/*.json", "$stage_output_dir/sample_instances/jsonfile.list");
        # Aggr and cap instances
        $aggr_sampled_instances_jobid = runjobs(
            [ $generate_sample_instance_filelist_jobid ], "$stage_job_prefix/sampled_instances/aggr_job",
            {
                SGE_VIRTUAL_FREE => "24G",
                BATCH_QUEUE      => $QUEUE
            },
            [ "$PYTHON3 $learnit_root/helpers/learnit_obversation_similarity/run.py --mode further_down_sample_representating_instances_for_learnit_obversation --input_learnit_obversation_instance_json_file_list $stage_output_dir/sample_instances/jsonfile.list --output_prefix $aggr_sampled_instances_json_path --number_of_instances_per_learnit_obsersation $number_of_instances_per_learnit_obversation_cap" ]
        )
    }
    my $generate_bert_list_jobid;
    my $generate_bert_list_path = $stage_output_dir . "/extract_embs/bert_npz.list";
    {
        # Step 2, extract bert embs for instances
        (my $mini_stage_output_folder,undef) = Utils::make_output_dir($stage_output_dir . "/extract_embs","$stage_job_prefix/extract_bert_emb/mkdir",[]);
        (my $mini_stage_doc_batch_output_folder,undef) = Utils::make_output_dir($stage_output_dir . "/extract_embs/mini_batch",
            "$stage_job_prefix/extract_bert_emb/mkdir_minibatch",[]);
        my $generate_docid_based_emb_extraction_list_jobid = runjobs(
            [ $aggr_sampled_instances_jobid ], "$stage_job_prefix/extract_bert_emb/generate_doc_based_jobs",
            {
                SGE_VIRTUAL_FREE => "16G",
                BATCH_QUEUE      => $QUEUE
            },
            [ "$PYTHON3 $learnit_root/helpers/learnit_obversation_similarity/run.py --mode generate_docid_based_emb_extraction_list --input_learnit_obversation_instance_json_file $aggr_sampled_instances_json_path --num_of_batches $number_of_batches --output_prefix $mini_stage_doc_batch_output_folder/" ]
        );
        my @split_bert_grabber_jobids = ();
        for (my $i = 0; $i < $number_of_batches; $i++) {
            my $current_instance_folder = "$mini_stage_doc_batch_output_folder/$i";
            (my $mini_stage_batch_output_folder,undef) = Utils::make_output_dir($mini_stage_output_folder . "/bert_emb/$i","$stage_job_prefix/extract_bert_emb/mkdir_grab_bert_$i",[]);
            my $grab_bert_seperate_jobid = runjobs(
                [ $generate_docid_based_emb_extraction_list_jobid ], "$stage_job_prefix/extract_bert_emb/grab_bert_$i",
                {
                    SGE_VIRTUAL_FREE => "8G",
                    BATCH_QUEUE      => $QUEUE
                },
                [ "$PYTHON3 $learnit_root/helpers/learnit_obversation_similarity/run.py --mode grab_bert_emb --bert_npz_list $params->{bert_npz_list} --input_bert_idx_index_file $current_instance_folder --output_prefix $mini_stage_batch_output_folder" ]
            );
            push(@split_bert_grabber_jobids, $grab_bert_seperate_jobid);
        }
        $generate_bert_list_jobid = generate_file_list(\@split_bert_grabber_jobids, "$stage_job_prefix/extract_bert_emb/generate_bert_npz_lists", "$mini_stage_output_folder/bert_emb/*/*.npz", $generate_bert_list_path);
    }


    my $gather_featurelist_jobid;
    my $feature_npz_list = "$stage_output_dir/extract_features/feature.list";
    (my $hume_feature_output_dir,undef) = Utils::make_output_dir($stage_output_dir . "/extract_features/features_npzs","$stage_job_prefix/extract_features/mkdir_feature_npz",[]);

    {
        # Step 3 extract features

        (my $mini_stage_output_dir,undef) = Utils::make_output_dir($stage_output_dir . "/extract_features","$stage_job_prefix/extract_features/mkdir",[]);
        (my $pattern_batch_dir,undef) = Utils::make_output_dir($stage_output_dir . "/extract_features/pattern_batches","$stage_job_prefix/extract_features/mkdir_batch",[]);
        my $divide_patterns_into_batches_jobid = runjobs(
            [ $generate_bert_list_jobid ], "$stage_job_prefix/extract_features/divide_patterns_into_batches",
            {
                SGE_VIRTUAL_FREE => "16G",
                BATCH_QUEUE      => $QUEUE
            },
            [ "$PYTHON3 $learnit_root/helpers/learnit_obversation_similarity/run.py --mode divide_pattern_list_into_batches --num_of_batches $number_of_batches --input_learnit_obversation_instance_json_file $aggr_sampled_instances_json_path --output_prefix $pattern_batch_dir/" ]
        );

        my @split_learnit_obversation_ave_emb_extractor_jobids = ();
        for (my $i = 0; $i < $number_of_batches; $i++) {
            my $current_instance_json = "$pattern_batch_dir/$i.json";
            my $extract_feature_jobid = runjobs(
                [ $divide_patterns_into_batches_jobid ], "$stage_job_prefix/extract_features/extract_feature_$i",
                {
                    SGE_VIRTUAL_FREE => "16G",
                    BATCH_QUEUE      => $QUEUE
                },
                [ "$PYTHON3 $hume_root/src/python/fuzzy_search/similarity/event_and_arg_emb_pairwise/run.py --mode BUILD_LEARNIT_OBVERSATION_FEATURE --input_bert_npz_list $generate_bert_list_path --input_learnit_obversation_instance_json_file $current_instance_json --output_path $hume_feature_output_dir --output_prefix $i " ]
            );
            push(@split_learnit_obversation_ave_emb_extractor_jobids, $extract_feature_jobid);
        }
        $gather_featurelist_jobid = generate_file_list(\@split_learnit_obversation_ave_emb_extractor_jobids, "$stage_job_prefix/extract_features/generate_feature_npz_lists", "$hume_feature_output_dir/*features.npz", $feature_npz_list);
    }


    my $list_annoy_cache_jobid;
    my $list_merged_npz_jobid;

    (my $annoy_cache_dir,undef) = Utils::make_output_dir($stage_output_dir . "/annoy_cache","$stage_job_prefix/annoy_cache/mkdir",[]);
    my $annoy_cache_list = "$annoy_cache_dir/annoy_cache.list";
    my $merged_feature_npz_list = "$annoy_cache_dir/feature.list";
    {
        # Step 4: Build Annoy cache


        my $annoy_metric = "angular";
        my $n_trees = 100;

        my $build_annoy_index_jobid = runjobs(
            [ $gather_featurelist_jobid ], "$stage_job_prefix/annoy_cache/build_annoy_cache",
            {
                BATCH_QUEUE      => $QUEUE,
                SGE_VIRTUAL_FREE => "64G",
            },
            [ "$PYTHON3 $hume_root/src/python/fuzzy_search/similarity/event_and_arg_emb_pairwise/run.py --mode BUILD_ANNOY_INDEX --input_feature_list $feature_npz_list --annoy_metric $annoy_metric --n_trees $n_trees --output_path $annoy_cache_dir" ]
        );

        my $shrink_and_merge_feature_file_jobid = runjobs4::runjobs(
            [ $gather_featurelist_jobid ], "$stage_job_prefix/annoy_cache/shrink_and_merge_feature_file",
            {
                BATCH_QUEUE      => $QUEUE,
                SGE_VIRTUAL_FREE => "32G",
            },
            [ "$PYTHON3 $hume_root/src/python/fuzzy_search/similarity/event_and_arg_emb_pairwise/run.py --mode MERGE_FEATURE_NPZ --input_feature_list $feature_npz_list --should_drop_features_array_when_merging true --output_path $annoy_cache_dir/merged_feature.npz" ]
        );

        $list_annoy_cache_jobid = generate_file_list([ $build_annoy_index_jobid ], "$stage_job_prefix/annoy_cache/build_annoy_cache_list", "$annoy_cache_dir/*.ann", $annoy_cache_list);

        $list_merged_npz_jobid = generate_file_list([ $shrink_and_merge_feature_file_jobid ], "$stage_job_prefix/annoy_cache/merged_feature_npz_list", "$annoy_cache_dir/merged_feature.npz", $merged_feature_npz_list)
    }

    (my $pairwise_cache_dir,undef) = Utils::make_output_dir($stage_output_dir . "/pairwise_cache/cache","$stage_job_prefix/pairwise_cache/mkdir",[]);

    (my $pairwise_tabular_dir,undef) = Utils::make_output_dir($stage_output_dir . "/pairwise_tabular","$stage_job_prefix/pairwise_cache/mkdir_tabular",[]);

    {
        # Step 5 Build Pairwise Cache
        my $key_getter_str = "aveBERTEmb";
        my $annoy_metric = "angular";
        my $threshold = 9999;
        my $cutoff = 50;
        my $feature_name_to_dimension_path = "$annoy_cache_dir/feature_name_to_dimension.npz";
        my $feature_id_to_annoy_idx_npz_path = "$annoy_cache_dir/feature_id_to_annoy_idx.npz";

        (my $ministage_processing_batch_list_dir,undef) = Utils::make_output_dir($stage_output_dir . "/pairwise_cache/batch_list","$stage_job_prefix/pairwise_cache/mkdir_batch",[]);

        my $generate_batch_jobid = runjobs(
            [ $gather_featurelist_jobid ], "$stage_job_prefix/pairwise_cache/split_jobs",
            {
                BATCH_QUEUE      => $QUEUE,
                SGE_VIRTUAL_FREE => "16G",
                SCRIPT           => 1
            },
            [ "$PYTHON3 $textopen_root/src/python/util/common/create_filelist_with_batch_size.py --list_file_path $feature_npz_list --output_list_prefix $ministage_processing_batch_list_dir/ --num_of_batches $number_of_batches --suffix \"\" " ]
        );

        for (my $i = 0; $i < $number_of_batches; $i++) {
            my $batch_file = "$ministage_processing_batch_list_dir/$i";
            my $pairwise_output_path = "$pairwise_cache_dir/$i.npz";
            my $build_pairwise_cache_jobid = runjobs4::runjobs(
                [ $list_annoy_cache_jobid, $generate_batch_jobid ], "$stage_job_prefix/pairwise_cache/build_pairwise_cache_$i",
                {
                    BATCH_QUEUE      => $QUEUE,
                    SGE_VIRTUAL_FREE => "16G",
                },
                [ "$PYTHON3 $hume_root/src/python/fuzzy_search/similarity/event_and_arg_emb_pairwise/run.py --mode BUILD_PAIRWISE_CACHE --input_feature_list $batch_file --output_path $pairwise_output_path --key_getter_str $key_getter_str --annoy_metric $annoy_metric --input_annoy_cache_list $annoy_cache_list --feature_name_to_dimension_path $feature_name_to_dimension_path --feature_id_to_annoy_idx_path $feature_id_to_annoy_idx_npz_path --threshold $threshold --cutoff $cutoff" ]
            );
            my $dump_pairwise_into_learnit_tabular_jobid = runjobs4::runjobs(
                [ $build_pairwise_cache_jobid ], "$stage_job_prefix/dump_pairwise_cache_$i",
                {
                    BATCH_QUEUE      => $QUEUE,
                    SGE_VIRTUAL_FREE => "16G",
                },
                [ "$PYTHON3 $hume_root/src/python/fuzzy_search/similarity/event_and_arg_emb_pairwise/run.py --mode DUMP_PAIRWISE_CACHE_TO_LEARNIT_TABULAR --cutoff $cutoff --sim_matrix_path $pairwise_output_path --output_path $pairwise_tabular_dir/$i.tsv" ]
            );
        }
    }

}


sub generate_file_list {
    my @job_dependencies = @{$_[0]};
    my $create_list_job_name = $_[1];
    my $unix_path_str = $_[2];
    my $output_file_path = $_[3];
    return runjobs(
        \@job_dependencies, $create_list_job_name,
        {
            SCRIPT => 1
        },
        [ "$CREATE_FILE_LIST_SCRIPT --unix_style_pathname \"$unix_path_str\" --output_list_path $output_file_path" ]
    );
}


1;

