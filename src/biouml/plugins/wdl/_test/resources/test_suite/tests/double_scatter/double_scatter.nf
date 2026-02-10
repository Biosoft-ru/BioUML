nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll;  range } from './biouml_function.nf'
process add_two_ints {

  fair true
  publishDir "add_two_ints", mode: 'copy'

  input:
  val a
  val b
  val c

  output:
  path "result${c}.txt", emit: res 

  script:
  """
  echo \$((${a} + ${b}+ ${c})) > result${c}.txt
  """
}

workflow mainWorkflow {

  main:
  outer_range = range(3)

  inner_range = range(2)


 k = toChannel(outer_range).combine(toChannel(inner_range)).map { i,j -> i*j }
  add_two_ints_input_a = combineAll( [ outer_range, inner_range ] ).map { i, j -> i+1 }
  add_two_ints_input_b = combineAll( [ outer_range, inner_range ] ).map { i, j -> j }
  add_two_ints_input_c = combineAll( [ outer_range, k ] ).map { i, k -> k }  
result_add_two_ints = add_two_ints( add_two_ints_input_a, add_two_ints_input_b, add_two_ints_input_c )

}

workflow {
mainWorkflow(  )
}
