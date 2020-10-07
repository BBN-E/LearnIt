#!/usr/bin/perl
use strict;
use warnings FATAL => 'all';

use Cwd 'abs_path';

use File::Basename;
use File::Path;

use Getopt::Long;
use List::Util qw[min max];

my $textopen_root;
my $learnit_root;
my $hume_root;
BEGIN{
    $textopen_root = "/home/hqiu/ld100/text-open";
    $learnit_root = Cwd::abs_path(__FILE__ . "/../..");
    $hume_root = "/home/hqiu/ld100/Hume.new_ir";
    unshift(@INC, abs_path(__FILE__ . "/../../sequences"));
    unshift(@INC, "$textopen_root/src/perl/text_open/lib");
    unshift(@INC, "/d4m/ears/releases/runjobs4/R2019_03_29/lib");
}

use runjobs4;
use ParameterFiles;
use Utils;

package main;

my $PYTHON3 = "/nfs/raid87/u11/users/hqiu/miniconda_prod/envs/py3-ml-general/bin/python3";
my $CREATE_FILE_LIST_SCRIPT = "$textopen_root/src/python/util/common/create_filelist_with_batch_size.py";

my $PARAMS;
my $number_of_batches_global;
our @SAVED_ARGV = @ARGV;
Getopt::Long::Configure("pass_through");
GetOptions(
    "params=s"            => \$PARAMS,
    "number_of_batches=i" => \$number_of_batches_global
); #optional for yield view/seed initialization
Getopt::Long::Configure("no_pass_through");

$number_of_batches_global = int($number_of_batches_global);


# my @targets = ("unary_entity","unary_event","binary_event_event","binary_entity_entity","binary_event_entity_or_value");
# my @targets = ("unary_entity", "unary_event", "binary_event_event", "binary_event_entity_or_value");
my @targets = ("unary_event", "binary_event_event");
my $learnit_jar_path = "$learnit_root/neolearnit/target/neolearnit-2.0-SNAPSHOT-jar-with-dependencies.jar";
my $instance_extractor_endpoint = "java -cp $learnit_jar_path com.bbn.akbc.neolearnit.exec.InstanceExtractor";
my $merge_mappings_endpoint = "java -cp $learnit_jar_path com.bbn.akbc.neolearnit.util.MergeMappingsForDemo";
my $decoding_endpoint = "java -cp $learnit_jar_path com.bbn.akbc.neolearnit.exec.GenerateTrainingDataForNN";

print "Using param file $PARAMS\n";
my $params = ParameterFiles::load_param_file($PARAMS);
my $QUEUE = $params->{"linux_queue"};
my $QUEUE_PRIORITY = $params->{"queue_priority"};
my $corpus_name = $params->{"corpus_name"};
my $hume_ir_proj_root = "$hume_root/src/python/fuzzy_search/similarity/event_and_arg_emb_pairwise";

my ($expt_dir, $exp) = runjobs4::startjobs();
runjobs4::max_jobs(100);

{
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

    my $job_prefix = $corpus_name . "/" . $EPOCH;
    my $serifxml_list;
    if ($EPOCH < 2) {
        $serifxml_list = $params->{"bootstrap_serifxml_list"};
    }
    else {
        my $PREV_EPOCH = $EPOCH - 1;
        $serifxml_list = $params->{"learnit_data_root"} . "/" . $PREV_EPOCH . "/decoding/serifxmls.list";
    }
    my $output_dir = $params->{"learnit_data_root"} . "/" . $EPOCH;
    Utils::make_output_dir("$output_dir/lock", "$job_prefix/mkdir", []);
    my @previous_job_queue = ();
    # Decoding
    if ($EPOCH > 0) {
        (my $decodinglist_collector_job_id, $serifxml_list) = decoding(
            job_dependency    => [],
            job_prefix        => $job_prefix,
            input_serif_list  => $serifxml_list,
            output_dir        => $output_dir,
            number_of_batches => $number_of_batches_global
        );
        push(@previous_job_queue, $decodinglist_collector_job_id);
    }
    # Generate mappings
    # find similar

    my @dep_job_queue = ();
    foreach my $target (@targets) {
        (my $mapping_job_id, my $mappings_path) = generate_mappings(
            job_dependency    => \@previous_job_queue,
            job_prefix        => $job_prefix,
            input_serif_list  => $serifxml_list,
            output_dir        => $output_dir,
            target            => $target,
            number_of_batches => $number_of_batches_global,
        );
        my @mappings_list = ();
        push(@mappings_list, $mappings_path);
        my @calculate_seed_or_pattern_similarity_jobid = calculate_seed_or_pattern_similarity(
            dependant_job_ids                               => [ $mapping_job_id ],
            focus_learnit_obversation_obj                   => "LearnitPattern",
            number_of_batches                               => 5,
            job_prefix                                      => $job_prefix,
            number_of_instances_per_learnit_obversation_cap => 3,
            output_dir                                      => $output_dir,
            target                                          => $target,
            mappings_list                                   => @mappings_list
        );
        for my $similarity_jobid (@calculate_seed_or_pattern_similarity_jobid) {
            push(@dep_job_queue, $similarity_jobid);
        }
    }

    my $remove_lock_jobid = runjobs4::runjobs(
        \@dep_job_queue,
        "$job_prefix/remove_lock",
        {
            BATCH_QUEUE => $QUEUE,
            SCRIPT      => 1
        },
        [ "rm -rf $output_dir/lock" ]
    );
}

runjobs4::dojobs();
runjobs4::endjobs();

sub generate_mappings {
    my %args = @_;
    my $job_prefix = $args{job_prefix};
    my $input_serif_list = $args{input_serif_list};
    my $output_dir = $args{output_dir};
    my $target = $args{target};
    my $number_of_batches = $args{number_of_batches};
    my $job_dependency = $args{job_dependency};
    my $mini_stage = "mappings/$target";
    (my $mappings_dir, undef) = Utils::make_output_dir("$output_dir/$mini_stage/mappings", "$job_prefix/$mini_stage/mkdir_mappings", []);
    (my $mappings_batch_dir, my $mkdir_batch_dir_jobid) = Utils::make_output_dir("$output_dir/$mini_stage/batch", "$job_prefix/$mini_stage/mkdir_batch", $job_dependency);
    my (
        $create_filelist_jobid, @file_list_at_disk
    ) = Utils::split_file_list_with_num_of_batches(
        PYTHON                  => $PYTHON3,
        CREATE_FILELIST_PY_PATH => $CREATE_FILE_LIST_SCRIPT,
        dependant_job_ids       => $mkdir_batch_dir_jobid,
        job_prefix              => "$job_prefix/$mini_stage" . "/create_batch",
        num_of_batches          => $number_of_batches,
        list_file_path          => $input_serif_list,
        output_file_prefix      => $mappings_batch_dir . "/batch_",
        suffix                  => ".list",
    );
    my @instance_extractor_split_jobs = ();
    for (my $batch = 0; $batch < $number_of_batches; $batch++) {
        my $batch_file = "$mappings_batch_dir/batch_$batch.list";
        my $batch_job_prefix = "$job_prefix/$mini_stage/mappings";

        my $MEM_LIMIT = "16G";
        my $instance_extractor_jobid = runjobs4::runjobs(
            $create_filelist_jobid,
            $batch_job_prefix . "/$batch",
            {
                BATCH_QUEUE      => $QUEUE,
                SGE_VIRTUAL_FREE => $MEM_LIMIT
            },
            [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" $instance_extractor_endpoint $PARAMS $target $batch_file $mappings_dir/$batch.sjson" ]
        );
        push(@instance_extractor_split_jobs, $instance_extractor_jobid);
    }
    my $list_collector_job_id =
        runjobs4::runjobs(
            \@instance_extractor_split_jobs,
            "$job_prefix/$mini_stage/" .
                "list_collector",
            {
                BATCH_QUEUE => $QUEUE,
                SCRIPT      => 1
            },
            [
                "find $mappings_dir -name \"*.sjson\" -exec readlink -f {} \\;  " .
                    " | sort -u > $output_dir/$mini_stage/mappings.list"
            ]
        );
    my $MEM_LIMIT = "64G";
    my $merge_mappings_jobid = runjobs4::runjobs(
        [ $list_collector_job_id ], "$job_prefix/$mini_stage/merge_mappings",
        {
            BATCH_QUEUE      => $QUEUE,
            SGE_VIRTUAL_FREE => $MEM_LIMIT
        },
        [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" $merge_mappings_endpoint $PARAMS $output_dir/$mini_stage/mappings.list $output_dir/$mini_stage/mappings.master.sjson" ]
    );
    my $master_mappings_list_collector_job_id =
        runjobs4::runjobs(
            [ $merge_mappings_jobid ],
            "$job_prefix/$mini_stage/" .
                "master_mappings_list_collector",
            {
                BATCH_QUEUE => $QUEUE,
                SCRIPT      => 1
            },
            [
                "find $output_dir/$mini_stage/mappings.master.sjson > $output_dir/$mini_stage/master_mappings.list"
            ]
        );
    return $master_mappings_list_collector_job_id, "$output_dir/$mini_stage/master_mappings.list";
}

sub decoding {
    my %args = @_;
    my $job_prefix = $args{job_prefix};
    my $input_serif_list = $args{input_serif_list};
    my $output_dir = $args{output_dir};
    my $number_of_batches = $args{number_of_batches};
    my $job_dependency = $args{job_dependency};

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
        my $targetStr = join ",", @targets;
        my $learnit_decoder_jobid = runjobs4::runjobs(
            $mkdir_batch_jobid,
            $batch_job_prefix . "/learnit_decoder",
            {
                BATCH_QUEUE      => $QUEUE,
                SGE_VIRTUAL_FREE => $MEM_LIMIT
            },
            [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" $decoding_endpoint $PARAMS $batch_file $batch_serifxml_output_dir $targetStr false" ]
        );
        push(@decoding_split_jobs, $learnit_decoder_jobid);
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
    return $list_collector_job_id, "$output_dir/$mini_stage/serifxmls.list";
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
        [ "find $unix_path_str > $output_file_path" ]
    );
}

sub calculate_seed_or_pattern_similarity {
    my (%args) = @_;
    my $dependant_job_ids = $args{dependant_job_ids};
    my $focus_learnit_obversation_obj = $args{focus_learnit_obversation_obj};
    my $number_of_batches = $args{number_of_batches};
    my @mappings_list = $args{mappings_list};
    my $job_prefix = $args{job_prefix};
    my $target = $args{target};
    my $number_of_instances_per_learnit_obversation_cap = $args{number_of_instances_per_learnit_obversation_cap};
    my $stage_job_prefix = "$job_prefix/similarity/$target/$focus_learnit_obversation_obj";
    my $output_dir = "$args{output_dir}/similarity/$target/$focus_learnit_obversation_obj";
    (my $stage_output_dir, undef) = Utils::make_output_dir($output_dir, "$stage_job_prefix/mkdir_master", []);

    my @extract_instance_from_learnit_obversation_jobs = ();
    {
        # Step 1 dump learnit obversation out

        foreach my $source_mappings_list (@mappings_list) {
            my ($batch_name, $batch_dir) = fileparse($source_mappings_list);
            (my $mini_stage_output_folder, undef) = Utils::make_output_dir($stage_output_dir . "/sample_instances/mini_batch", "$stage_job_prefix/sample_instances/mkdir_${batch_name}", []);
            my $sample_instances_jobid = runjobs(
                $dependant_job_ids, "$stage_job_prefix/sample_instances/${batch_name}",
                {
                    SGE_VIRTUAL_FREE => "8G",
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
    my $generate_docid_based_emb_extraction_output_jobid;
    my $generate_docid_based_emb_extraction_output_list;
    {
        # Step 2, extract bert embs for instances
        (my $mini_stage_output_folder, undef) = Utils::make_output_dir($stage_output_dir . "/extract_embs", "$stage_job_prefix/extract_bert_emb/mkdir", []);
        (my $mini_stage_doc_batch_output_folder, undef) = Utils::make_output_dir($stage_output_dir . "/extract_embs/mini_batch",
            "$stage_job_prefix/extract_bert_emb/mkdir_minibatch", []);
        my $generate_docid_based_emb_extraction_list_jobid = runjobs(
            [ $aggr_sampled_instances_jobid ], "$stage_job_prefix/extract_bert_emb/generate_doc_based_jobs",
            {
                SGE_VIRTUAL_FREE => "16G",
                BATCH_QUEUE      => $QUEUE
            },
            [ "$PYTHON3 $learnit_root/helpers/learnit_obversation_similarity/run.py --mode generate_docid_based_emb_extraction_list --input_learnit_obversation_instance_json_file $aggr_sampled_instances_json_path --num_of_batches $number_of_batches --output_prefix $mini_stage_doc_batch_output_folder/" ]
        );
        $generate_docid_based_emb_extraction_output_jobid =
            runjobs4::runjobs(
                [ $generate_docid_based_emb_extraction_list_jobid ],
                "$stage_job_prefix/extract_features/" .
                    "list_collector_previous",
                {
                    BATCH_QUEUE => $QUEUE,
                    SCRIPT      => 1
                },
                [
                    "find $mini_stage_doc_batch_output_folder -name \"*.json\" -exec readlink -f {} \\;  " .
                        " | sort -u > $mini_stage_output_folder/per_document_extraction.list"
                ]
            );
        $generate_docid_based_emb_extraction_output_list = "$mini_stage_output_folder/per_document_extraction.list";
    }
    my $gather_featurelist_jobid;
    my $feature_npz_list;

    {
        # Step 3 extract features
        (my $mini_stage_output_folder, undef) = Utils::make_output_dir($stage_output_dir . "/extract_features", "$stage_job_prefix/extract_features/mkdir", []);
        (my $mini_stage_doc_batch_output_folder, undef) = Utils::make_output_dir($stage_output_dir . "/extract_features/mini_batch",
            "$stage_job_prefix/extract_features/mkdir_minibatch", []);

        my $generate_batch_jobid = runjobs(
            [ $generate_docid_based_emb_extraction_output_jobid ], "$stage_job_prefix/extract_features/split_jobs",
            {
                SCRIPT => 1
            },
            [ "$PYTHON3 $textopen_root/src/python/util/common/create_filelist_with_batch_size.py --list_file_path $generate_docid_based_emb_extraction_output_list --output_list_prefix $mini_stage_doc_batch_output_folder/ --num_of_batches $number_of_batches --suffix \"\" " ]
        );
        my @extract_feature_split_jobs = ();
        for (my $n = 0; $n < $number_of_batches; $n++) {
            my $batch_file = "$mini_stage_doc_batch_output_folder/$n";
            my ($batch_output_dir, undef) = Utils::make_output_dir("$mini_stage_output_folder/feature_mapper/$n", "$stage_job_prefix/extract_features/feature_mapper/mkdir_$n", []);
            my $feature_extractor_job_id =
                runjobs4::runjobs(
                    [ $generate_batch_jobid ], "$stage_job_prefix/extract_features/feature_mapper/extract_feature_$n",
                    {
                        BATCH_QUEUE        => $QUEUE,
                        input_feature_list => $batch_file,
                        input_bert_list    => $params->{bert_npz_list},
                        output_path        => $batch_output_dir,
                        cap                => $number_of_instances_per_learnit_obversation_cap,
                        number_of_batches  => $number_of_batches,
                        SGE_VIRTUAL_FREE => "16G",
                    },
                    [ "env PYTHONPATH=$textopen_root/src/python $PYTHON3 $hume_ir_proj_root/pipeline/extracting.py", "feature_extractor.json", "feature_1" ]
                );
            push(@extract_feature_split_jobs, $feature_extractor_job_id);
        }
        my $list_feature_extraction_mapper_jobid = runjobs(
            \@extract_feature_split_jobs, "$stage_job_prefix/extract_features/feature_mapper/list_feature_extraction",
            {
                SCRIPT => 1
            },
            [ "find $mini_stage_output_folder/feature_mapper -type f -name 'features.npz' > $mini_stage_output_folder/features_mapper.list" ]);

        (my $feature_reducer_output_folder, undef) = Utils::make_output_dir($mini_stage_output_folder . "/feature_reducer", "$stage_job_prefix/extract_features/feature_reducer/mkdir", []);
        (my $feature_reducer_output_folder_batch, undef) = Utils::make_output_dir($mini_stage_output_folder . "/feature_reducer/batch", "$stage_job_prefix/extract_features/feature_reducer/mkdir_batch", []);
        my $create_feature_batch_index_jobid = runjobs4::runjobs(
            [ $list_feature_extraction_mapper_jobid ],
            "$stage_job_prefix/extract_features/feature_reducer/divide_mapper_feature_into_list",
            {
                BATCH_QUEUE        => $QUEUE,
                partial_expansion  => 1,
                input_feature_list => "$mini_stage_output_folder/features_mapper.list",
                output_path        => $feature_reducer_output_folder_batch,
                cap                => "",
                input_bert_list    => "",
                number_of_batches  => $number_of_batches
            },
            [ "$PYTHON3 $hume_ir_proj_root/utils/create_feature_batches.py", "feature_extractor.json", "feature_1" ]
        );
        my @merge_feature_jobs = ();
        for (my $n = 0; $n < $number_of_batches; $n++) {
            my $batch_file = "$feature_reducer_output_folder_batch/$n.list";
            my ($batch_output_dir, undef) = Utils::make_output_dir("$mini_stage_output_folder/feature_reducer/$n", "$stage_job_prefix/extract_features/feature_reducer/mkdir_$n", []);
            my $merge_feature_extractor_job_id =
                runjobs4::runjobs(
                    [ $create_feature_batch_index_jobid ], "$stage_job_prefix/extract_features/feature_reducer/merge_feature_$n",
                    {
                        BATCH_QUEUE        => $QUEUE,
                        partial_expansion  => 1,
                        input_feature_list => "$mini_stage_output_folder/features_mapper.list",
                        output_path        => "$batch_output_dir",
                        cap                => $number_of_instances_per_learnit_obversation_cap,
                        input_bert_list    => "",
                        number_of_batches  => $number_of_batches,
                        SGE_VIRTUAL_FREE => "16G",
                    },
                    [ "$PYTHON3 $hume_ir_proj_root/pipeline/extracting_merger.py", "feature_extractor.json", "feature_1 $batch_file" ]
                );
            push(@merge_feature_jobs, $merge_feature_extractor_job_id);
        }

        my $list_feature_extraction_jobid = runjobs(
            \@merge_feature_jobs, "$stage_job_prefix/extract_features/feature_reducer/list_feature_extraction",
            {
                SCRIPT => 1
            },
            [ "find $mini_stage_output_folder/feature_reducer -type f -name 'features.npz' > $mini_stage_output_folder/feature_reducer/features.list" ]
        );
        $feature_npz_list = "$mini_stage_output_folder/feature_reducer/features.list";
        $gather_featurelist_jobid = $list_feature_extraction_jobid;
    }

    my $annoy_cache_list_path;
    my $annoy_cache_merge_jobid;

    {
        # Step 4: Build Annoy cache
        (my $annoy_cache_dir, undef) = Utils::make_output_dir($stage_output_dir . "/annoy_cache", "$stage_job_prefix/annoy_cache/mkdir", []);
        (my $annoy_cache_dir_batch, undef) = Utils::make_output_dir($stage_output_dir . "/annoy_cache/batch", "$stage_job_prefix/annoy_cache/mkdir_batch", []);

        my ($split_jobid, undef) = Utils::split_file_list_with_num_of_batches(
            PYTHON                  => $PYTHON3,
            CREATE_FILELIST_PY_PATH => "$textopen_root/src/python/util/common/create_filelist_with_batch_size.py",
            num_of_batches          => $number_of_batches,
            suffix                  => "",
            output_file_prefix      => "$annoy_cache_dir_batch/",
            list_file_path          => $feature_npz_list,
            job_prefix              => "$stage_job_prefix/annoy_cache/",
            dependant_job_ids       => [ $gather_featurelist_jobid ],
        );
        my @index_split_jobs = ();
        for (my $n = 0; $n < $number_of_batches; $n++) {
            my $batch_file = "$annoy_cache_dir_batch/$n";
            my ($batch_output_dir, undef) = Utils::make_output_dir("$annoy_cache_dir/$n", "$stage_job_prefix/annoy_cache/mkdir_$n", []);
            my $index_job_id =
                runjobs4::runjobs(
                    $split_jobid, "$stage_job_prefix/annoy_cache/build_index_$n",
                    {
                        BATCH_QUEUE        => $QUEUE,
                        input_feature_list => "$batch_file",
                        output_path        => "$batch_output_dir",
                        SGE_VIRTUAL_FREE => "16G",
                    },
                    [ "env PYTHONPATH=$textopen_root/src/python $PYTHON3 $hume_ir_proj_root/pipeline/indexing.py", "annoy_index.json", "index_1" ]
                );
            push(@index_split_jobs, $index_job_id);
        }
        my $list_index_jobid = runjobs(
            \@index_split_jobs, "$stage_job_prefix/annoy_cache/list_index",
            {
                SCRIPT => 1
            },
            [ "find $annoy_cache_dir -type f -name 'cache_config.json' > $annoy_cache_dir/index.list" ]
        );
        $annoy_cache_merge_jobid = $list_index_jobid;
        $annoy_cache_list_path = "$annoy_cache_dir/index.list";
    }
    (my $pairwise_tabular_dir, undef) = Utils::make_output_dir($stage_output_dir . "/pairwise_tabular", "$stage_job_prefix/pairwise_cache/mkdir_tabular", []);
    my @dep_job_ids = ();
    {
        # Step 5 Query and dump
        (my $query_dir, undef) = Utils::make_output_dir($stage_output_dir . "/query", "$stage_job_prefix/query/mkdir", []);
        (my $query_batch, undef) = Utils::make_output_dir($stage_output_dir . "/query/batch", "$stage_job_prefix/query/mkdir_batch", []);
        my ($split_jobid, undef) = Utils::split_file_list_with_num_of_batches(
            PYTHON                  => $PYTHON3,
            CREATE_FILELIST_PY_PATH => "$textopen_root/src/python/util/common/create_filelist_with_batch_size.py",
            num_of_batches          => $number_of_batches,
            suffix                  => "",
            output_file_prefix      => "$query_batch/",
            list_file_path          => $feature_npz_list,
            job_prefix              => "$stage_job_prefix/query/",
            dependant_job_ids       => [ $annoy_cache_merge_jobid ],
        );

        my @query_split_jobs = ();
        for (my $n = 0; $n < $number_of_batches; $n++) {
            my $batch_file = "$query_batch/$n";
            my ($batch_output_dir, undef) = Utils::make_output_dir("$stage_output_dir/query/$n", "$stage_job_prefix/query/mkdir_$n", []);
            my $query_job_id =
                runjobs4::runjobs(
                    $split_jobid, "$stage_job_prefix/query/query_$n",
                    {
                        BATCH_QUEUE        => $QUEUE,
                        query_feature_list => $batch_file,
                        input_cache_list   => $annoy_cache_list_path,
                        output_path        => $batch_output_dir,
                        SGE_VIRTUAL_FREE => "16G",
                    },
                    [ "env PYTHONPATH=$textopen_root/src/python $PYTHON3 $hume_ir_proj_root/pipeline/querying.py", "query.json", "query_1" ]
                );

            my $my_output_list = runjobs(
                [ $query_job_id ], "$stage_job_prefix/query/list_query_result_$n",
                {
                    SCRIPT => 1
                },
                [ "find $batch_output_dir -type f -name 'sim.npz' > $batch_output_dir/sim.list" ]
            );

            my $serialize_job_id =
                runjobs4::runjobs(
                    [ $my_output_list ], "$stage_job_prefix/query/dump_$n",
                    {
                        BATCH_QUEUE           => $QUEUE,
                        input_sim_matrix_list => "$batch_output_dir/sim.list",
                        output_path           => "$pairwise_tabular_dir/$n.tsv"
                    },
                    [ "env PYTHONPATH=$textopen_root/src/python $PYTHON3 $hume_ir_proj_root/pipeline/dumping.py", "dump.json", "dumper_1" ]
                );
            push(@dep_job_ids, $serialize_job_id);
        }
    }
    return @dep_job_ids;
}

1;