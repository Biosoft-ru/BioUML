version 1.0

task fastQC {
  input {
    File read
  }

  runtime {
    docker: "biocontainers/fastqc:v0.11.9_cv8"
  }

  command {
     fastqc ~{read}
  }

  output {
    Array[File] out_files = "*_fastqc.*"
  }
}

task multiQC {
  input {
    Array[File] inputfiles
  }

  runtime {
    docker: "quay.io/biocontainers/multiqc:1.21--pyhdfd78af_0"
  }

  command {
     multiqc .
  }

  output {
    File multiqc_report = "multiqc_report.html"
  }
}

workflow full {
  input {
    Array[File] reads = "reads/*.fastq.gz"
    String readsDir = biouml.get("data/Collaboration/Ilya/Data/Diagrams/WDL/FASTQC/reads")
  }

  scatter ( read in reads ) {
  call fastQC {
     input:  read = read  }

  }

  call multiQC {
     input:  inputfiles = fastQC.out_files  }

}