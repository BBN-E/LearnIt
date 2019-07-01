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
our $QUEUE_PRIORITY = 9;

my ($expt_dir, $exp) = startjobs();
max_jobs(1000);  # Max number of jobs to run at once

my $MAVEN_BIN = "/opt/apache-maven-3.0.4/bin/mvn";

# my $job_prefix = "gigaword_1.5M.v2";
# add_event_mentions("/nfs/mercury-04/u41/learnit/gigaword_1.5M/source_lists/",
#                       "/nfs/mercury-04/u41/learnit/gigaword_1.5M/serifxml.add_extended_events2/");

my $job_prefix = "causeex-m15.v2";
add_event_mentions("/nfs/mercury-04/u41/learnit/CauseEx-M15/source_lists/",
	"/nfs/mercury-04/u41/learnit/CauseEx-M15/serifxml/");

endjobs();

sub add_event_mentions {
	(
		my $source_list_dir,
                my $output_base_dir
	) = @_;

	my @batch_source_lists = glob "$source_list_dir/*";
	foreach my $batch_list (@batch_source_lists) {
		my ($batch_name,$batch_dir) = fileparse($batch_list);

		my $output_dir = "$output_base_dir/$batch_name";

		run_on_queue("m15-make-output-dir-${batch_name}", 
                                "2G",
				"mkdir -p $output_dir");
		
		run_on_queue_with_prev_job("m15-add_event_mentions-${batch_name}",
                                "4G",
                                "m15-make-output-dir-${batch_name}",
				 make_java_command("4G", "/nfs/ld100/u10/bmin/repositories/jserif.364-add-event-from-json/serif-util/target/appassembler/bin/", "AddEventMentionByPOSTags", "$batch_list $output_dir"));
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

