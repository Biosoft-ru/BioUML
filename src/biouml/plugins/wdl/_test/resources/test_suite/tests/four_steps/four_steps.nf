nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
process step1 {

  fair true
  publishDir "step1", mode: 'copy'

  output:
  path "step1.txt", emit: out_file 

  script:
  """
  echo "Step 1 output" > step1.txt
  """
}

process step2 {

  fair true
  publishDir "step2", mode: 'copy'

  input:
  path input_file

  output:
  path "step2.txt", emit: out_file 

  script:
  """
  cat ${input_file} > step2.txt
    echo "Step 2 output" >> step2.txt
  """
}

process step3 {

  fair true
  publishDir "step3", mode: 'copy'

  input:
  path input_file

  output:
  path "step3.txt", emit: out_file 

  script:
  """
  cat ${input_file} > step3.txt
    echo "Step 3 output" >> step3.txt
  """
}

process step4 {

  fair true
  publishDir "step4", mode: 'copy'

  input:
  path input_file

  output:
  path "step4.txt", emit: out_file 

  script:
  """
  cat ${input_file} > step4.txt
    echo "Step 4 output" >> step4.txt
  """
}

workflow mainWorkflow {

  main:
  result_step1 = step1( )

  result_step2 = step2( result_step1.out_file )

  result_step3 = step3( result_step2.out_file )

  result_step4 = step4( result_step3.out_file )

  emit: 
  final_output = result_step4.out_file
}

workflow {
mainWorkflow(  )
}
