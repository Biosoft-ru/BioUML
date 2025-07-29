nextflow.enable.dsl=2
include { basename; sub; length; range } from './biouml_function.nf'
params.infile
params.pattern

process hello_task {

  input :
  path infile
  val pattern


  publishDir "hello_task", mode: 'copy'

  script:
  """
  grep -E '${pattern}' '${infile}' > result.txt
  """

  output:
  path "result.txt", emit: result 
}

workflow hello {
  take:
  infile
  pattern

  main:
  result_hello_task = hello_task( infile, pattern )

  emit: 
  result = result_hello_task.result
}

workflow {
hello( params.infile, params.pattern  )
}