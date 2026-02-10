nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
process sayHello {

  fair true
  publishDir "sayHello", mode: 'copy'

  output:
  path "hello.txt", emit: result 

  script:
  """
  echo "Hello World" > hello.txt
  """
}

workflow mainWorkflow {

  main:
  result_sayHello = sayHello( )

  emit: 
  result = result_sayHello.result
}

workflow {
mainWorkflow(  )
}
