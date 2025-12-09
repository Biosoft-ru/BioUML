nextflow.enable.dsl=2
include { basename; sub; length; range; createChannelIfNeeded; getDefault; read_int; numerate } from './biouml_function.nf'
params.x = [1,2,3,4]

workflow mainWorkflow {
  take:
  x



  main:
  x_mapping = x.multiMap {element -> 
    def incremented = element+1
    def z = incremented*2

    incremented: incremented
    z: z
  }
  incremented = x_mapping.incremented
  z = x_mapping.z

  result = z

  emit: 
  result_values = result
  z_values = z
}

workflow {
mainWorkflow( createChannelIfNeeded(params.x).flatten()  )
}