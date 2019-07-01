#!/bin/env perl
#
# SERIF Run ExperimentBuild Experiment
# Copyright (C) 2012 BBN Technologies
# use strict;
# use warnings;
#
# # Standard libraries:
# use Getopt::Long;
# use File::Basename;
#
# # Runjobs libraries:
# use FindBin qw($Bin);
# use lib "$Bin/../bin/Byblos/Cube2/install-optimize/perl_lib";
# use lib "$Bin/../bin/Byblos/ExpModules/Distillation";
# use runjobs4;
# use File::PathConvert;
# use ParameterFiles;
# use Getopt::Long;
use strict;
use warnings;

use lib "/d4m/ears/releases/Cube2/R2016_07_21/install-optimize-x86_64/perl_lib";
use runjobs4;

use Cwd 'abs_path';

use File::Basename;
use File::Path;
use ParameterFiles;
use Getopt::Long;

package main;

my $MAPPINGS_ONLY = 'mappings-only';
my $SIMILARITIES_ONLY = 'similarities-only';
my $MAPPINGS_AND_SIMILARITIES = 'mappings-and-similarities';
my $MAPPINGS_AND_SCORING = 'mappings-and-scoring';
my $SCORING_ONLY = 'scoring-only';
sub usage() {
	print "USAGE:\n\n";
	print "perl run_learnit.pl --target <target-name> --params <abs-path-to-params-file> --mode <$MAPPINGS_ONLY|$SIMILARITIES_ONLY|$MAPPINGS_AND_SIMILARITIES|$MAPPINGS_AND_SCORING|$SCORING_ONLY> --epoch <num> [-sge]\n\n";
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
    "mode=s"     => \$main::MODE,
    "epoch=s"    => \$main::EPOCH
);    #optional for yield view/seed initialization
Getopt::Long::Configure("no_pass_through");

defined( our $PARAMS ) && defined( our $TARGET ) && defined(our $MODE) && defined(our $EPOCH) || usage();

if ($MODE ne $MAPPINGS_ONLY && $MODE ne $SIMILARITIES_ONLY && $MODE ne $MAPPINGS_AND_SIMILARITIES && $MODE ne $MAPPINGS_AND_SCORING && $MODE ne $SCORING_ONLY) {
 print "\nInvalid value for 'mode'\n";
 usage();
}


print "target is: $TARGET\n";
print "mode is: $MODE\n";
if ($MODE eq $MAPPINGS_ONLY) {
  print "\tThis sequence will only extract mappings!\n";
} elsif ($MODE eq $SIMILARITIES_ONLY) {
  print "\tThis sequence will compute seed/pattern similarity matrices using given mappings!\n";
} elsif ($MODE eq $MAPPINGS_AND_SIMILARITIES){
  print "\tThis sequence will extract mappings and then compute seed/pattern similarity matrices!\n";
} elsif ($MODE eq $MAPPINGS_AND_SCORING){
  print "\tThis sequence will extract mappings and score them against given extractors!\n";
} elsif ($MODE eq $SCORING_ONLY){
  print "\tThis sequence will score given mappings against given extractors!\n";
}

print "Using param file $PARAMS\n";
our $params = ParameterFiles::load_param_file($PARAMS);

#WINDOW QUEUE
# my $path_convert = \&File::PathConvert::unix2win;
my $path_convert = (sub {shift;});
our $QUEUE = $params->{"linux_queue"};
our $QUEUE_PRIORITY = $params->{"queue_priority"};

####### START RUNJOBS ###########
my ($expt_dir, $exp) = startjobs();
max_jobs(400);  # Max number of jobs to run at once

my $JAVA_PREFIX = $params->{"java_command"};
my $MAVEN_BIN = "/opt/apache-maven-3.0.4/bin/mvn";

my $CORPUS_DIR = $params->{"corpus_dir"};

my $expt_name = $params->{"corpus_name"};
my $job_prefix = "$expt_name-$EPOCH/$TARGET";

run_on_queue_now_local("make_expt_dirs-$TARGET", "mkdir -p $expt_dir/expts/$job_prefix");

our $learnit_root = $params->{"learnit_root"};

my $seed_frequency = $params->{"min_seed_frequency_per_batch"};
my $pattern_frequency = $params->{"min_pattern_frequency_per_batch"};
my $frequency_combination = $seed_frequency.'_'.$pattern_frequency;
my $mappings_dir = $params->{"mappings_dir"} . "/$TARGET-$EPOCH/freq_$frequency_combination";
my $mappings_lists_dir = $params->{"mappings_lists_dir"} . "/$TARGET-$EPOCH/freq_$frequency_combination";

#extract instances to slots and patterns mappings
if ($MODE eq $MAPPINGS_ONLY || $MODE eq $MAPPINGS_AND_SIMILARITIES || $MODE eq $MAPPINGS_AND_SCORING){
extract_mappings($CORPUS_DIR, $mappings_dir, $mappings_lists_dir);
create_aggregate_mappings_file($mappings_lists_dir,$mappings_dir."/mappings.master.sjson");
}

if ($MODE eq $MAPPINGS_ONLY){
  endjobs();
  exit(0);
}

if ($MODE eq $MAPPINGS_AND_SIMILARITIES || $MODE eq $SIMILARITIES_ONLY){
# extracts seeds (slot-pairs) and patterns from the mappings
my $seeds_dir = $params->{"seeds_dir"} . "/$TARGET-$EPOCH/min_freq_.$seed_frequency";
my $seeds_lists_dir = $params->{"seeds_lists_dir"} . "/$TARGET-$EPOCH/min_freq_$seed_frequency";
my $seed_vectors_dir = $params->{"seed_vectors_dir"}."/$TARGET-$EPOCH/min_freq_$seed_frequency"; 
my $patterns_dir = $params->{"patterns_dir"} . "/$TARGET-$EPOCH/min_freq_$pattern_frequency";
my $patterns_lists_dir = $params->{"patterns_lists_dir"} . "/$TARGET-$EPOCH/min_freq_$pattern_frequency";
my $pattern_vectors_dir = $params->{"pattern_vectors_dir"}."/$TARGET-$EPOCH/min_freq_$pattern_frequency"; 
extract_seeds_and_patterns($mappings_lists_dir, $seeds_dir, $seeds_lists_dir, $patterns_dir, $patterns_lists_dir);

# compute seed vectors (using word embeddings) and dump them in a single pickle file
my $seed_vectors_file = compute_seed_vectors($seeds_lists_dir,$seed_vectors_dir,$params->{"embeddings_file_for_seeds"});

my $pattern_vectors_file;
my $pattern_embeddings_file_name = fileparse($params->{"embeddings_file_for_patterns"});
my $use_word_embeddings_for_patterns="false";
if (index(lc $pattern_embeddings_file_name,"glove")!=-1){
    $use_word_embeddings_for_patterns = "true";
}
if ($use_word_embeddings_for_patterns eq "true"){
	#compute pattern vectors using pattern-tokens and word embeddings
	$pattern_vectors_file = compute_pattern_vectors($patterns_lists_dir,$pattern_vectors_dir,$params->{"embeddings_file_for_patterns"});
}else{
	# extract pattern vectors (from pre-trained pattern embeddings) and dump them in a single pickle file
	$pattern_vectors_file = extract_pattern_vectors($patterns_lists_dir,$pattern_vectors_dir,$params->{"embeddings_file_for_patterns"});
}
# Compute similarity between seed pairs and dump the similarity matrices as .tsv.gz files.
# Write a similarity-list file also to be used by downstream processes/sequences.
# Make use of $seeds_lists_dir to ensure one to one mapping between input seed files and their similarity vectors
my $seed_thresh = $params->{"seed_similarity_threshold"} ;
my $seed_similarity_dir = $params->{"seed_similarity_dir"}."/$TARGET-$EPOCH/min_freq_$seed_frequency"."_$seed_thresh";
my $seed_similarity_lists_dir = $params->{"seed_similarity_lists_dir"}."/$TARGET-$EPOCH/min_freq_$seed_frequency"."_$seed_thresh";
compute_vector_similarity($seed_vectors_file,$seeds_lists_dir,$seed_similarity_dir,$seed_similarity_lists_dir,$seed_thresh,"seeds");

# Compute similarity between pattern pairs and dump the similarity matrices as .tsv.gz files.
# Write a similarity-list file also to be used by downstream processes/sequences.
# Make use of $patterns_lists_dir to ensure one to one mapping between input pattern files and their similarity vectors
my $pattern_thresh = $params->{"pattern_similarity_threshold"};
my $pattern_similarity_dir = $params->{"pattern_similarity_dir"}."/$TARGET-$EPOCH/min_freq_$pattern_frequency"."_$pattern_thresh";
my $pattern_similarity_lists_dir = $params->{"pattern_similarity_lists_dir"}."/$TARGET-$EPOCH/min_freq_$pattern_frequency"."_$pattern_thresh";
compute_vector_similarity($pattern_vectors_file,$patterns_lists_dir,$pattern_similarity_dir,$pattern_similarity_lists_dir,$pattern_thresh,"patterns");

sub extract_seeds_and_patterns {
	(
		my $source_mapping_lists,
		my $seeds_dir,
		my $seeds_lists,
		my $patterns_dir,
		my $patterns_lists
	) = @_;

	run_on_queue_now_local("prepare/make-seed-and-pattern-dirs-${frequency_combination}",
		"mkdir -p $seeds_dir; mkdir -p $seeds_lists; mkdir -p $patterns_dir; mkdir -p $patterns_lists");
	
	my @batch_mapping_lists = glob "$source_mapping_lists/*";
        if (!@batch_mapping_lists){
		print "Found empty directory for source mapping lists: ".$source_mapping_lists."! Cannot locate mappings.\n";
		exit(1);
	}	
	foreach my $batch_list (@batch_mapping_lists) {
		my ($batch_name,$batch_dir) = fileparse($batch_list);

		my $seed_output_file = "$seeds_dir/$batch_name.tsv";
		my $pattern_output_file = "$patterns_dir/$batch_name.tsv";
		
		run_on_queue("extract-seeds-and-patterns-${batch_name}-${frequency_combination}",
				"75G",
				 make_java_command("75G", $params->{"learnit_root"}."/neolearnit/target/appassembler/bin/", "EventSeedAndPatternExtractor","$PARAMS $batch_list $seed_output_file $pattern_output_file $TARGET"));
	}
	
	dojobs();
	
	my $num_list_batches = $params->{"mappings_list_batches"};
	run_on_queue_now_local("prepare/make-seed-sublists-balanced-${frequency_combination}",
        "/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/split_batches_by_num_lines.py $seeds_dir $num_list_batches ${seeds_lists}");

	$num_list_batches = $params->{"mappings_list_batches"};
	run_on_queue_now_local("prepare/make-pattern-sublists-balanced-${frequency_combination}",
        "/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/split_batches_by_num_lines.py $patterns_dir $num_list_batches ${patterns_lists}");

	return ($seeds_lists,$patterns_lists);
}

sub compute_seed_vectors {
	(
		my $seed_lists,
		my $seed_vector_dir,
                my $embeddings_file
	) = @_;

	run_on_queue_now_local("prepare/make-seed-vector-dirs-${seed_frequency}",
		"mkdir -p $seed_vector_dir; mkdir -p $seed_vector_dir/combined_list");
	
        my $combined_seed_list = $seed_vector_dir."/combined_list/all_seeds_files.list";

	run_on_queue_now_local("prepare/create-combined-seed-list-${seed_frequency}",
				"find $seed_lists -type f | xargs cat > $combined_seed_list");

	my $seed_vector_output_file = $seed_vector_dir."/seed_vectors.p";

	run_on_queue_now_local("compute-seed-vectors-${seed_frequency}",
			"/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/compute_seed_vectors.py $combined_seed_list $embeddings_file $seed_vector_output_file");

        #run_on_queue_now_local("prepare/delete-combined-seed-list",
	#		"rm -fr $seed_vector_dir/temp");

	dojobs();

	return $seed_vector_output_file;
}

sub compute_pattern_vectors {
	(
		my $pattern_lists,
		my $pattern_vector_dir,
                my $embeddings_file
	) = @_;

	run_on_queue_now_local("prepare/make-pattern-vector-dirs-${pattern_frequency}",
		"mkdir -p $pattern_vector_dir; mkdir -p $pattern_vector_dir/combined_list");
	
        my $combined_pattern_list = $pattern_vector_dir."/combined_list/all_patterns_files.list";

	run_on_queue_now_local("prepare/create-combined-pattern-list-${pattern_frequency}",
				"find $pattern_lists -type f | xargs cat > $combined_pattern_list");

	my $pattern_vector_output_file = $pattern_vector_dir."/pattern_vectors.p";

	run_on_queue_now_local("compute-pattern-vectors-${pattern_frequency}",
			"/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/compute_pattern_vectors.py $combined_pattern_list $embeddings_file $pattern_vector_output_file");

        #run_on_queue_now_local("prepare/delete-combined-pattern-list",
	#		"rm -fr $pattern_vector_dir/temp");

	dojobs();

	return $pattern_vector_output_file;
}

sub extract_pattern_vectors {
	(
		my $pattern_lists,
		my $pattern_vector_dir,
                my $embeddings_file
	) = @_;

	run_on_queue_now_local("prepare/make-pattern-vector-dirs-${pattern_frequency}",
		"mkdir -p $pattern_vector_dir; mkdir -p $pattern_vector_dir/combined_list");
	
        my $combined_pattern_list = $pattern_vector_dir."/combined_list/all_patterns_files.list";

	run_on_queue_now_local("prepare/create-combined-pattern-list-${pattern_frequency}",
				"find $pattern_lists -type f | xargs cat > $combined_pattern_list");

	my $pattern_vector_output_file = $pattern_vector_dir."/pattern_vectors.p";

	run_on_queue_now_local("extract-pattern-vectors-${pattern_frequency}",
			"/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/extract_pattern_vectors.py $combined_pattern_list $embeddings_file $pattern_vector_output_file");

       #run_on_queue_now_local("prepare/delete-combined-pattern-list",
			#"rm -fr $pattern_vector_dir/temp");

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

	run_on_queue_now_local("prepare/make-similarity-dirs-$artifact_type-$frequency_combination-$similarity_threshold",
		"mkdir -p $output_dir; mkdir -p $output_lists_dir");

	run_on_queue_now_local("compute-$artifact_type-similarity-$frequency_combination-$similarity_threshold",
			"/d4m/ears/developers/msrivast/material/msrivast_venv_py2/bin/python2.7 $expt_dir/scripts/compute_vector_similarity.py $embeddings_file $original_artifact_lists_dir $output_dir $output_lists_dir $similarity_threshold $artifact_type");

	dojobs();
	
}
}

if ($MODE eq $SIMILARITIES_ONLY||$MODE eq $MAPPINGS_AND_SIMILARITIES){
  endjobs();
  exit(0);
}

if ($MODE eq $MAPPINGS_AND_SCORING || $MODE eq $SCORING_ONLY){
score_mappings($mappings_lists_dir, 
		$params->{"extractors_list"},
                $params->{"scoring_output_dir"});
}

endjobs();

sub extract_mappings {
	(
		my $source_list_dir,
		my $source_mappings_dir,
		my $source_mapping_lists
	) = @_;

	run_on_queue_now_local("prepare/make-mappings-dirs-$frequency_combination",
		"mkdir -p $source_mappings_dir; mkdir -p $source_mapping_lists");
	
	my @batch_source_lists = glob "$source_list_dir/*";
        if (!@batch_source_lists){
		print "Found empty directory for source lists! Cannot extract mappings.\n";
		exit(1);
	}
	foreach my $batch_list (@batch_source_lists) {
		my ($batch_name,$batch_dir) = fileparse($batch_list);

		my $output_file = "$source_mappings_dir/$batch_name.sjson";

		my $extract_thread_job = run_on_queue("extract-${batch_name}-".$frequency_combination,
				"50G",
				 make_java_command("75G", $params->{"learnit_root"}."/neolearnit/target/appassembler/bin/", "InstanceExtractor","$PARAMS $TARGET $batch_list $output_file.before_filter"));
		my $filter_thread_job = run_on_queue_with_prev_job
			(
				"filter-extract-${batch_name}-".$frequency_combination,
				$extract_thread_job,
				make_java_command("75G", $params->{"learnit_root"}."/neolearnit/target/appassembler/bin/", "FilterMappings","$PARAMS normal $output_file.before_filter $output_file"));
	}
	
	dojobs();
	
	my $num_list_batches = $params->{"mappings_list_batches"};
	run_on_queue_now_local("prepare/make-mappings-sublists-balanced-".$frequency_combination,
        "/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/split_batches_by_size.py $source_mappings_dir $num_list_batches ${source_mapping_lists}");
	run_on_queue_now_local("prepare/filter-mappings-sublists-".$frequency_combination,
		"/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/source_mappings_list_filter.py ${source_mapping_lists}"
	);
	return $source_mapping_lists;
}

sub create_aggregate_mappings_file{
	(
		my $mappings_lists_dir,
		my $output_mappings_file
	) = @_;

	run_on_queue_now_local("create-aggregate-mappings-".$frequency_combination,
		make_java_command("150G", $params->{"learnit_root"}."/neolearnit/target/appassembler/bin/", "MergeMappings","$PARAMS $mappings_lists_dir $output_mappings_file.before_filter"));
	run_on_queue_now_local("filter-aggregate-mappings-".$frequency_combination,
		make_java_command("128G", $params->{"learnit_root"}."/neolearnit/target/appassembler/bin/", "FilterMappings","$PARAMS aggregated $output_mappings_file.before_filter $output_mappings_file"));
	
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

	run_on_queue_now_local("prepare/make-scoring-dirs",
		"mkdir -p $scoring_output_dir");
	
	my @batch_mappings_lists = glob "$mappings_lists_dir/*";
	foreach my $batch_list (@batch_mappings_lists) {
		my ($batch_name,$batch_dir) = fileparse($batch_list);
		my $output_json_file = "$scoring_output_dir/$batch_name.decoded.json";
		my $output_file = "$scoring_output_dir/$batch_name.decoder.output";
		
		run_on_queue("decode-${batch_name}",
				"75G",
				 "sh $expt_dir/scripts/run_EventEventRelationPatternDecoder.sh $PARAMS $batch_list $extractors_list $learnit_root $output_json_file $output_file");
	}
	
	dojobs();
	
	run_on_queue_now_local("aggregate_decoding_output",
        "/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/decoder_output_aggregator.py $scoring_output_dir $scoring_output_dir/final_event_event_relations.json");

	run_on_queue_now_local("score-mappings",
        "/opt/Python-2.7.8-x86_64/bin/python $expt_dir/scripts/score_extracted_event_event_relations.py $scoring_output_dir $scoring_output_dir/event_event_relations.decoder.score");
	
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
         my $command
        ) = @_;
        return runjobs([$prev_job],"$job_prefix/$job_name", {BATCH_QUEUE => $QUEUE, QUEUE_PRIO => $QUEUE_PRIORITY, SGE_VIRTUAL_FREE => "75G"}, $command); 
}

sub run_on_queue {
        (
         my $job_name,
         my $sge_vm_free,
         my $command
        ) = @_;

        return runjobs([],"$job_prefix/$job_name", {BATCH_QUEUE => $QUEUE, QUEUE_PRIO => $QUEUE_PRIORITY, SGE_VIRTUAL_FREE => "$sge_vm_free"}, $command);
}


sub run_on_queue_now_local {
        (
         my $job_name,
         my $command
        ) = @_;

        runjobs([],"$job_prefix/$job_name", {SCRIPT => 1, SGE_VIRTUAL_FREE => "75G"}, $command);
        dojobs();
}

endjobs();

1;

