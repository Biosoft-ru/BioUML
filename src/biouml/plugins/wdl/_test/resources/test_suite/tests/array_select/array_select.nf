nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.num_files
params.index

process generate_files {

  fair true
  publishDir "generate_files", mode: 'copy'

  input:
  val num_files

  output:
  path "output/file_*.txt", emit: files 

  script:
  """
  mkdir -p output
        for i in \$(seq 1 ${num_files}); do
            echo "This is file \$i" > output/file_\${i}.txt
        done
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
  num_files
  index

  main:
  result_generate_files = generate_files( num_files )

  result_process_file = process_file( get(result_generate_files.files, index), "res_2" )

  emit: 
  processed_file = result_process_file.output_file
}

workflow {
mainWorkflow( params.num_files, params.index  )
}
