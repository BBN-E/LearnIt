#!/bin/env perl
#
# SERIF Run ExperimentBuild Experiment
# Copyright (C) 2012 BBN Technologies
use strict;
use warnings;

# Standard libraries:
use Getopt::Long;
use File::Basename;

# Runjobs libraries:
use FindBin qw($Bin);
use lib "$Bin/../bin/Byblos/Cube2/install-optimize/perl_lib";
use lib "$Bin/../bin/Byblos/ExpModules/Distillation";
use runjobs4;
use File::PathConvert;
use ParameterFiles;
use Getopt::Long;

sub usage() {
	print "USAGE:\n\n";
	print "perl generate_mappings_and_similarities.pl --target <target-name> --params <abs-path-to-params-file> --mode <mappings-only|similarities-only|both> [-sge]\n\n";
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
    "target=s"  => \$main::TARGET,
    "params=s"    => \$main::PARAMS,
    "mode=s"     => \$main::MODE
);    #optional for yield view/seed initialization
Getopt::Long::Configure("no_pass_through");

defined( our $PARAMS ) && defined( our $TARGET ) && defined(our $MODE) || usage();

if ($MODE ne 'mappings-only' && $MODE ne 'similarities-only' && $MODE ne 'both') {
 print "\nInvalid value for 'mode'\n";
 usage();
}

print "Using param file $PARAMS\n";
our $params = ParameterFiles::load_param_file($PARAMS);

print "target is: $TARGET\n";
print "mode is: $MODE\n";
if ($MODE eq 'mappings-only') {
  print "\tThis sequence will only extract mappings!\n";
} elsif ($MODE eq 'similarities-only') {
  print "\tThis sequence will compute seed/pattern similarity matrices using existing mappings!\n";
} else{
  print "\tThis sequence will extract mappings and then compute seed/pattern similarity matrices!\n";
}

#WINDOW QUEUE
# my $path_convert = \&File::PathConvert::unix2win;
my $path_convert = (sub {shift;});
#our $QUEUE = $params->{"linux_queue"};
#our $QUEUE_PRIORITY = $params->{"queue_priority"};

our $QUEUE = "gale-sl6";
our $QUEUE_PRIORITY = 5;

####### START RUNJOBS ###########
my ($expt_dir, $exp) = startjobs();
max_jobs(400);  # Max number of jobs to run at once

my $JAVA_PREFIX = $params->{"java_command"};
my $MAVEN_BIN = "/opt/apache-maven-3.0.4/bin/mvn";

my $CORPUS_DIR = $params->{"corpus_dir"};

my $expt_name = $params->{"corpus_name"};
my $job_prefix = "$expt_name/$TARGET";

run_on_queue_now_local("make_expt_dirs-$TARGET", "mkdir -p $expt_dir/expts/$job_prefix");

#extract instances to slots and patterns mappings
if ($MODE ne 'similarities-only'){
extract_mappings($CORPUS_DIR, 
					$params->{"mappings_dir"} . "/$TARGET",
					$params->{"mappings_lists_dir"} . "/$TARGET-master.txt",
					$params->{"mappings_lists_dir"} . "/$TARGET");
}

if ($MODE eq 'mappings-only'){
  endjobs();
  exit(0);
}
# extracts seeds (slot-pairs) and patterns from the mappings

extract_seeds_and_patterns($params->{"mappings_lists_dir"} . "/$TARGET", 
		$params->{"seeds_dir"} . "/$TARGET",
		$params->{"seeds_lists_dir"} . "/$TARGET",
		$params->{"patterns_dir"} . "/$TARGET",
		$params->{"patterns_lists_dir"} . "/$TARGET", 
		$TARGET);

my $seeds_lists_dir = $params->{"seeds_lists_dir"} . "/$TARGET";
my $seed_vectors_dir = $params->{"seed_vectors_dir"}."/$TARGET"; 
my $patterns_lists_dir = $params->{"patterns_lists_dir"} . "/$TARGET";
my $pattern_vectors_dir = $params->{"pattern_vectors_dir"}."/$TARGET"; 

# compute seed vectors (using word embeddings) and dump them in a single pickle file
my $seed_vectors_file = compute_seed_vectors($seeds_lists_dir,$seed_vectors_dir,$params->{"event_words_phrase_embeddings_file"});

# extract pattern vectors (from pre-trained pattern embeddings) and dump them in a single pickle file
my $pattern_vectors_file = extract_pattern_vectors($patterns_lists_dir,$pattern_vectors_dir,$params->{"patterns_phrase_embeddings_file"});

# Compute similarity between seed pairs and dump the similarity matrices as .tsv.gz files.
# Write a similarity-list file also to be used by downstream processes/sequences.
# Make use of $seeds_lists_dir to ensure one to one mapping between input seed files and their similarity vectors
my $seed_similarity_dir = $params->{"seed_similarity_dir"}."/$TARGET";
my $seed_similarity_lists_dir = $params->{"seed_similarity_lists_dir"}."/$TARGET";

compute_vector_similarity($seed_vectors_file,$seeds_lists_dir,$seed_similarity_dir,$seed_similarity_lists_dir,$params->{"seed_similarity_threshold"},"seeds");

# Compute similarity between pattern pairs and dump the similarity matrices as .tsv.gz files.
# Write a similarity-list file also to be used by downstream processes/sequences.
# Make use of $patterns_lists_dir to ensure one to one mapping between input pattern files and their similarity vectors
my $pattern_similarity_dir = $params->{"pattern_similarity_dir"}."/$TARGET";
my $pattern_similarity_lists_dir = $params->{"pattern_similarity_lists_dir"}."/$TARGET";

compute_vector_similarity($pattern_vectors_file,$patterns_lists_dir,$pattern_similarity_dir,$pattern_similarity_lists_dir,$params->{"pattern_similarity_threshold"},"patterns");

endjobs();

our $learnit_root = $params->{"learnit_root"};
sub extract_mappings {
	(
		my $source_list_dir,
		my $source_mappings_dir,
		my $source_mappings_master_list,
		my $source_mapping_lists
	) = @_;

	run_on_queue_now_local("prepare/make-mappings-dirs",
		"mkdir -p $source_mappings_dir; mkdir -p $source_mapping_lists");
	
	my @batch_source_lists = glob "$source_list_dir/*";
	foreach my $batch_list (@batch_source_lists) {
		my ($batch_name,$batch_dir) = fileparse($batch_list);

		my $output_file = "$source_mappings_dir/$batch_name.sjson";
		
		run_on_queue("prepare/extract-${batch_name}",
				"24G",
				 make_java_command("24G", $params->{"learnit_root"}."/neolearnit/target/appassembler/bin/", "InstanceExtractor","$PARAMS $TARGET $batch_list $output_file"));
	}
	
	dojobs();
	
	my $num_list_batches = $params->{"mappings_list_batches"};
	run_on_queue_now_local("prepare/make-mappings-sublists-balanced",
        "/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/split_batches_by_size.py $source_mappings_dir $num_list_batches ${source_mapping_lists}");
	
	return $source_mapping_lists;
}

sub extract_seeds_and_patterns {
	(
		my $source_mapping_lists,
		my $seeds_dir,
		my $seeds_lists,
		my $patterns_dir,
		my $patterns_lists,
		my $TARGET
	) = @_;

	run_on_queue_now_local("prepare/make-seed-and-pattern-dirs",
		"mkdir -p $seeds_dir; mkdir -p $seeds_lists; mkdir -p $patterns_dir; mkdir -p $patterns_lists");
	
	my @batch_mapping_lists = glob "$source_mapping_lists/*";
	foreach my $batch_list (@batch_mapping_lists) {
		my ($batch_name,$batch_dir) = fileparse($batch_list);

		my $seed_output_file = "$seeds_dir/$batch_name.tsv";
		my $pattern_output_file = "$patterns_dir/$batch_name.tsv";
		
		run_on_queue("extract-seeds-and-patterns-${batch_name}",
				"24G",
				 make_java_command("24G", $params->{"learnit_root"}."/neolearnit/target/appassembler/bin/", "EventSeedAndPatternExtractor","$PARAMS $batch_list $seed_output_file $pattern_output_file $TARGET"));
	}
	
	dojobs();
	
	my $num_list_batches = $params->{"mappings_list_batches"};
	run_on_queue_now_local("prepare/make-seed-sublists-balanced",
        "/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/split_batches_by_num_lines.py $seeds_dir $num_list_batches ${seeds_lists}");

	$num_list_batches = $params->{"mappings_list_batches"};
	run_on_queue_now_local("prepare/make-pattern-sublists-balanced",
        "/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/split_batches_by_num_lines.py $patterns_dir $num_list_batches ${patterns_lists}");

	return ($seeds_lists,$patterns_lists);
}

sub compute_seed_vectors {
	(
		my $seed_lists,
		my $seed_vector_dir,
                my $embeddings_file
	) = @_;

	run_on_queue_now_local("prepare/make-seed-vector-dirs",
		"mkdir -p $seed_vector_dir; mkdir -p $seed_vector_dir/temp");
	
        my $combined_seed_list = $seed_vector_dir."/temp/all_seeds_files.list";

	run_on_queue_now_local("prepare/create-combined-seed-list",
				"find $seed_lists -type f | xargs cat > $combined_seed_list");

	my $seed_vector_output_file = $seed_vector_dir."/seed_vectors.p";

	run_on_queue_now_local("prepare/compute-seed-vectors",
			"/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/compute_seed_vectors.py $combined_seed_list $embeddings_file $seed_vector_output_file");

        run_on_queue_now_local("prepare/delete-combined-seed-list",
			"rm -fr $seed_vector_dir/temp");

	dojobs();

	return $seed_vector_output_file;
}

sub extract_pattern_vectors {
	(
		my $pattern_lists,
		my $pattern_vector_dir,
                my $embeddings_file
	) = @_;

	run_on_queue_now_local("prepare/make-pattern-vector-dirs",
		"mkdir -p $pattern_vector_dir; mkdir -p $pattern_vector_dir/temp");
	
        my $combined_pattern_list = $pattern_vector_dir."/temp/all_patterns_files.list";

	run_on_queue_now_local("prepare/create-combined-pattern-list",
				"find $pattern_lists -type f | xargs cat > $combined_pattern_list");

	my $pattern_vector_output_file = $pattern_vector_dir."/pattern_vectors.p";

	run_on_queue_now_local("prepare/extract-pattern-vectors",
			"/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/extract_pattern_vectors.py $combined_pattern_list $embeddings_file $pattern_vector_output_file");

        run_on_queue_now_local("prepare/delete-combined-pattern-list",
			"rm -fr $pattern_vector_dir/temp");

	dojobs();

	return $pattern_vector_output_file;
}

sub compute_vector_similarity {
	(
		my $embeddings_file,
                my $original_artifact_lists_dir,
		my $output_dir,
                my $output_lists_dir,
		my $similarity_threshold,
                my $artifact_type
	) = @_;

	run_on_queue_now_local("prepare/make-similarity-dirs-$artifact_type",
		"mkdir -p $output_dir; mkdir -p $output_lists_dir");

	run_on_queue_now_local("prepare/compute-$artifact_type-similarity-$similarity_threshold",
			"/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/compute_vector_similarity.py $embeddings_file $original_artifact_lists_dir $output_dir $output_lists_dir $similarity_threshold $artifact_type");

	dojobs();
	
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
         my $command
        ) = @_;
        return runjobs([$prev_job],"$job_prefix/$job_name", {BATCH_QUEUE => $QUEUE, QUEUE_PRIO => $QUEUE_PRIORITY, SGE_VIRTUAL_FREE => "24G"}, $command); 
}

sub run_on_queue {
        (
         my $job_name,
         my $sge_vm_free,
         my $command
        ) = @_;

        runjobs([],"$job_prefix/$job_name", {BATCH_QUEUE => $QUEUE, QUEUE_PRIO => $QUEUE_PRIORITY, SGE_VIRTUAL_FREE => "$sge_vm_free"}, $command);
}


sub run_on_queue_now_local {
        (
         my $job_name,
         my $command
        ) = @_;

        runjobs([],"$job_prefix/$job_name", {SCRIPT => 1, SGE_VIRTUAL_FREE => "24G"}, $command);
        dojobs();
}

endjobs();

1;

