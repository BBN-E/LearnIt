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

my $job_prefix = "gigaword_1.5M";
my $params="/nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/params/learnit/runs/gigaword_1.5M.params";

#my $initial_mappings_list="/nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/resources/mappings_for_nre_test.list";
#my $tmp_list="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/tmp_lists/list";
#my $out_dir="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/";
#my $final_mappings_list="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/list_sjson";
#my $final_mappings="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_test/final.sjson";
#my $prefix="test";

my $initial_mappings_list="/nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/resources/mappings_for_nre_train.list";
my $tmp_list="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/tmp_lists/list";
my $out_dir="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/";
my $final_mappings_list="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/list_sjson";
my $final_mappings="/nfs/mercury-04/u41/learnit/gigaword_1.5M/mappings_for_nre_train/final.sjson";
my $prefix="train";

my $seedMin="3";
my $seedMax="-1";
my $patternMin="-1";
my $patternMax="-1";

merge_and_filter_mappings($prefix, $initial_mappings_list, $tmp_list, $out_dir,
		$seedMin,
		$seedMax,
		$patternMin,
		$patternMax);

system "find $out_dir/*.sjson > $final_mappings_list";

run_on_queue("merge-mappings-${prefix}-all", 
		"96G",
		make_java_command("96G", "/nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/neolearnit/target/appassembler/bin/", "MergeMappings", "$params $final_mappings_list $final_mappings 50 -1 -1 -1"));

dojobs();

endjobs();

sub merge_and_filter_mappings {
	(
		my $prefix,
		my $source_list,
                my $temporay_list_prefix,
                my $output_base_dir, 
		my $seedMin,
		my $seedMax,
		my $patternMin,
		my $patternMax
	) = @_;

        my $BATCH_SIZE="40";
        system "split -d -a 5 -l $BATCH_SIZE $source_list $temporay_list_prefix";
     
	my @batch_source_lists = glob "${temporay_list_prefix}*";
	foreach my $batch_list (@batch_source_lists) {
		my ($batch_name,$batch_dir) = fileparse($batch_list);

		my $output_file = "$output_base_dir/$batch_name.sjson";

		run_on_queue("merge-mappings-${prefix}-${batch_name}", 
                                "48G",
				 make_java_command("48G", "/nfs/ld100/u10/bmin/repositories/learnit.hqiu_dev/neolearnit/target/appassembler/bin/", "MergeMappings", "$params $batch_list $output_file $seedMin $seedMax $patternMin $patternMax"));
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

