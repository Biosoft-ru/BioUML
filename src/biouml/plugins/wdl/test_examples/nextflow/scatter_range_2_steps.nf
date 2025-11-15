nextflow.enable.dsl=2
include { basename; sub; length; range } from './biouml_function.nf'
params.indices = [1,3,5]

process generate_file {

  input :
  val index


  publishDir "generate_file", mode: 'copy'

  script:
  """
  echo "This is file number ${index}" > output_${index}.txt
  """

  output:
  path "output_${index}.txt", emit: out_file 
}

process process_file {

  input :
  path in_file


  publishDir "process_file", mode: 'copy'

  script:
  """
  cat ${in_file} > processed_${basename(in_file)}
  """

  output:
  path "processed_${basename(in_file)}", emit: processed_file 
}

workflow scatter_range_2_steps {
  take:
  indices

  main:
  index_ch = Channel.from(indices)
  result_generate_file = generate_file( index_ch )

  result_process_file = process_file( result_generate_file.out_file )

  emit: 
  generated_files = result_generate_file.out_file
  processed_files = result_process_file.processed_file
}

workflow {
scatter_range_2_steps( params.indices  )
}