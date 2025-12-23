nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.flag

process write_false_file {

  fair true
  publishDir "write_false_file", mode: 'copy'

  output:
  path "false.txt", emit: out 

  script:
  """
  echo "This is false" > false.txt
  """
}

process write_true_file {

  fair true
  publishDir "write_true_file", mode: 'copy'

  output:
  path "true.txt", emit: out 

  script:
  """
  echo "This is true" > true.txt
  """
}

workflow mainWorkflow {

  take:
  flag

  main:

if ( !flag ) {
  result_write_false_file = write_false_file( )

}

if ( flag ) {
  result_write_true_file = write_true_file( )

}
  emit: 
  result = ((flag)) ? result_write_true_file.out : result_write_false_file.out
}

workflow {
mainWorkflow( params.flag  )
}
