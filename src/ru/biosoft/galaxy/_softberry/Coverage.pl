#!/usr/bin/perl

use File::Basename;

my $dabz = shift;

chdir(dirname($dabz));
system("tar xjvf $dabz");
unlink($dabz);

my %files = ("da" => [glob("*.da")]);
my %options = ("sy" => shift);

my $output = shift;

open F, ">da_list.txt";
if (ref $files{da} eq "ARRAY") {
  print F "$_\n" foreach ( @{$files{da}} );
}
else {
  print F "$files{da}\n";
}
close F;
$cmd = "da_list.txt -o:/wrun/synteny/bin/g_mask.cfg";
$cmd .= " -sy_one_path" if $options{sy} eq "for the entire set of query sequences";
$cmd .= " -sy_seq_path" if $options{sy} eq "for each of the query sequence alone";

system("perl -I /opt/galaxy/galaxy-dist/tools/softberry /wrun/synteny/bin/synteny_wrapper.pl $cmd");

open(F,">index.html");
print F <<EOF;
<html><head><body>
EOF

for my $da (glob "*.align")
{
    my $newda = $da;
    $newda =~ s/.da.align$/.txt/;
    rename($da,$newda);
    print F qq(<a href="$newda">$newda</a><br>);
}
print F <<EOF;
</body></html>
EOF
close(F);

system("zip $output index.html seq*.txt");
