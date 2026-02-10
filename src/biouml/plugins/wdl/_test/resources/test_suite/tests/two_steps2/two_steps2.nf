nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll; basename } from './biouml_function.nf'
params.indices = [1,3,5]
params.extra_info

process generate_file {

  fair true
  publishDir "generate_file", mode: 'copy'

  input:
  val index

  output:
  path "output_${index}.txt", emit: out_file 

  script:
  """
  echo "This is file number ${index}" > output_${index}.txt
  """
}

process process_file {

  fair true
  publishDir "process_file", mode: 'copy'

  input:
  path in_file
  val extra_info

  output:
  path "processed_${basename(in_file)}", emit: processed_file 

  script:
  """
  echo "Extra info: ${extra_info}" > processed_${basename(in_file)}
    cat ${in_file} >> processed_${basename(in_file)}
  """
}

workflow mainWorkflow {

  take:
  indices
  extra_info

  main:

  generate_file_input_index = toChannel( indices )  
result_generate_file = generate_file( generate_file_input_index )

  process_file_input_in_file = toChannel( result_generate_file.out_file )  
result_process_file = process_file( process_file_input_in_file, extra_info )

  emit: 
  generated_files = result_generate_file.out_file
  processed_files = result_process_file.processed_file
}

workflow {
mainWorkflow( params.indices, params.extra_info  )
}
