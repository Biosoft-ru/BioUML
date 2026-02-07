nextflow.enable.dsl=2
include { basename; sub; length; range; toChannel; getDefault; read_int; numerate; select_first } from './biouml_function.nf'
params.i = 4
params.j = 8

process generate_file_a {

  fair true

  input :
  val index


  publishDir "generate_file_a", mode: 'copy'

  script:
  """
  echo "File A: ${index}" > output_a_${index}.txt
  """

  output:
  path "output_a_${index}.txt", emit: outFile 
}

process generate_file_b {

  fair true

  input :
  val index


  publishDir "generate_file_b", mode: 'copy'

  script:
  """
  echo "File B: ${index}" > output_b_${index}.txt
  """

  output:
  path "output_b_${index}.txt", emit: outFile 
}

process process_files {

  fair true

  input :
  path fileA
  path fileB


  publishDir "process_files", mode: 'copy'

  script:
  """
  cat ${fileA} ${fileB} > combined_${basename(fileA)}_${basename(fileB)}.txt
  """

  output:
  path "combined_${basename(fileA)}_${basename(fileB)}.txt", emit: combinedFile 
}

workflow mainWorkflow {
  take:
  i
  j



  main:
  result_generate_file_b = generate_file_b( j )

  result_generate_file_a = generate_file_a( i )

  result_process_files = process_files( result_generate_file_a.outFile, result_generate_file_b.outFile )

  emit: 
  results = result_process_files.combinedFile
}
