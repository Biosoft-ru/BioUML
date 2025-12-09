version 1.0





task fastQC {
  input {
    File read
  }

  command {
     fastqc ~{read}
  }

  output {
    Array[File] out_files = "*_fastqc.*"
  }

  runtime {
    docker: "biocontainers/fastqc:v0.11.9_cv8"
  }

}
task multiQC {
  input {
    Array[File] inputfiles
  }

  command {
     multiqc .
  }

  output {
    File multiqc_report = "multiqc_report.html"
  }

  runtime {
    docker: "quay.io/biocontainers/multiqc:1.21--pyhdfd78af_0"
  }

}
workflow mainWorkflow {
  input {
    Array[File] reads
    String readsDir
  }

  scatter ( read in reads ) {
  call fastQC  as fastQC {
     input:  read = read  }

  }

  Array[File] fastQC_result = fastQC.out_files
  call multiQC  as multiQC {
     input:  inputfiles = fastQC_result  }

  output {
    File report = multiQC.multiqc_report
  }
}