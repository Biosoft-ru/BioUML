#!/usr/bin/perl

use File::Basename;

my %files = ("reads" => shift, "info" => shift, "contigs" => shift, "stdout" => shift);

chdir(dirname($files{"reads"}));

%files = map{($_ => basename($files{$_}))} keys %files;

$cmd  = " $files{reads} x -o:/wrun/ozip/ozip.cfg -O:/wrun/ozip/oozip.cfg -info:1 -pair_info_file:$files{info}";
$cmd .= " -store_cons:$files{contigs} -Fthr:5 -hstep:2 -hlflank:10";
$cmd .= " 1>\"$files{stdout}\"";

system("/wrun/ozip/ozip $cmd");
