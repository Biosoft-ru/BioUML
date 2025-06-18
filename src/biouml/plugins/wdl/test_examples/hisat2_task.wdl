version 1.1

task hisat2 {
  input {
    File index
    String sra_acc
    Int? max_reads
    Int threads = 8
    Float memory_gb = 16
    Float disk_size_gb = 100
  }

  String index_id = basename(index, ".tar.gz")

  command <<<
    mkdir index
    tar -C index -xzf ~{index}
    hisat2 \
      -p ~{threads} \
      ~{if defined(max_reads) then "-u ~{select_first([max_reads])}" else ""} \
      -x index/~{index_id} \
      --sra-acc ~{sra_acc} > ~{sra_acc}.sam
  >>>
  
  output {
    File sam = "output.sam"
  }
  
  runtime {
    container: "quay.io/biocontainers/hisat2:2.2.1--h1b792b2_3"
    cpu: threads
    memory: "~{memory_gb} GB"
    disks: "~{disk_size_gb} GB"
  }

  meta {
    description: "Align single-end reads with BWA MEM"
  }

  parameter_meta {
    index: "Gzipped tar file with HISAT2 index files"
    sra_acc: "SRA accession number or reads to align"
  }
}