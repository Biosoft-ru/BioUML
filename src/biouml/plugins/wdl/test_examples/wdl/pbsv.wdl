version 1.0

import "common.wdl" as common




task concat_pbsv_vcfs {
  input {
    String sample_name
    String reference_name
    Array[File] input_vcfs
    Array[File] input_indexes
    String output_filename = "~{sample_name}.~{reference_name}.pbsv.vcf"
    Int threads = 4
  }

  Int memory = 4*threads
  Int disk_size = ceil(3.25*(size(input_vcfs,"GB")+size(input_indexes,"GB")))+20
  command {
     set -o pipefail
    bcftools concat --allow-overlaps \
      --output ~{output_filename} \
      ~{sep=" " input_vcfs} \
  }

  output {
    File vcf = output_filename
  }

  runtime {
    maxRetries: 3
    memory: "~{memory}GB"
    preemptible: 1
    disks: "local-disk ~{disk_size} SSD"
    cpu: threads
    docker: "juniperlake/bcftools:1.14"
  }

}
task pbsv_call_by_region {
  input {
    String sample_name
    Array[File] svsigs
    String reference_name
    File reference_fasta
    File reference_index
    String region
    String output_filename = "~{sample_name}.~{reference_name}.~{region}.pbsv.vcf"
    Int threads = 8
  }

  Int memory = 6*threads
  Int disk_size = 200
  command {
     set -o pipefail

    for svsig in ~{sep=" " svsigs}; do
      if [[ $svsig != *~{region}.svsig.gz ]]; then
        printf '%s\n' "Region does not match svsig filename." >&2
        exit 1
      fi
    done

    pbsv call \
      --hifi \
      --min-sv-length 30 \
      --call-min-reads-all-samples 2 \
      --call-min-reads-one-sample 2 \
      --call-min-read-perc-one-sample 10 \
      --num-threads ~{threads} \
      ~{reference_fasta} \
      ~{sep=" " svsigs} \
      ~{output_filename}
  }

  output {
    File vcf = output_filename
  }

  runtime {
    maxRetries: 3
    memory: "~{memory}GB"
    preemptible: 1
    disks: "local-disk ~{disk_size} SSD"
    cpu: threads
    docker: "juniperlake/pbsv:2.8"
  }

}
task pbsv_discover_by_region {
  input {
    String sample_name
    String region
    Array[File] bams
    Array[File] bais
    String svsig_filename = "~{sample_name}.~{region}.svsig.gz"
    Int threads = 4
  }

  Int memory = 4*threads
  Int disk_size = ceil(3.25*(size(bams,"GB")))+20
  command {
     set -o pipefail
    
    # symlink bams and bais to a single folder so indexes can be found
    mkdir bams_and_bais
    for file in ~{sep=" " bams} ~{sep=" " bais}; do 
      ln -s "$(readlink -f $file)" bams_and_bais
    done
    
    # make XML dataset so all bams can be processed with one pbsv command
    dataset create --type AlignmentSet --novalidate --force ~{region}.xml bams_and_bais/*.bam

    pbsv discover \
      --sample ~{sample_name}_pbsv \
      --hifi \
      --min-mapq 20 \
      --region ~{region} \
      ~{region}.xml \
      ~{svsig_filename}
  }

  output {
    File svsig = svsig_filename
  }

  runtime {
    maxRetries: 3
    memory: "~{memory}GB"
    preemptible: 1
    disks: "local-disk ~{disk_size} SSD"
    cpu: threads
    docker: "juniperlake/pbsv:2.8"
  }

}
workflow mainWorkflow {
  input {
    Array[File] bais
    Array[File] bams
    File reference_fasta
    File reference_index
    String reference_name
    Array[String] regions
    String sample_name
  }

  Array[Int] expression = range(length(regions))
  scatter ( idx in expression ) {
  call pbsv_discover_by_region  as pbsv_discover_by_region {
     input:  sample_name = sample_name, bams = bams, bais = bais, region = regions[idx]  }

  call pbsv_call_by_region  as pbsv_call_by_region {
     input:  sample_name = sample_name, svsigs = [pbsv_discover_by_region.svsig], reference_name = reference_name, reference_fasta = reference_fasta, reference_index = reference_index, region = regions[idx]  }

  call common.zip_and_index_vcf  as zip_and_index_vcf {
     input:  input_vcf = pbsv_call_by_region.vcf  }

  }

  Array[File] svsigs = pbsv_discover_by_region.svsig
  call concat_pbsv_vcfs  as concat_pbsv_vcfs {
     input:  sample_name = sample_name, reference_name = reference_name, input_vcfs = zip_and_index_vcf.vcf, input_indexes = zip_and_index_vcf.index  }

  call common.zip_and_index_vcf  as zip_and_index_final_vcf {
     input:  input_vcf = concat_pbsv_vcfs.vcf  }

  Array[File] region_indexes = zip_and_index_vcf.index
  Array[File] region_vcfs = zip_and_index_vcf.vcf
  File index = zip_and_index_final_vcf.index
  File vcf = zip_and_index_final_vcf.vcf
}