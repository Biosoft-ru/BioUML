nextflow.enable.dsl=2
include { basename; sub; length; range } from './biouml_function.nf'
params.arr = [1,4,2]
params.constant = 3

process square {

  input :
  val input_value
  val other_input


  publishDir "square", mode: 'copy'

  script:
  """
  echo ${input_value} * ${other_input} > result.txt
  """
}

workflow scatter_range2 {
  take:
  arr
  constant

  main:
  expression = range(length(arr))
  input_value_ch = Channel.from(expression).map{idx -> arr[idx]}
  result_square = square( input_value_ch, constant )

}

workflow {
scatter_range2( params.arr, params.constant  )
}