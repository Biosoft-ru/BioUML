nextflow.enable.dsl=2
include { get; basename; sub; length; range; toChannel; getDefault; read_int; read_string; read_float; numerate; select_first; select_all } from './biouml_function.nf'
params.input_file
params.numbers

process GenerateFile {

  fair true

  input :
  val count

  container '"ubuntu:20.04"'

  publishDir "GenerateFile", mode: 'copy'

  script:
  """
  echo "Generating file with ${count} lines"
        for i in \$(seq 1 ${count}); do
            echo "Line \$i" >> generated.txt
        done
  """

  output:
  path "generated.txt", emit: output_file 
}

process ProcessFiles {

  fair true

  input :
  path files

  container '"ubuntu:20.04"'

  publishDir "ProcessFiles", mode: 'copy'

  script:
  files_str = files.join(' ')
  """
  echo "Processing files..."
        cat ${files_str} > combined.txt
        wc -l combined.txt > result.txt
  """

  output:
  path "result.txt", emit: result 
}

workflow mainWorkflow {
  take:
  input_file
  numbers



  main:

result_GenerateFile = GenerateFile(toChannel(numbers))
 file_array = result_GenerateFile.output_file.map { output_file -> [input_file,output_file] }
result_ProcessFiles = ProcessFiles(file_array)
  emit: 
  final_output = result_ProcessFiles.result
}

workflow {
mainWorkflow( file(params.input_file), params.numbers  )
}