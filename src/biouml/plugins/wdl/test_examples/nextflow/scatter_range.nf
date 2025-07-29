nextflow.enable.dsl=2
include { basename; sub; length; range } from './biouml_function.nf'
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

workflow scatter_range {
  take:
  len

  main:
  expression = range(len)
  input_value_ch = Channel.from(expression)
  result_square = square( input_value_ch )

}

workflow {
scatter_range( params.len  )
}