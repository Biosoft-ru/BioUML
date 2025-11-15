version 1.1

task samtools_faidx {
 input {
 File reference_fasta

 }

 runtime {
  docker: "developmentontheedge/samtools:latest"
 }

 command {
  samtools faidx ~{reference_fasta}
 }

  output {
   File reference_index = "~{reference_fasta}.fai"
  }
}

workflow faidx2 {
 input {
  File reference_fasta
  File region_fasta
 }

 call samtools_faidx {
  input: reference_fasta = reference_fasta }

 call samtools_faidx as faidx1 {
  input: reference_fasta = region_fasta }

 output {
  File ref_fasts_fai = samtools_faidx.reference_index
  File region_fasta_fai = faidx1.reference_index
 }
}