nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll;  length;  range } from './biouml_function.nf'
params.arr = [1,4,2]
params.constant = 3

process square {

  fair true
  publishDir "square", mode: 'copy'

  input:
  val input_value
  val other_input

  script:
  """
  echo ${input_value} * ${other_input} > result.txt
  """
}

workflow mainWorkflow {

  take:
  arr
  constant

  main:
  expression = range(length(arr))


  square_input_input_value = toChannel(arr)  
result_square = square( square_input_input_value, constant )

}

workflow {
mainWorkflow( params.arr, params.constant  )
}
