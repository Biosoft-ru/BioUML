version 1.1

import "faidx" as samtools_faidx

workflow faidx_import {
 input {
  File reference_fasta
  File region_fasta
 }

 call samtools_faidx.samtools_faidx as faidx{
  input: reference_fasta = reference_fasta }

 call samtools_faidx.samtools_faidx as faidx1 {
  input: reference_fasta = region_fasta }

 output {
  File ref_fasts_fai = faidx.reference_index
  File region_fasta_fai = faidx1.reference_index
 }
}