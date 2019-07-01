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
	print "perl add_event_mentions.pl\n\n";
	exit(1);
}

# Package declaration:
package main;

if (!defined($ENV{SGE_ROOT})) {
    print "WARNING: SGE_ROOT not defined; using a default value!\n";
    $ENV{SGE_ROOT} = "/opt/sge-6.2u5";
}

#WINDOW QUEUE
# my $path_convert = \&File::PathConvert::unix2win;
my $path_convert = (sub {shift;});
our $QUEUE = "nongale-sl6";
# our $QUEUE = "afrl";
our $QUEUE_PRIORITY = 5;

my ($expt_dir, $exp) = startjobs();
max_jobs(10);  # Max number of jobs to run at once

my $MAVEN_BIN = "/opt/apache-maven-3.0.4/bin/mvn";

my $job_prefix = "causeex-m15-decoding.v3";
my $learnit_repo="/nfs/ld100/u10/bmin/repositories/learnit";
my $params="$learnit_repo/params/learnit/runs/causeex-m15.params";
my $min_freq_seeds="5";
my $MAX_INSTANCES_PER_SEED=1000000;
my $NEGATIVE_SAMPLING_RATIO=1.0;

my $initial_extractor_list="/nfs/ld100/u10/bmin/repositories/learnit.master/resources/learnit_extractors.list";
# my $initial_mappings_list="$learnit_repo/resources/mappings_for_nre_decode_causeex-m15.list";
my $initial_mappings_list="$learnit_repo/resources/mappings_for_nre_test.list";

$expt_dir="${expt_dir}/expts/$job_prefix";
system "mkdir $expt_dir";

my $dir_extractors="${expt_dir}/extractors";
my $dir_list_output_extractors="${expt_dir}/list_output_extractors";
my $dir_final_extractors="${expt_dir}/final_extractors";
my $dir_opennre_intermediate="${expt_dir}/opennre_intermediate";
my $dir_opennre_final="${expt_dir}/opennre_final";

system "mkdir $dir_extractors";
system "mkdir $dir_list_output_extractors";
system "mkdir $dir_final_extractors";
system "mkdir $dir_opennre_intermediate";
system "mkdir $dir_opennre_final";


open(my $fh, '<:encoding(UTF-8)', $initial_extractor_list) or die "Could not open file '$initial_extractor_list' $!";
while (my $extractor = <$fh>) {
        chomp $extractor;
        my ($extractor_name,$extractor_dir) = fileparse($extractor);

	my $relationType="NA";
	if (index($extractor_name, "Cause-Effect") != -1) {
		$relationType="Cause-Effect";
	} 
        if (index($extractor_name, "Before-After") != -1) {
                $relationType="Before-After";
        }
        if (index($extractor_name, "Catalyst-Effect") != -1) {
                $relationType="Catalyst-Effect";
        }
        if (index($extractor_name, "MitigatingFactor-Effect") != -1) {
                $relationType="MitigatingFactor-Effect";
        }
        if (index($extractor_name, "Precondition-Effect") != -1) {
                $relationType="Precondition-Effect";
        }
        if (index($extractor_name, "Preventative-Effect") != -1) {
                $relationType="Preventative-Effect";
        }

	# generate NRE data
        open(my $fh, '<:encoding(UTF-8)', $initial_mappings_list) or die "Could not open file '$initial_mappings_list' $!";
        while (my $mappings = <$fh>) {
                chomp $mappings;

                my ($mappings_name,$mappings_dir) = fileparse($mappings);

		# my $final_extractor = "${dir_final_extractors}/${extractor_name}.json";
                my $final_extractor = $extractor;

		my $output_file_prefix = "${dir_opennre_intermediate}/opennre-${extractor_name}-${mappings_name}";
                my $iteration = 1;

                run_on_queue("${job_prefix}/GenerateTrainingDataFromSeedsForOpenNRE-${extractor_name}-${mappings_name}",
                                "4G",
                                make_java_command("4G", "$learnit_repo/neolearnit/target/appassembler/bin/", "GenerateTrainingDataFromSeedsForOpenNRE", "$params $final_extractor $mappings $output_file_prefix $relationType $MAX_INSTANCES_PER_SEED $NEGATIVE_SAMPLING_RATIO LABELING_INSTANCE"));
                my $input_json="${output_file_prefix}/data.json";
        }
}

dojobs();

my $list_input_json="${dir_opennre_final}/list_json_files";
system "find ${dir_opennre_intermediate}/ | grep data.json > ${list_input_json}";
my $output_json="${dir_opennre_final}/final.json";

run_on_queue("${job_prefix}/MergeJsonFile",
		"48G",
		"python $learnit_repo/neolearnit/src/main/python/merge_json_file_decoding.py $list_input_json $output_json");

dojobs();

system "cat ${dir_opennre_intermediate}/*/wordlist.txt|sort|uniq > ${dir_opennre_final}/wordlist.txt";

exit 0;

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
         my $sge_vm_free,
         my $prev_job,
         my $command
        ) = @_;
        return runjobs([$prev_job],"$job_name", {BATCH_QUEUE => $QUEUE, QUEUE_PRIO => $QUEUE_PRIORITY, SGE_VIRTUAL_FREE => "$sge_vm_free"}, $command); 
}

sub run_on_queue {
        (
         my $job_name,
         my $sge_vm_free,
         my $command
        ) = @_;

        runjobs([],"$job_name", {BATCH_QUEUE => $QUEUE, QUEUE_PRIO => $QUEUE_PRIORITY, SGE_VIRTUAL_FREE => "$sge_vm_free"}, $command);
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

