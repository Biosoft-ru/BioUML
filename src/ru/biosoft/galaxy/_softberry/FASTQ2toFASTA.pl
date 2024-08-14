#!/usr/bin/perl

use File::Basename;

my $f1 = shift;
my $f2 = shift;
my $pi = shift;

my $phred = shift;

my $output = shift;
my $output_pi = shift;

chdir(dirname($f1));
$f1 = basename($f1);
$f2 = basename($f2);

$cmd  = " --in1 $f1 --in2 $f2 --inf $pi --out $output --phred $phred >$output_pi";

system("perl -I /opt/galaxy/galaxy-dist/tools/softberry /wrun/ozip/fastq2_2_fasta_wrapper.pl $cmd");
