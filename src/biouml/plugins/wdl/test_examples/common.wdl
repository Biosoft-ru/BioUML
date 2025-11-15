version 1.0

task bgzip_fasta {
  input {
    File fasta
    Int threads = 4
  }

  runtime {
    maxRetries: 3
    memory: "~{memory}GB"
    preemptible: 1
    disks: "local-disk ~{disk_size} HDD"
    cpu: threads
    docker: "developmentontheedge/htslib:1.14"
  }

  String output_filename = "~{basename(fasta)}.gz"
  Int memory = 4*threads
  Int disk_size = ceil(3.25*size(fasta,"GB"))+20
  command {
     set -o pipefail
    bgzip --threads ~{threads} ~{fasta} -c > ~{output_filename}
  }

  output {
    File gzipped_fasta = output_filename
  }
}

task sort_vcf {
  input {
    File input_vcf
    String output_filename
  }

  runtime {
    maxRetries: 3
    memory: "~{memory}GB"
    preemptible: 1
    disks: "local-disk ~{disk_size} HDD"
    cpu: threads
    docker: "developmentontheedge/bcftools:1.14"
  }

  Int threads = 1
  Int memory = 4*threads
  Int disk_size = ceil(3.25*size(input_vcf,"GB"))+20
  command {
     set -o pipefail
    bcftools sort ~{input_vcf} -Ov -o ~{output_filename}
  }

  output {
    File vcf = output_filename
  }
}

task zip_and_index_vcf {
  input {
    File input_vcf
    Int threads = 2
  }

  runtime {
    maxRetries: 3
    memory: "~{memory}GB"
    preemptible: 1
    disks: "local-disk ~{disk_size} HDD"
    cpu: threads
    docker: "developmentontheedge/htslib:1.14"
  }

  Int memory = 4*threads
  String output_filename = "~{basename(input_vcf)}.gz"
  Int disk_size = ceil(3.25*size(input_vcf,"GB"))+20
  command {
     set -o pipefail
    bgzip --threads ~{threads} ~{input_vcf} -c > ~{output_filename}
    tabix --preset vcf ~{output_filename}
  }

  output {
    File vcf = output_filename
    File index = "~{output_filename}.tbi"
  }
}

workflow common {
}