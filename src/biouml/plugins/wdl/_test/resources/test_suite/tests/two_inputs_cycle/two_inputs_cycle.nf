nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll; basename;  length;  range } from './biouml_function.nf'
params.arrayA = [1,2,3]
params.arrayB = [4,5,6]

process generate_file_a {

  fair true
  publishDir "generate_file_a", mode: 'copy'

  input:
  val index

  output:
  path "output_a_${index}.txt", emit: outFile 

  script:
  """
  echo "File A: ${index}" > output_a_${index}.txt
  """
}

process generate_file_b {

  fair true
  publishDir "generate_file_b", mode: 'copy'

  input:
  val index

  output:
  path "output_b_${index}.txt", emit: outFile 

  script:
  """
  echo "File B: ${index}" > output_b_${index}.txt
  """
}

process process_files {

  fair true
  publishDir "process_files", mode: 'copy'

  input:
  path fileA
  path fileB

  output:
  path "combined_${basename(fileA)}_${basename(fileB)}.txt", emit: combinedFile 

  script:
  """
  cat ${fileA} ${fileB} > combined_${basename(fileA)}_${basename(fileB)}.txt
  """
}

workflow mainWorkflow {

  take:
  arrayA
  arrayB

  main:
  expression = range(length(arrayA))

  expression_1 = range(length(arrayB))


  generate_file_b_input_index = toChannel(arrayB)  
result_generate_file_b = generate_file_b( generate_file_b_input_index )

  expression_2 = range(length(arrayA))


  generate_file_a_input_index = toChannel(arrayA)  
result_generate_file_a = generate_file_a( generate_file_a_input_index )


  process_files_input_fileA = result_generate_file_a.outFile
  process_files_input_fileB = result_generate_file_b.outFile  
result_process_files = process_files( process_files_input_fileA, process_files_input_fileB )

  emit: 
  results = result_process_files.combinedFile
}

workflow {
mainWorkflow( params.arrayA, params.arrayB  )
}
