nextflow.enable.dsl=2

params.do_scatter = true
params.scatter_range = [1,2,3,4,5]

process gt_three {

  input:
  val i


  publishDir "gt_three", mode: 'copy'

  script:
  """
  
  """

  output:
  val i, emit: valid 
}

workflow mainWorkflow {
  take:
  do_scatter
  scatter_range



  main:
if ( do_scatter ) {
  j = 2
  i = Channel.from(scatter_range)
  result_gt_three = gt_three( i )

if ( result_gt_three.valid > 3) { result = i*j }
 
  result2 = (defined(result)) ? select_first([result]) : 0
}
  maybe_results = select_first([result,[]])

  emit: 
  j_out = j
  maybe_result2 = result2
  result_array = select_all(maybe_results)
}

workflow {
mainWorkflow( params.do_scatter, params.scatter_range  )
}
