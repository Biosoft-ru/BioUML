nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
process generate_file {

  fair true
  publishDir "generate_file", mode: 'copy'

  output:
  path "greeting.txt", emit: out 

  script:
  """
  echo "Hello!" > greeting.txt
  """
}

process process_files {

  fair true
  publishDir "process_files", mode: 'copy'

  input:
  path input_files
  val output_name

  output:
  path output_name, emit: output_file 

  script:
  input_files_str = input_files.join(' ')
  """
  for file in ${input_files_str}; do
       echo "Processing: \$file" >> ${output_name}
       cat "\$file" >> ${output_name}
   done
  """
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
