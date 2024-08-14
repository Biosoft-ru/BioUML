#!/usr/bin/perl

use File::Basename;

my %files = ("target_seq" => shift, "query_seq" => shift);
my %options = ("chain" => shift);
my $output = shift;

chdir(dirname($files{target_seq}));
$files{target_seq} = basename($files{target_seq});
$files{query_seq} = basename($files{query_seq});

$cmd = "$files{target_seq} $files{query_seq} ";
$cmd .= " -o:/wrun/synteny/bin/align.cfg";
$cmd .= " -D:0" if $options{chain} eq 'direct';
$cmd .= " -D:1" if $options{chain} eq 'reverse';
$cmd .= " -D:2" if $options{chain} eq 'both';
$cmd .= " -nthreads:4";

system("perl -I /opt/galaxy/galaxy-dist/tools/softberry /wrun/synteny/bin/sbl_wrapper.pl $cmd");

for my $da (glob "*.da")
{
    my $newda = $da;
    $newda =~ s/^.+\:(\d+)\:00\.da$/seq_$1.da/;
    rename($da,$newda);
}

system("tar cjvf $output *.da");
