nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
process generate_file {

  fair true
  publishDir "generate_file", mode: 'copy'

  input:
  val index

  output:
  path "output_${index}.txt", emit: out_files 

  script:
  """
  echo "This is file number ${index}" > output_${index}.txt
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
  expression = [0,1,2]


  generate_file_input_index = toChannel( expression )  
result_generate_file = generate_file( generate_file_input_index )

  result_process_files = process_files( result_generate_file.out_files.collect(), "res_all.txt" )

  emit: 
  processed_file = result_process_files.output_file
}

workflow {
mainWorkflow(  )
}
