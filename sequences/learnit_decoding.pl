use strict;
use warnings FATAL => 'all';

use lib "/d4m/ears/releases/runjobs4/R2019_03_29/lib";

use Cwd 'abs_path';

my $textopen_root;
my $learnit_root;
BEGIN{
    $textopen_root = "/home/hqiu/ld100/Hume_pipeline_2/text-open";
    $learnit_root = abs_path(__FILE__ . "/../../");
    unshift(@INC, "$textopen_root/src/perl/text_open/lib");
    unshift(@INC, "$learnit_root/lib/perl_lib/");
    unshift(@INC, "$learnit_root/sequences");
    unshift(@INC, "/d4m/ears/releases/runjobs4/R2019_03_29/lib");
}
use constants;
use Utils;
use runjobs4;
use ParameterFiles;
use Getopt::Long;

my $PYTHON3 = "/home/hqiu/ld100/miniconda_dev/envs/hat_new/bin/python3";
my $CREATE_FILE_LIST_SCRIPT = "$textopen_root/src/python/util/common/create_filelist_with_batch_size.py";

my $PARAMS;
my $FILELIST;
my $NUM_OF_BATCHES_GLOBAL;

our @SAVED_ARGV = @ARGV;
Getopt::Long::Configure("pass_through");
GetOptions(
    "params=s"            => \$PARAMS,
    "filelist=s"          => \$FILELIST,
    "number_of_batches=s" => \$NUM_OF_BATCHES_GLOBAL,
); #optional for yield view/seed initialization
Getopt::Long::Configure("no_pass_through");

our $params = ParameterFiles::load_param_file($PARAMS);

my $expt_name = $params->{"corpus_name"};
my $job_prefix = "$expt_name" . "_decoding";


####### START RUNJOBS ###########
my ($expt_dir, $exp) = runjobs4::startjobs();
runjobs4::max_jobs(100);
(my $processing_dir, undef) = Utils::make_output_dir("$expt_dir/expts/$job_prefix", "$job_prefix/mkdir_job_directory", []);

our $QUEUE = $params->{"linux_queue"};
our $QUEUE_PRIORITY = $params->{"queue_priority"};
my @stages = split(/,/, $params->{"stages_to_run"});

{
    {
        my $mini_stage = "generate_ui_data";
        # Generate UI data
        (my $ui_data_dir, undef) = Utils::make_output_dir("$expt_dir/expts/$job_prefix/$mini_stage", "$job_prefix/$mini_stage/mkdir_ui_data_directory", []);
        (my $ui_data_batch_dir, my $mkdir_batch_dir_jobid) = Utils::make_output_dir("$expt_dir/expts/$job_prefix/$mini_stage/batch", "$job_prefix/$mini_stage/mkdir_ui_data_batch_directory", []);
        my (
            $create_filelist_jobid, @file_list_at_disk
        ) = Utils::split_file_list_with_num_of_batches(
            PYTHON                  => $PYTHON3,
            CREATE_FILELIST_PY_PATH => $CREATE_FILE_LIST_SCRIPT,
            dependant_job_ids       => $mkdir_batch_dir_jobid,
            job_prefix              => "$job_prefix/$mini_stage" . "/create_batch",
            num_of_batches          => $NUM_OF_BATCHES_GLOBAL,
            list_file_path          => $FILELIST,
            output_file_prefix      => $ui_data_batch_dir . "/batch_",
            suffix                  => ".list",
        );
        my @ui_data_split_jobs = ();
        for (my $batch = 0; $batch < $NUM_OF_BATCHES_GLOBAL; $batch++) {
            my $batch_file = "$ui_data_batch_dir/batch_$batch.list";
            my $batch_job_prefix = "$job_prefix/$mini_stage/$batch";
            my ($batch_output_dir, $mkdir_batch_jobid) = Utils::make_output_dir("$expt_dir/expts/$job_prefix/$mini_stage/$batch", "$batch_job_prefix/mkdir_batch", $create_filelist_jobid);
            my ($batch_serifxml_output_dir, $mkdir_serifxml_batch_jobid) = Utils::make_output_dir("$batch_output_dir/serifxmls", "$batch_job_prefix/mkdir_serifxml_batch", $create_filelist_jobid);
            my $MEM_LIMIT = "16G";
            my $learnit_decoder_jobid = runjobs4::runjobs(
                $mkdir_batch_jobid,
                $batch_job_prefix . "/learnit_decoder",
                {
                    BATCH_QUEUE => $main::QUEUE,
                    SGE_VIRTUAL_FREE => $MEM_LIMIT
                },
                [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" java -cp $learnit_root/neolearnit/target/neolearnit-2.0-SNAPSHOT-jar-with-dependencies.jar com.bbn.akbc.neolearnit.exec.GenerateTrainingDataForNN $PARAMS $batch_file $batch_serifxml_output_dir"]
            );

            my $list_collector_job_id =
                runjobs4::runjobs(
                    [$learnit_decoder_jobid],
                    "$batch_job_prefix/" .
                        "list_collector",
                    {
                        BATCH_QUEUE => $main::QUEUE,
                        SCRIPT      => 1
                    },
                    [
                        "find $batch_serifxml_output_dir -name \"*.xml\" -exec readlink -f {} \\;  " .
                            " | sort -u > $batch_output_dir/serifxmls.list"
                    ]
                );

            my $generate_ui_data_jobid = runjobs4::runjobs(
                [$list_collector_job_id], "$batch_job_prefix/generate_ui_data",
                {
                    BATCH_QUEUE      => $QUEUE,
                    SGE_VIRTUAL_FREE => "8G"
                },
                [ "env PYTHONPATH=$textopen_root/src/python:\$PYTHONPATH $PYTHON3 $learnit_root/HAT/new_backend/utils/prepare_connective_graph_from_serifxml.py --input_serif_list $batch_output_dir/serifxmls.list --output_path $batch_output_dir/output.graph.json" ]

            );
            push(@ui_data_split_jobs, $generate_ui_data_jobid);
        }
        my $aggr_ui_data_jobid = runjobs4::runjobs(
            \@ui_data_split_jobs, "$job_prefix/$mini_stage/generate_ui_data",
            {
                BATCH_QUEUE      => $QUEUE,
                SGE_VIRTUAL_FREE => "8G"
            },
            [ "env PYTHONPATH=$textopen_root/src/python:\$PYTHONPATH $PYTHON3 $learnit_root/HAT/new_backend/utils/aggr_ui_data_from_kb_constructor.py --dir_of_serialization $expt_dir/expts/$job_prefix/$mini_stage --output_path $expt_dir/expts/$job_prefix/$mini_stage/eer_graph.json" ]
        );
    }
}



runjobs4::dojobs();
runjobs4::endjobs();