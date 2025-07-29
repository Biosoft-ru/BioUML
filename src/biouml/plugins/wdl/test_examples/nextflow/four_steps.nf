nextflow.enable.dsl=2
include { basename; sub; length; range } from './biouml_function.nf'
process step1 {

  publishDir "step1", mode: 'copy'

  script:
  """
  echo "Step 1 output" > step1.txt
  """

  output:
  path "step1.txt", emit: out_file 
}

process step2 {

  input :
  path input_file


  publishDir "step2", mode: 'copy'

  script:
  """
  cat ${input_file} > step2.txt
    echo "Step 2 output" >> step2.txt
  """

  output:
  path "step2.txt", emit: out_file 
}

process step3 {

  input :
  path input_file


  publishDir "step3", mode: 'copy'

  script:
  """
  cat ${input_file} > step3.txt
    echo "Step 3 output" >> step3.txt
  """

  output:
  path "step3.txt", emit: out_file 
}

process step4 {

  input :
  path input_file


  publishDir "step4", mode: 'copy'

  script:
  """
  cat ${input_file} > step4.txt
    echo "Step 4 output" >> step4.txt
  """

  output:
  path "step4.txt", emit: out_file 
}

workflow four_steps {

  main:
  result_step1 = step1( )

  result_step2 = step2( result_step1.out_file )

  result_step3 = step3( result_step2.out_file )

  result_step4 = step4( result_step3.out_file )

  emit: 
  final_output = result_step4.out_file
}

workflow {
four_steps(  )
}