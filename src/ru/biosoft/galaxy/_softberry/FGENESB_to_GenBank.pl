#!/usr/bin/perl

use File::Temp qw/ tempfile tempdir /;

my %files = (
	"fasta" => shift,
	"annotation" => shift,
	"gb_annotation" => shift,
);

chdir "/wrun/BACT/FgenesB_converters/FgenesB_2_GenBank/";
$cmd = "-a \"$files{annotation}\" \"/wrun/xmldata/examples/FGENESB_Converter/header.bac\" \"$files{fasta}\" \"$files{gb_annotation}\"";
print("/wrun/BACT/FgenesB_converters/FgenesB_2_GenBank/fgenesb_2_genbank.pl $cmd");
system("/wrun/BACT/FgenesB_converters/FgenesB_2_GenBank/fgenesb_2_genbank.pl $cmd");
