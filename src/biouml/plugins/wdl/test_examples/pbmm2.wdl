version 1.0

workflow run_pbmm2 {
  meta {
    description: "Align array of movies using pbmm2."
  }

  parameter_meta {
    # inputs
    reference_name: { help: "Name of the the reference genome, used for file labeling." }
    reference_fasta: { help: "Path to the reference genome FASTA file." }
    reference_index: { help: "Path to the reference genome FAI index file." }
    movies: { help: "Array of BAMs and/or FASTQs containing HiFi reads." }
    sample_name: { help: "Name of the sample." }

    # outputs
    bams: { description: "Array of aligned bam file." }
    bais: { description: "Array of aligned bam index." }
  }

  input {
    String sample_name
    Array[File] movies
    String reference_name
    File reference_fasta
    File reference_index
  }
  
  scatter (idx in range(length(movies))) {     
    # align each movie with pbmm2
    call pbmm2_align {
        input: 
          reference_name = reference_name,
          reference_fasta = reference_fasta,
          reference_index = reference_index,
          movie = movies[idx],
          sample_name = sample_name
    }
  }

  output {
    Array[File] bams = pbmm2_align.bam
    Array[File] bais = pbmm2_align.bai
  }
}


task pbmm2_align {
  meta {
    description: "Aligns HiFi reads to reference genome from either a BAM or FASTQ file."
  }

  parameter_meta {
    # inputs
    reference_name: { help: "Name of the the reference genome, used for file labeling." }
    reference_fasta: { help: "Path to the reference genome FASTA file." }
    reference_index: { help: "Path to the reference genome FAI index file." }
    movie: { help: "An BAM or FASTQ file containing HiFi reads." }
    sample_name: { help: "Name of the sample." }
    threads: { help: "Number of threads to be used." }

    # outputs
    bam: { description: "Aligned bam file." }
    bai: { description: "Aligned bam index." }
  }

  input {
    String reference_name
    File reference_fasta
    File reference_index
    File movie
    String sample_name
    }

  String movie_name = sub(basename(movie), "\\..*", "")
  String output_bam = "~{movie_name}.~{reference_name}.bam"
  Int memory = 16
  Int disk_size = ceil(2.5 * (size(reference_fasta, "GB") + size(reference_index, "GB") + size(movie, "GB"))) + 20
  
  command {
    set -o pipefail
    pbmm2 align \
      --sample ~{sample_name} \
      --log-level INFO \
      --preset CCS \
      --sort \
      --unmapped \
      -c 0 -y 70 \
      -j ~{threads} \
      ~{reference_fasta} \
      ~{movie} \
      ~{output_bam}
    }

  output {
    File bam = output_bam
    File bai = "~{output_bam}.bai"
  }

  runtime {
    cpu: threads
    memory: "~{memory}GB"
    disks: "local-disk ~{disk_size} SSD"
    maxRetries: 3
    preemptible: 1
    docker: "developmentontheedge/pbmm2:1.7.0"
  }
}
