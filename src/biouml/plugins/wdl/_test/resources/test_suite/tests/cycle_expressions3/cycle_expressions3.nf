nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll;  select_first } from './biouml_function.nf'
params.x

workflow mainWorkflow {

  take:
  x

  main:

 incremented = toChannel(x).map { element -> element+1 }
z_conditional = incremented.map { incremented-> ( incremented>2 ) ? incremented*2: null }.filter { it != null }
 z1 = incremented.merge( z_conditional ).map { incremented,z_conditional -> select_first([z_conditional,incremented]) }

 decremented = toChannel(x).map { element -> element-1 }
z_conditional2 = decremented.map { decremented-> ( decremented<2 ) ? decremented*2: null }.filter { it != null }
 z2 = decremented.merge( z_conditional2 ).map { decremented,z_conditional2 -> select_first([z_conditional2,decremented]) }
  emit: 
  z_values1 = z1
  z_values2 = z2
}

workflow {
mainWorkflow( params.x  )
}
