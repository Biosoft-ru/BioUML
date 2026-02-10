nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.indices = [1,3,4]
params.index = 2

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
  path input_file
  val output_name

  output:
  path output_name, emit: output_file 

  script:
  """
  echo "Processed file: ${input_file}" >> ${output_name}
       cp ${input_file} ${output_name}
  """
}

workflow mainWorkflow {

  take:
  indices
  index

  main:

  generate_file_input_index = toChannel( indices )  
result_generate_file = generate_file( generate_file_input_index )

  result_process_file = process_file( get(result_generate_file.out_file.collect(), index), "res_2" )

  emit: 
  processed_file = result_process_file.output_file
}

workflow {
mainWorkflow( params.indices, params.index  )
}
