nextflow.enable.dsl=2
include { basename; sub; length; range; toChannel; getDefault; read_int; numerate; select_first } from './biouml_function.nf'
params.len = 3

process square {

  input :
  val input_value


  publishDir "square", mode: 'copy'

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

  expression_mapping = expression.multiMap {idx -> 

    square_input: tuple(idx)
  }

  input_value = expression
  result_square = square( expression_mapping.square_input.map { it[0] } )

}

workflow {
mainWorkflow( params.len  )
}