nextflow.enable.dsl=2
params.flag

process write_false_file {

  publishDir "write_false_file", mode: 'copy'

  script:
  """
  echo "This is false" > false.txt
  """

  output:
  path "false.txt", emit: out 
}

process write_true_file {

  publishDir "write_true_file", mode: 'copy'

  script:
  """
  echo "This is true" > true.txt
  """

  output:
  path "true.txt", emit: out 
}

workflow mainWorkflow {
  take:
  flag



  main:

if ( flag ) {
  result_write_true_file = write_true_file( )

}

if ( !flag ) {
  result_write_false_file = write_false_file( )

}
  emit: 
  result = ((flag)) ? write_true_file.out : write_false_file.out
}

workflow {
mainWorkflow( params.flag  )
}