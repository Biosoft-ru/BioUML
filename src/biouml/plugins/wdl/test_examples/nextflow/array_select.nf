nextflow.enable.dsl=2
include { basename; sub; length; range; toChannel; getDefault; read_int; numerate; select_first } from './biouml_function.nf'
params.num_files = 10
params.index = 2

process generate_files {

  input :
  val num_files


  publishDir "generate_files", mode: 'copy'

  script:
  """
  mkdir -p output
        for i in \$(seq 1 ${num_files}); do
            echo "This is file \$i" > output/file_\${i}.txt
        done
  """

  output:
  path "output/file_*.txt", emit: files 
}

process process_file {

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
  num_files
  index



  main:
  result_generate_files = generate_files( num_files )
  result_process_file = process_file( result_generate_files.files.collect().map{v->v[index]}, "res_2" )

  emit: 
  processed_file = result_process_file.output_file
}

workflow {
mainWorkflow( params.num_files, params.index  )
}
