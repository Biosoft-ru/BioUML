nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll; basename } from './biouml_function.nf'
params.i = 4
params.j = 8

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
  i
  j

  main:
  result_generate_file_a = generate_file_a( i )

  result_generate_file_b = generate_file_b( j )

  x = [result_generate_file_a.outFile,result_generate_file_b.outFile]

  result_process_files = process_files( result_generate_file_a.outFile, result_generate_file_b.outFile )

  emit: 
  results = result_process_files.combinedFile
}

workflow {
mainWorkflow( params.i, params.j  )
}
