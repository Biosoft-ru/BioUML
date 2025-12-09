version 1.1

task samtools_faidx {
  input {
    File reference_fasta
  }

  command {
     samtools faidx ~{reference_fasta}
  }

  output {
    File reference_index = "~{reference_fasta}.fai"
  }

  runtime {
    docker: "developmentontheedge/samtools:latest"
  }

}
workflow mainWorkflow {
  input {
    File reference_fasta
  }

  call samtools_faidx  as samtools_faidx {
     input:  reference_fasta = reference_fasta  }

  output {
    File ref_fasts_fai = samtools_faidx.reference_index
  }
}