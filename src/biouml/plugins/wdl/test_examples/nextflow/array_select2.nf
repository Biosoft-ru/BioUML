nextflow.enable.dsl=2
include { basename; sub; length; range; toChannel; getDefault; read_int; numerate; select_first } from './biouml_function.nf'
params.indices = [1,3,4]
params.index = 2

process generate_file {

  fair true

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

  fair true

  input :
  path input_file
  val output_name


  publishDir "process_file", mode: 'copy'

  script:
  """
  echo "Processed file: ${input_file}" >> ${output_name}
       cp ${input_file} ${output_name}
  """

  output:
  path output_name, emit: output_file 
}

workflow mainWorkflow {
  take:
  indices
  index



  main:
  indices_mapping = toChannel(indices).multiMap {i -> 

    generate_file_input: tuple(i)
  }

  result_generate_file = generate_file( indices_mapping.generate_file_input.map { it[0] } )
result_generate_file.out_file.collect().map{v->v[index]}.view()
  result_process_file = process_file( result_generate_file.out_file.collect().map{v->v[index]}, "res_2" )

  emit: 
  processed_file = result_process_file.output_file
}

workflow {
mainWorkflow( params.indices, params.index  )
}