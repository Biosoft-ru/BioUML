nextflow.enable.dsl=2
include { toChannel; fileOrNull; get; toArray; range; getDefault; combineAll; pair; stringify_wdl; saveOutput; orNull } from 'genespace_function.nf'
process sayHello {

  publishDir "/sayHello", mode: 'copy', overwrite: true
stageInMode 'copy'

  output:
  path "hello.txt", emit: result

  script:
  """
  echo "Hello World" > hello.txt
  """
}

workflow mainWorkflow {

  main:
  sayHello( )

  emit: 
  result = sayHello.out.result
}

workflow {
}