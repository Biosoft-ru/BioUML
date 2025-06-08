version 1.1

struct Reference {
  String id
  File fasta
  File index
  File dict
}

task gatk_haplotype_caller {
  input {
    File bam
    Reference reference
    String? interval
    Int memory_gb = 4
    Float? disks_gb
    String? sample_id
  }
  
  String prefix = select_first([sample_id, basename(bam, ".bam")])
  Float disk_size_gb = select_first([
    disks_gb, 10 + size([bam, reference.fasta], "GB")
  ])

  command <<<
    # ensure all reference files are in the same directory
    mkdir ref
    ln -s ~{reference.fasta} ref/~{reference.id}.fasta
    ln -s ~{reference.index} ref/~{reference.id}.fasta.fai
    ln -s ~{reference.dict} ref/~{reference.id}.dict
    gatk --java-options "-Xmx~{memory_gb}g" HaplotypeCaller \
      ~{if defined(interval) then "-L ~{select_first([interval])}" else ""} \
      -R ref/~{reference.id}.fasta \
      -I ~{bam} \
      -O ~{prefix}.vcf
  >>>

  output {
    File vcf = "~{prefix}.vcf"
  }

  parameter_meta {
    bam: "BAM file to call"
    reference_fasta: "Reference genome in FASTA format"
    memory_gb: "Amount of memory to allocate to the JVM in GB"
    disks_gb: "Amount of disk space to reserve"
    sample_id: "The ID of the sample to call"
  }

  meta {
    author: "Joe Somebody"
    email: "joe@company.org"
  }
  
  runtime {
    container: "broadinstitute/gatk"
    memory: "~{memory_gb + 1} GB"
    disks: "~{disk_size_gb} GB"
  }
}