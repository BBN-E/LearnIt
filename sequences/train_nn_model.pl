#!/usr/bin/perl
use strict;
use warnings FATAL => 'all';

use Cwd 'abs_path';

my $textopen_root;
my $learnit_root;
my $nlplingo_root;
my $nn_event_retraining_root;


BEGIN{
    $textopen_root = "/home/hqiu/ld100/text-open";
    $learnit_root = abs_path(__FILE__ . "/../../");
    $nlplingo_root = "/d4m/nlp/releases/nlplingo/R2020_06_02";
    $nn_event_retraining_root = "/home/hqiu/ld100/nn-event-retraining";
    unshift(@INC, "$textopen_root/src/perl/text_open/lib");
    unshift(@INC, "$learnit_root/lib/perl_lib/");
    unshift(@INC, "$learnit_root/sequences");
    unshift(@INC, "$nn_event_retraining_root/lib/perl_lib");
    unshift(@INC, "/d4m/ears/releases/runjobs4/R2019_03_29/lib");
}
use constants;
use Utils;
use runjobs4;
use LearnItDecoding;
use NNEventTraining;

use ParameterFiles;
use Getopt::Long;

my $PARAMS;
my $NUM_OF_BATCHES_GLOBAL;

our @SAVED_ARGV = @ARGV;
Getopt::Long::Configure("pass_through");
GetOptions(
    "params=s"            => \$PARAMS,
    "number_of_batches=s" => \$NUM_OF_BATCHES_GLOBAL,
); #optional for yield view/seed initialization
Getopt::Long::Configure("no_pass_through");

our $params = ParameterFiles::load_param_file($PARAMS);

my $expt_name = $params->{"corpus_name"};
my $job_prefix = "$expt_name" . "_decoding";

my $QUEUE = $params->{"linux_queue"};
my $QUEUE_PRIORITY = $params->{"queue_priority"};

my ($expt_dir, $exp) = runjobs4::startjobs();

my $PYTHON3 = "/nfs/raid87/u11/users/hqiu/miniconda_prod/envs/nlplingo-gpu/bin/python";
my $learnit_jar_path = "$learnit_root/neolearnit/target/neolearnit-2.0-SNAPSHOT-jar-with-dependencies.jar";
my $decoding_endpoint = "java -cp $learnit_jar_path com.bbn.akbc.neolearnit.exec.GenerateTrainingDataForNN";
my $copy_yaml_endpoint = "java -cp $learnit_jar_path com.bbn.akbc.neolearnit.util.CopyOntologyFile";
my $CREATE_FILE_LIST_SCRIPT = "$textopen_root/src/python/util/common/create_filelist_with_batch_size.py";


my $learnit_decoder_jobids;
my $output_learnit_seriflist_path;

my @targets = ("unary_event", "binary_event_event");

{
    my $mini_stage = "generate_training_data_for_nn";
    my $EPOCH = 0;

    if (-d $params->{"learnit_data_root"}) {
        opendir my $dir, $params->{"learnit_data_root"} or die "Cannot open directory: $!";
        my @files = readdir $dir;
        foreach my $file (@files) {
            next if ($file =~ /^\.$/);
            next if ($file =~ /^\.\.$/);
            if (-d $params->{"learnit_data_root"} . "/" . $file) {
                if (-d $params->{"learnit_data_root"} . "/" . $file . "/lock") {
                    $EPOCH = max($EPOCH, int($file));
                }
                else {
                    $EPOCH = max($EPOCH, int($file) + 1);
                }
            }
        }
        closedir $dir;
    }

    (my $decoding_dir, undef) = Utils::make_output_dir("$expt_dir/expts/$job_prefix/$mini_stage", "$job_prefix/$mini_stage/mkdir_decoding_directory", []);
    my @enabled_targets = ("unary_event", "binary_event_event");

    my $serifxml_list;
    if ($EPOCH < 2) {
        $serifxml_list = $params->{"bootstrap_serifxml_list"};
    }
    else {
        my $PREV_EPOCH = $EPOCH - 1;
        $serifxml_list = $params->{"learnit_data_root"} . "/" . $PREV_EPOCH . "/decoding/serifxmls.list";
    }
    foreach my $target (@targets) {
        (my $target_decoding_dir, undef) = Utils::make_output_dir("$expt_dir/expts/$job_prefix/$mini_stage/$target", "$job_prefix/$mini_stage/$target/mkdir_decoding_directory", []);
        ($learnit_decoder_jobids, $output_learnit_seriflist_path) = output_serifxmls(
            job_dependency    => [],
            job_prefix        => "$job_prefix/$mini_stage/$target",
            input_serif_list  => $serifxml_list,
            output_dir        => $target_decoding_dir,
            number_of_batches => $NUM_OF_BATCHES_GLOBAL,
            targets           => [$target]
        );
    }
}

runjobs4::dojobs();

# {
#     my $mini_stage = "train_nlplingo_event_model";
#     my $nn_event_training_obj = NNEventTraining->new(
#         TEXT_OPEN       => $textopen_root,
#         PYTHON3         => $PYTHON3,
#         LINUX_CPU_QUEUE => $LINUX_CPU_QUEUE,
#         LINUX_GPU_QUEUE => $LINUX_GPU_QUEUE,
#         NLPLINGO        => $nlplingo_root,
#         HUME            => "",
#         exp_root        => $expt_dir,
#         exp             => $exp,
#         SH              => "/bin/bash"
#     );
#
#     $nn_event_training_obj->main(
#         exptName                   => "$job_prefix/$mini_stage",
#         input_serifxml             => $output_learnit_seriflist_path,
#         negative_event_type_prefix => "Negative_",
#         num_batches                => $NUM_OF_BATCHES_GLOBAL,
#         max_per_token              => 50,
#         trusted_rate               => 0.5,
#         minimum_per_type           => 25,
#         input_bert                 => $params->{bert_npz_list},
#         use_mask                   => "false",
#         use_whitelist              => "false",
#         tune_batches               => "15,20",
#         tune_layers                => "768",
#         tune_neighbors             => 0,
#         tune_rates                 => 0.001,
#         tune_epochs                => "20,30,40",
#         train_prop                 => 75,
#         test_prop                  => 10,
#         entity_mention_anchors     => "false",
#         current_model              => undef,
#         install                    => "false",
#         archive_dir                => undef,
#         model_dir                  => undef
#
#     );
# }

runjobs4::dojobs();

runjobs4::endjobs();

sub output_serifxmls {
    my %args = @_;
    my $job_prefix = $args{job_prefix};
    my $input_serif_list = $args{input_serif_list};
    my $output_dir = $args{output_dir};
    my $number_of_batches = $args{number_of_batches};
    my $job_dependency = $args{job_dependency};
    my @enabled_targets = @{$args{targets}};

    my $mini_stage = "decoding";
    (my $decoding_dir, undef) = Utils::make_output_dir("$output_dir/$mini_stage", "$job_prefix/$mini_stage/mkdir_", []);
    (my $decoding_batch_dir, my $mkdir_batch_dir_jobid) = Utils::make_output_dir("$output_dir/$mini_stage/batch", "$job_prefix/$mini_stage/mkdir_batch", $job_dependency);
    my (
        $create_filelist_jobid, @file_list_at_disk
    ) = Utils::split_file_list_with_num_of_batches(
        PYTHON                  => $PYTHON3,
        CREATE_FILELIST_PY_PATH => $CREATE_FILE_LIST_SCRIPT,
        dependant_job_ids       => $mkdir_batch_dir_jobid,
        job_prefix              => "$job_prefix/$mini_stage" . "/create_batch",
        num_of_batches          => $number_of_batches,
        list_file_path          => $input_serif_list,
        output_file_prefix      => $decoding_batch_dir . "/batch_",
        suffix                  => ".list",
    );
    my @decoding_split_jobs = ();
    for (my $batch = 0; $batch < $number_of_batches; $batch++) {
        my $batch_file = "$decoding_batch_dir/batch_$batch.list";
        my $batch_job_prefix = "$job_prefix/$mini_stage/decoding_$batch";
        my ($batch_output_dir, $mkdir_batch_jobid) = Utils::make_output_dir("$output_dir/$mini_stage/decoding/$batch", "$batch_job_prefix/mkdir_batch", $create_filelist_jobid);
        my ($batch_serifxml_output_dir, $mkdir_serifxml_batch_jobid) = Utils::make_output_dir("$batch_output_dir/serifxmls", "$batch_job_prefix/mkdir_serifxml_batch", $create_filelist_jobid);
        my $MEM_LIMIT = "16G";
        my $targetStr = join ",", @enabled_targets;
        my $learnit_decoder_jobid = runjobs4::runjobs(
            $mkdir_batch_jobid,
            $batch_job_prefix . "/learnit_decoder",
            {
                BATCH_QUEUE      => $QUEUE,
                SGE_VIRTUAL_FREE => $MEM_LIMIT
            },
            [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" $decoding_endpoint $PARAMS $batch_file $batch_serifxml_output_dir $targetStr true" ]
        );
        push(@decoding_split_jobs, $learnit_decoder_jobid);
    }
    my @pending_job_queue = ();
    foreach my $targetStr (@enabled_targets){
        my $copy_ontology_file_job_id = runjobs4::runjobs(
            $mkdir_batch_dir_jobid,
            "$job_prefix/$mini_stage/copy_ontology_yaml_$targetStr",
            {
                BATCH_QUEUE => $QUEUE,
            },
            [
                "$copy_yaml_endpoint $PARAMS $targetStr $output_dir/$mini_stage"
            ]
        );
        push(@pending_job_queue,$copy_ontology_file_job_id);
    }
    my $list_collector_job_id =
        runjobs4::runjobs(
            \@decoding_split_jobs,
            "$job_prefix/$mini_stage/" .
                "list_collector",
            {
                BATCH_QUEUE => $QUEUE,
                SCRIPT      => 1
            },
            [
                "find $output_dir/$mini_stage/decoding/ -name \"*.xml\" -exec readlink -f {} \\;  " .
                    " | sort -u > $output_dir/$mini_stage/serifxmls.list"
            ]
        );
    push(@pending_job_queue,$list_collector_job_id);
    return @pending_job_queue, "$output_dir/$mini_stage/serifxmls.list";
}

1;