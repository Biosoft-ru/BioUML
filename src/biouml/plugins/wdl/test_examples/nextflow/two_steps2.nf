nextflow.enable.dsl=2
include { basename; sub; length; range; toChannel; getDefault; read_int; numerate; select_first } from './biouml_function.nf'
params.indices = [1,3,5]
params.extra_info = "Something"

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
  val extra_info


  publishDir "process_file", mode: 'copy'

  script:
  """
  echo "Extra info: ${extra_info}" > processed_${basename(in_file)}
    cat ${in_file} >> processed_${basename(in_file)}
  """

  output:
  path "processed_${basename(in_file)}", emit: processed_file 
}

workflow mainWorkflow {
  take:
  indices
  extra_info



  main:
  indices_mapping = toChannel(indices).multiMap {i -> 
    generate_file_input: tuple(i)
  }

  result_generate_file = generate_file( indices_mapping.generate_file_input.map { it[0] } )

  indices_mapping2 = toChannel(indices).multiMap {i -> 
    process_file_input: tuple(extra_info)
  }
 
  result_process_file = process_file( result_generate_file.out_file, indices_mapping2.process_file_input.map { it[0] } )

  emit: 
  generated_files = result_generate_file.out_file
  processed_files = result_process_file.processed_file
}

workflow {
mainWorkflow( params.indices, params.extra_info  )
}