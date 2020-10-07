package LearnItDecoding;
use strict;
use warnings FATAL => 'all';

use Carp;

use Utils;
use runjobs4;



sub new {
    my ($class, %rest) = @_;
    my $self;

    if (%rest) {
        $self = \%rest;
    }
    else {
        $self = {};
    }

    bless $self, $class;

    my @fs = $self->fields();

    # Check the passed fields
    foreach my $k (keys %{$self}) {
        Carp::croak "In new(), $class doesn't have a $k field but was given it as a parameter" unless grep ( /^$k$/, @fs );
    }

    my @missing = grep {!defined($self->{$_})} @fs;
    if (@missing) {
        Carp::croak "In new(), $class not passed mandatory field(s): @missing\n";
    }

    $self->init();

    return $self;
}

sub init {
    my $self = shift;
    $self->{TEXT_OPEN_PYTHONPATH} = Cwd::abs_path($self->{TEXT_OPEN} . "/src/python");
    $self->{CREATE_FILELIST_PY_PATH} = Cwd::abs_path($self->{TEXT_OPEN_PYTHONPATH} . "/util/common/create_filelist_with_batch_size.py");
    $self->{LEARNIT_ROOT} = Cwd::abs_path(__FILE__ . "/../../../");
    $self->{LEARNIT_JAR} = "$self->{LEARNIT_ROOT}/neolearnit/target/neolearnit-2.0-SNAPSHOT-jar-with-dependencies.jar";
}

sub fields {
    return(
        "TEXT_OPEN",
        "PYTHON3",
        "BATCH_QUEUE",
        "MEM_LIMIT"
    );
}


sub SerializeLabeledMappingsIntoSerifXML {
    my ($self, %args) = @_;
    my $dependant_job_ids = $args{dependant_job_ids};
    my $job_prefix = $args{job_prefix};
    my $runjobs_template_path = $args{runjobs_template_path};
    my $runjobs_template_hash = $args{runjobs_template_hash};

    my $target = $args{target};
    my $input_serifxml_list = $args{input_serifxml_list};
    my $input_mappings_path = $args{input_mappings_path};
    my $target_and_score_table_dir = $args{target_and_score_table_dir};
    my $output_labeled_mappings_path = $args{output_labeled_mappings_path};
    my $output_serifxml_dir = $args{output_serifxml_dir};

    my $MEM_LIMIT = $self->{MEM_LIMIT};
    $runjobs_template_hash->{learnit_root} = $self->{LEARNIT_ROOT};
    $runjobs_template_hash->{SGE_VIRTUAL_FREE} = $MEM_LIMIT;
    $runjobs_template_hash->{BATCH_QUEUE} = $self->{LINUX_CPU_QUEUE};

    my $target_and_score_table_jobid = runjobs4::runjobs(
        $dependant_job_ids,
        $job_prefix . "_TargetAndScoreTableLabeler",
        $runjobs_template_hash,
        [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" sh $self->{LEARNIT_ROOT}/neolearnit/target/appassembler/bin/TargetAndScoreTableLabeler", $runjobs_template_path, "$target_and_score_table_dir $input_mappings_path $output_labeled_mappings_path" ]
    );

    my $serialization_to_serifxml_jobid = runjobs4::runjobs(
        [ $target_and_score_table_jobid ],
        $job_prefix . "_SerifXMLSerializer",
        $runjobs_template_hash,
        [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" sh $self->{LEARNIT_ROOT}/neolearnit/target/appassembler/bin/SerifXMLSerializer", $runjobs_template_path, "$input_serifxml_list $output_labeled_mappings_path $target $output_serifxml_dir" ]
    );

    return([ $serialization_to_serifxml_jobid ], $output_serifxml_dir);
}

sub InstanceExtractor {
    my ($self, %args) = @_;
    my $dependant_job_ids = $args{dependant_job_ids};
    my $job_prefix = $args{job_prefix};
    my $target = $args{target};
    my $runjobs_template_path = $args{runjobs_template_path};
    my $runjobs_template_hash = $args{runjobs_template_hash};
    my $output_mappings_path = $args{output_mappings_path};
    my $input_serifxml_list = $args{input_serifxml_list};

    my $MEM_LIMIT = $self->{MEM_LIMIT};
    $runjobs_template_hash->{learnit_root} = $self->{LEARNIT_ROOT};
    $runjobs_template_hash->{SGE_VIRTUAL_FREE} = $MEM_LIMIT;
    $runjobs_template_hash->{BATCH_QUEUE} = $self->{LINUX_CPU_QUEUE};

    my $jobid = runjobs4::runjobs(
        $dependant_job_ids,
        $job_prefix . "_InstanceExtractor",
        $runjobs_template_hash,
        [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" sh $self->{LEARNIT_ROOT}/neolearnit/target/appassembler/bin/InstanceExtractor", $runjobs_template_path, "$target $input_serifxml_list $output_mappings_path" ]
    );

    return([ $jobid ], $output_mappings_path);
}

sub LearnItDecoding {
    my ($self, %args) = @_;
    my $dependant_job_ids = $args{dependant_job_ids};
    my $job_prefix = $args{job_prefix};
    my $runjobs_template_path = $args{runjobs_template_path};
    my $runjobs_template_hash = $args{runjobs_template_hash};

    my $input_serifxml_list = $args{input_serifxml_list};
    my $num_of_jobs = $args{num_of_jobs};
    my $stage_processing_dir = $args{stage_processing_dir};
    my $should_output_incomplete_examples = $args{should_output_incomplete_examples};
    my $output_doc_list = "$stage_processing_dir/learnit_decoder.list";

    Utils::make_output_dir($stage_processing_dir, "$job_prefix/mkdir_output", $dependant_job_ids);

    my ($batch_file_dir, $mkdir_batch_dir_jobid) = Utils::make_output_dir("$stage_processing_dir/batch_files", "$job_prefix/mkdir_batch", $dependant_job_ids);

    $runjobs_template_hash->{source_lists} = $batch_file_dir;
    $runjobs_template_hash->{learnit_root} = $self->{LEARNIT_ROOT};
    $runjobs_template_hash->{corpus_name} = "decoding";
    $runjobs_template_hash->{BATCH_QUEUE} = $self->{BATCH_QUEUE};
    my $MEM_LIMIT = $self->{MEM_LIMIT};
    $runjobs_template_hash->{SGE_VIRTUAL_FREE} = $MEM_LIMIT;

    my (
        $create_filelist_jobid, @file_list_at_disk
    ) = Utils::split_file_list_with_num_of_batches(
        PYTHON                  => $self->{PYTHON3},
        CREATE_FILELIST_PY_PATH => $self->{CREATE_FILELIST_PY_PATH},
        dependant_job_ids       => $mkdir_batch_dir_jobid,
        job_prefix              => $job_prefix . "/create_batch",
        num_of_batches          => $num_of_jobs,
        list_file_path          => $input_serifxml_list,
        output_file_prefix      => $batch_file_dir . "/batch_",
        suffix                  => ".list",
    );

    my @learnit_decoding_split_jobs = ();
    for (my $batch = 0; $batch < $num_of_jobs; $batch++) {
        my $batch_file = "$batch_file_dir/batch_$batch.list";
        my $batch_job_prefix = $job_prefix . "/$batch";
        my ($batch_output_dir, $mkdir_batch_jobid) = Utils::make_output_dir("$stage_processing_dir/$batch", "$batch_job_prefix/mkdir_batch", $create_filelist_jobid);
        my $learnit_decoder_jobid = runjobs4::runjobs(
            $mkdir_batch_jobid,
            $batch_job_prefix . "/learnit_decoder",
            $runjobs_template_hash,
            [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" java -cp $self->{LEARNIT_JAR} com.bbn.akbc.neolearnit.exec.LearnItDecoder", $runjobs_template_path ,"$batch_file $batch_output_dir $should_output_incomplete_examples" ]
        );
        push(@learnit_decoding_split_jobs, $learnit_decoder_jobid);
    }

    my $list_collector_job_id =
        runjobs4::runjobs(
            \@learnit_decoding_split_jobs,
            "$job_prefix/" .
                "list_collector",
            {
                BATCH_QUEUE => $self->{LINUX_CPU_QUEUE},
                SCRIPT      => 1
            },
            [
                "find $stage_processing_dir -name \"*.xml\" -exec readlink -f {} \\;  " .
                    " | sort -u > $output_doc_list"
            ]
        );
    return([ $list_collector_job_id ], $output_doc_list);
}

sub LearnItDecodingDemo{
    my ($self, %args) = @_;
    my $dependant_job_ids = $args{dependant_job_ids};
    my $job_prefix = $args{job_prefix};
    my $runjobs_template_path = $args{runjobs_template_path};
    my $runjobs_template_hash = $args{runjobs_template_hash};

    my $input_serifxml_list = $args{input_serifxml_list};
    my $num_of_jobs = $args{num_of_jobs};
    my $stage_processing_dir = $args{stage_processing_dir};
    my $output_doc_list = "$stage_processing_dir/learnit_decoder.list";

    Utils::make_output_dir($stage_processing_dir, "$job_prefix/mkdir_output", $dependant_job_ids);

    my ($batch_file_dir, $mkdir_batch_dir_jobid) = Utils::make_output_dir("$stage_processing_dir/batch_files", "$job_prefix/mkdir_batch", $dependant_job_ids);

    $runjobs_template_hash->{source_lists} = $batch_file_dir;
    $runjobs_template_hash->{learnit_root} = $self->{LEARNIT_ROOT};
    $runjobs_template_hash->{BATCH_QUEUE} = $self->{BATCH_QUEUE};
    my $MEM_LIMIT = $self->{MEM_LIMIT};
    $runjobs_template_hash->{SGE_VIRTUAL_FREE} = $MEM_LIMIT;

    my (
        $create_filelist_jobid, @file_list_at_disk
    ) = Utils::split_file_list_with_num_of_batches(
        PYTHON                  => $self->{PYTHON3},
        CREATE_FILELIST_PY_PATH => $self->{CREATE_FILELIST_PY_PATH},
        dependant_job_ids       => $mkdir_batch_dir_jobid,
        job_prefix              => $job_prefix . "/create_batch",
        num_of_batches          => $num_of_jobs,
        list_file_path          => $input_serifxml_list,
        output_file_prefix      => $batch_file_dir . "/batch_",
        suffix                  => ".list",
    );

    my @learnit_decoding_split_jobs = ();
    for (my $batch = 0; $batch < $num_of_jobs; $batch++) {
        my $batch_file = "$batch_file_dir/batch_$batch.list";
        my $batch_job_prefix = $job_prefix . "/$batch";
        my ($batch_output_dir, $mkdir_batch_jobid) = Utils::make_output_dir("$stage_processing_dir/$batch", "$batch_job_prefix/mkdir_batch", $create_filelist_jobid);
        my $learnit_decoder_jobid = runjobs4::runjobs(
            $mkdir_batch_jobid,
            $batch_job_prefix . "/learnit_decoder",
            $runjobs_template_hash,
            [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" java -cp $self->{LEARNIT_JAR} com.bbn.akbc.neolearnit.exec.GenerateTrainingDataForNN $runjobs_template_path $batch_file $batch_output_dir"]
        );
        push(@learnit_decoding_split_jobs, $learnit_decoder_jobid);
    }

    my $list_collector_job_id =
        runjobs4::runjobs(
            \@learnit_decoding_split_jobs,
            "$job_prefix/" .
                "list_collector",
            {
                BATCH_QUEUE => $self->{LINUX_CPU_QUEUE},
                SCRIPT      => 1
            },
            [
                "find $stage_processing_dir -name \"*.xml\" -exec readlink -f {} \\;  " .
                    " | sort -u > $output_doc_list"
            ]
        );


    return([ $list_collector_job_id ], $output_doc_list);


}

sub EERNASampler {
    my ($self, %args) = @_;
    my $dependant_job_ids = $args{dependant_job_ids};
    my $job_prefix = $args{job_prefix};
    my $runjobs_template_path = $args{runjobs_template_path};
    my $runjobs_template_hash = $args{runjobs_template_hash};

    my $input_serifxml_list = $args{input_serifxml_list};
    my $num_of_jobs = $args{num_of_jobs};
    my $stage_processing_dir = $args{stage_processing_dir};
    my $max_instances_per_seed = $args{max_instances_per_seed};
    my $output_doc_list = "$stage_processing_dir/eer_na_samples.list";

    Utils::make_output_dir($stage_processing_dir, "$job_prefix/mkdir_output", $dependant_job_ids);

    my ($batch_file_dir, $mkdir_batch_dir_jobid) = Utils::make_output_dir("$stage_processing_dir/batch_files", "$job_prefix/mkdir_batch", $dependant_job_ids);

    $runjobs_template_hash->{source_lists} = $batch_file_dir;
    $runjobs_template_hash->{learnit_root} = $self->{LEARNIT_ROOT};
    $runjobs_template_hash->{corpus_name} = "decoding";
    $runjobs_template_hash->{BATCH_QUEUE} = $self->{BATCH_QUEUE};
    my $MEM_LIMIT = $self->{MEM_LIMIT};
    $runjobs_template_hash->{SGE_VIRTUAL_FREE} = $MEM_LIMIT;

    my (
        $create_filelist_jobid, @file_list_at_disk
    ) = Utils::split_file_list_with_num_of_batches(
        PYTHON                  => $self->{PYTHON3},
        CREATE_FILELIST_PY_PATH => $self->{CREATE_FILELIST_PY_PATH},
        dependant_job_ids       => $mkdir_batch_dir_jobid,
        job_prefix              => $job_prefix . "/create_batch",
        num_of_batches          => $num_of_jobs,
        list_file_path          => $input_serifxml_list,
        output_file_prefix      => $batch_file_dir . "/batch_",
        suffix                  => ".list",
    );

    my @learnit_decoding_split_jobs = ();
    for (my $batch = 0; $batch < $num_of_jobs; $batch++) {
        my $batch_file = "$batch_file_dir/batch_$batch.list";
        my $batch_job_prefix = $job_prefix . "/$batch";
        my ($batch_output_dir, $mkdir_batch_jobid) = Utils::make_output_dir("$stage_processing_dir/$batch", "$batch_job_prefix/mkdir_batch", $create_filelist_jobid);
        my $learnit_decoder_jobid = runjobs4::runjobs(
            $mkdir_batch_jobid,
            $batch_job_prefix . "/eer_na_sampler",
            $runjobs_template_hash,
            [ "env JAVA_OPTS=\"-Xmx$MEM_LIMIT\" java -cp $self->{LEARNIT_JAR} com.bbn.akbc.neolearnit.exec.EERNASampler", $runjobs_template_path ,"$batch_file $batch_output_dir $max_instances_per_seed" ]
        );
        push(@learnit_decoding_split_jobs, $learnit_decoder_jobid);
    }

    my $list_collector_job_id =
        runjobs4::runjobs(
            \@learnit_decoding_split_jobs,
            "$job_prefix/" .
                "list_collector",
            {
                BATCH_QUEUE => $self->{LINUX_CPU_QUEUE},
                SCRIPT      => 1
            },
            [
                "find $stage_processing_dir -name \"*.json\" -exec readlink -f {} \\;  " .
                    " | sort -u > $output_doc_list"
            ]
        );
    return([ $list_collector_job_id ], $output_doc_list);
}


1;