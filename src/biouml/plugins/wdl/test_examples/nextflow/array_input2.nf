nextflow.enable.dsl=2
include { basename; sub; length; range; toChannel; getDefault; read_int; numerate; select_first } from './biouml_function.nf'
process generate_file {

  fair true

  publishDir "generate_file", mode: 'copy'

  script:
  """
  echo "Hello!" > greeting.txt
  """

  output:
  path "greeting.txt", emit: out 
}

process process_files {

  fair true

  input :
  path input_files
  val output_name


  publishDir "process_files", mode: 'copy'

  script:
  input_files_str = input_files.join(' ')
  """
  for file in ${input_files_str}; do
       echo "Processing: \$file" >> ${output_name}
       cat "\$file" >> ${output_name}
   done
  """

  output:
  path output_name, emit: output_file 
}

workflow mainWorkflow {



  main:
  result_generate_file = generate_file( )

  result_process_files = process_files( result_generate_file.out, "res_all.txt" )

  emit: 
  processed_file = result_process_files.output_file
}

workflow {
mainWorkflow(  )
}