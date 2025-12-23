nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.x

workflow mainWorkflow {

  take:
  x

  main:

 incremented = toChannel(x).map { element -> element+1 }
 z = incremented.map { incremented -> incremented*2 }
  result = z

  emit: 
  result_values = result
  z_values = z
}

workflow {
mainWorkflow( params.x  )
}
