version 1.0





task pbmm2_align {
  input {
    String reference_name
    File reference_fasta
    File reference_index
    File movie
    String sample_name
  }

  String movie_name = sub(basename(movie),"\\..*","")
  String output_bam = "~{movie_name}.~{reference_name}.bam"
  Int memory = 4*4
  Int disk_size = ceil(2.5*(size(reference_fasta,"GB")+size(reference_index,"GB")+size(movie,"GB")))+20
  command {
     set -o pipefail
    pbmm2 align \
      --sample ~{sample_name} \
      --log-level INFO \
      --preset CCS \
      --sort \
      --unmapped \
      -c 0 -y 70 \
      -j 4 \
      ~{reference_fasta} \
      ~{movie} \
      ~{output_bam}
  }

  output {
    File bam = output_bam
    File bai = "~{output_bam}.bai"
  }

  runtime {
    maxRetries: 3
    memory: "~{memory}GB"
    preemptible: 1
    disks: "local-disk ~{disk_size} SSD"
    cpu: 4
    docker: "juniperlake/pbmm2:1.7.0"
  }

}
workflow mainWorkflow {
  input {
    Array[File] movies
    File reference_fasta
    File reference_index
    String reference_name
    String sample_name
  }

  meta {
    description: "Align array of movies using pbmm2."
  }

  Array[Int] expression = range(length(movies))
  scatter ( idx in expression ) {
  call pbmm2_align  as pbmm2_align {
     input:  reference_name = reference_name, reference_fasta = reference_fasta, reference_index = reference_index, movie = movies[idx], sample_name = sample_name  }

  }

  Array[File] bais = pbmm2_align.bai
  Array[File] bams = pbmm2_align.bam
}