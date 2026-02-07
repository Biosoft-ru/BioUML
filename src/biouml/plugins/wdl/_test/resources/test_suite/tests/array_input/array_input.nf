nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.num_files

process generate_files {

  fair true
  publishDir "generate_files", mode: 'copy'

  input:
  val num_files

  output:
  path "output/file_*.txt", emit: files 

  script:
  """
  set -e
        mkdir -p output
        i=1
        while [ \$i -le ${num_files} ]; do
            echo "This is file \$i" > output/file_\${i}.txt
            i=\$((i + 1))
        done
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

  take:
  num_files

  main:
  result_generate_files = generate_files( num_files )

  result_process_files = process_files( result_generate_files.files, "res_all.txt" )

  emit: 
  processed_file = result_process_files.output_file
}

workflow {
mainWorkflow( params.num_files  )
}
