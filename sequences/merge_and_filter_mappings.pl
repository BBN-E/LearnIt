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
our $QUEUE_PRIORITY = 5;

my ($expt_dir, $exp) = startjobs();

max_jobs(400);  # Max number of jobs to run at once

my $MAVEN_BIN = "/opt/apache-maven-3.0.4/bin/mvn";

my $learnit_root="/nfs/ld100/u10/bmin/repositories/learnit.master";

my $job_prefix = "gigaword_1.5M.v5";
my $params="$learnit_root/params/learnit/runs/gigaword_1.5M.params";

my $initial_mappings_list="$learnit_root/resources/mappings_for_nre_train.list";
my $tmp_list_dir="$expt_dir/expts/$job_prefix/mappings/lists/";
my $out_dir="$expt_dir/expts/$job_prefix/mappings/";
my $final_mappings_list="$expt_dir/expts/$job_prefix/mappings/list_sjson";
my $final_mappings="$expt_dir/expts/$job_prefix/mappings/final.sjson";

mkdir $expt_dir."/expts/".$job_prefix;
mkdir $out_dir;
mkdir $tmp_list_dir;

# my $job_prefix = "gigaword_1.5M";
# my $params="$learnit_root/params/learnit/runs/gigaword_1.5M.params";

# my $initial_mappings_list="$learnit_root/resources/mappings_for_nre_test.list";
# my $tmp_list_dir="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/tmp_lists/";
# my $out_dir="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/";
# my $final_mappings_list="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/list_sjson";
# my $final_mappings="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/final.sjson";

# my $initial_mappings_list="/nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/resources/mappings_for_nre_train.list";
# my $tmp_list_dir="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/tmp_lists/";
# my $out_dir="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/";
# my $final_mappings_list="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/list_sjson";
# my $final_mappings="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/final.sjson";

my $seedMin="3";
my $seedMax="-1";
my $patternMin="3";
my $patternMax="-1";

merge_and_filter_mappings($initial_mappings_list, $tmp_list_dir, $out_dir,
		$seedMin,
		$seedMax,
		$patternMin,
		$patternMax);

system "find $out_dir/*.sjson > $final_mappings_list";

run_on_queue("${job_prefix}/merge-mappings-all", 
		"96G",
		make_java_command("96G", $learnit_root."/neolearnit/target/appassembler/bin/", "MergeMappings", "$params $final_mappings_list $final_mappings 10 -1 10 -1"));

dojobs();

endjobs();

sub merge_and_filter_mappings {
	(
		my $source_list,
                my $tmp_list_dir,
                my $output_base_dir, 
		my $seedMin,
		my $seedMax,
		my $patternMin,
		my $patternMax
	) = @_;

        my $BATCH_SIZE="50";
        system "split -d -a 5 -l $BATCH_SIZE $source_list $tmp_list_dir/list";
     
	my @batch_source_lists = glob "${tmp_list_dir}/list*";
	foreach my $batch_list (@batch_source_lists) {
		my ($batch_name,$batch_dir) = fileparse($batch_list);

		my $output_file = "$output_base_dir/$batch_name.sjson";

		run_on_queue("${job_prefix}/merge-mappings-${batch_name}", 
                                "48G",
				 make_java_command("48G", $learnit_root."/neolearnit/target/appassembler/bin/", "MergeMappings", "$params $batch_list $output_file $seedMin $seedMax $patternMin $patternMax"));
	}
	
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

