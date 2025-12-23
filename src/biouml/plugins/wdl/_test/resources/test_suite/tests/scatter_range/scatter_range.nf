nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll;  range } from './biouml_function.nf'
params.len = 3

process square {

  fair true
  publishDir "square", mode: 'copy'

  input:
  val input_value

  script:
  """
  echo ${input_value} * ${input_value}
  """
}

workflow mainWorkflow {

  take:
  len

  main:
  expression = range(len)


  square_input_input_value = toChannel( expression )  
result_square = square( square_input_input_value )

}

workflow {
mainWorkflow( params.len  )
}
