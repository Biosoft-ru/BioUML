nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll;  select_first } from './biouml_function.nf'
params.x

workflow mainWorkflow {

  take:
  x

  main:

 incremented = toChannel(x).map { element -> element+1 }
z_conditional = incremented.map { incremented-> ( incremented>2 ) ? incremented*2: null }.filter { it != null }
 z = incremented.merge( z_conditional ).map { incremented,z_conditional -> select_first([z_conditional,incremented]) }
  emit: 
  z_values = z
}

workflow {
mainWorkflow( params.x  )
}
