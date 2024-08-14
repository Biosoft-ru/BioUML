#!/usr/bin/perl -I/wrun/pipeline -I/opt/galaxy/galaxy-dist/tools/softberry

use Cwd;
use strict;
use Data::Dumper;
use Getopt::Long;
use File::Temp qw/ tempfile tempdir /;
use config;
use File::Basename;

my ($fasta,$o3_1,$o3_2,$o5_7,$o6_8,$o9,$o12_1,$o12_2,$o14,$o18,$o19,$o20,$o21,$o22,$o23,$output,$vis);

my $result = GetOptions ("fasta=s" => \$fasta,
                         "o3_1=s"  => \$o3_1,
                         "o3_2=s"  => \$o3_2,
                         "o5_7=s"  => \$o5_7,
                         "o6_8=s"  => \$o6_8,
                         "o9=s"    => \$o9,
                         "o12_1=s" => \$o12_1,
                         "o12_2=s" => \$o12_2,
                         "o14=s"   => \$o14,
                         "o18=s"   => \$o18,
                         "o19=s"   => \$o19,
                         "o20=s"   => \$o20,
                         "o21=s"   => \$o21,
                         "o22=s"   => \$o22,
                         "o23=s"   => \$o23,
                         "output=s" => \$output,
                         "vis=s"   => \$vis,
                        );

chdir(dirname($fasta));
$fasta = basename($fasta);
rename($fasta,"seq.fa");
$fasta = "seq.fa";

#1
my $cfg = "# locations of programs and data for bacterial genome annotation\n";
#2
$cfg .= "/wrun/BACT/BACT_SCRIPTS/                ## directory with scripts and programs\n";
#3
if ($o3_1 eq "Bacterial") {
  $cfg .= "/wrun/BACT/BACT_SCRIPTS/bact.par ## $o3_2 ## gene finding parameters\n";
}
elsif ($o3_1 eq "Archaeal") {
  $cfg .= "/wrun/BACT/BACT_SCRIPTS/archae.par ## $o3_2 ## gene finding parameters\n";
}
elsif ($o3_1 eq "Bacterial + archaeal") {
  $cfg .= "/wrun/BACT/BACT_SCRIPTS/gener.par ## $o3_2 ## gene finding parameters\n";
}
#4
$cfg .= "/wrun/BACT/blast-2.2.17/bin/blastall ## 8 ## 6 ## blast (for blastn)\n";
#5-8
my ($f1,$f2,$f3,$f4); $f1 = $f2 = $f3 = $f4 = 0;
if ($o5_7 eq "true") {
  if    ($o3_1 eq "Bacterial")            {$f1=1}
  elsif ($o3_1 eq "Archaeal")             {$f3=1}
  elsif ($o3_1 eq "Bacterial + archaeal") {$f1=$f3=1}
}
if ($o6_8 eq "true") {
  if    ($o3_1 eq "Bacterial")            {$f2=1}
  elsif ($o3_1 eq "Archaeal")             {$f4=1}
  elsif ($o3_1 eq "Bacterial + archaeal") {$f2=$f4=1}
}
$cfg .= "/wrun/BACT/DATA/rrna_db/bact_LSRRNA.set ## $f1 ## 300 ## bact. rRNA DB\n";
$cfg .= "/wrun/BACT/DATA/rrna_db/bact_5SRRNA.set ## $f2 ## 50 ## bact. 5S rRNA DB\n";
$cfg .= "/wrun/BACT/DATA/rrna_db/arch_LSRRNA.set ## $f3 ## 50 ## arch. rRNA DB\n";
$cfg .= "/wrun/BACT/DATA/rrna_db/arch_5SRRNA.set ## $f4 ## 50 ## arch. 5S rRNA DB\n";
#9
$f1 = 0;
if ($o9 eq "true") {
  if    ($o3_1 eq "Bacterial")            {$f1=2}
  elsif ($o3_1 eq "Archaeal")             {$f1=3}
  elsif ($o3_1 eq "Bacterial + archaeal") {$f1=1}
}
$cfg .= "/wrun/BACT/tRNAscan-SE/bin/tRNAscan-SE  ## $f1 ## 0 - not use, 1 use both bact and arch, 2 - use only bact, 3 use only arch\n";
#10
$cfg .= "/wrun/BACT/BACT_SCRIPTS/map.par         ## thresholds/parameters for protein mapping\n";
#11
$cfg .= "/wrun/BACT/blast-2.2.17/bin/blastall    ## 10 ##  6 ## blast executable (for blastx)\n";
#12
$f1 = $f2 = 0;
$f1 = 1 if $o12_1 eq "true";
$f2 = 1 if $o12_2 eq "true";
$cfg .= "/wrun/BACT/DATA/rbp_db/nr_ba_rbp        ## NR  ## $f1  ##  $f2 ## ribosomal proteins DB\n";
#13
$cfg .= "/wrun/BACT/blast-2.2.17/bin/blastpgp    ## 8 ##   6 ## blast executable\n";
#14
$f1 = 0;
$f1 = 1 if $o14 eq "true";
$cfg .= "/wrun/BACT/DATA/cog_db/cog.pro          ## COG  ## $f1 ## COG database\n";
#15
$cfg .= "/wrun/BACT/DATA/cog_db/cog_gene.list    ##              COG gene list\n";
#16
$cfg .= "/wrun/BACT/DATA/cog_db/org.list         ##              COG organisms list\n";
#17
$cfg .= "2\n";
#18
$cfg .= "/wrun/BACT/DB/KEGG/kegg.fa              ## KEGG ## $o18 ## KEGG protein database\n";
#19
$cfg .= "/wrun/BACT/DB/NR/nr_ba                  ## NR   ## $o19 ## nr protein DB\n";
#20
($o20 eq "true")?($o20 = 1):($o20 = 0);
$cfg .= "$o20                                       ## 1 - predict promoters/terminators, 0 - not predict\n";
#21
($o21 eq "true")?($o21 = 1):($o21 = 0);
$cfg .= "$o21                                       ## replace alternative start codons with M in proteins\n";
#22
($o22 eq "true")?($o22 = 1):($o22 = 0);
$cfg .= "$o22                                       ## make short protein deflines (for nr-like homologs)\n";
#23
($o23 eq "true")?($o23 = 1):($o23 = 0);
$cfg .= "$o23                                       ## 1 - add name of sequence to gene predictions, 0 - not add\n";

#print $cfg;
open F, ">config.cfg";
print F $cfg;
close F;

#my $cmd = "/wrun/BACT/BACT_SCRIPTS/bamg.pl config.cfg seq.fa annotation.txt 60 1>stdout.txt 2>log.txt";
system('/wrun/BACT/BACT_SCRIPTS/bamg.pl config.cfg seq.fa annotation.resa 60 2>&1');
if (-f "annotation.resa") {
print "... Visualization\n";
`/wrun/BACT/FgenesB_converters/FgenesB_2_CGView/fgenesb_2_cgview.pl --input annotation.resa --output CGView.xml`;
rename("annotation.resa",$output);
system("java -cp /wrun/CGView/lib -jar /wrun/CGView/cgview.jar -i CGView.xml -s vis -e T -W 500 -H 500 >/dev/null 2>/dev/null");
system("chdir vis;zip -r $vis * >/dev/null");
}
