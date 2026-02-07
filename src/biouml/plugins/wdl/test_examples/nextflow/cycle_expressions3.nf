nextflow.enable.dsl=2
include { basename; sub; length; range; createChannelIfNeeded; getDefault; read_int; numerate; select_first } from './biouml_function.nf'
params.x

workflow mainWorkflow {
  take:
  x



  main:
  x_mapping = x.multiMap {element -> 
    def incremented = element+1
    def     z_conditional = null
    if (incremented>2) {
    z_conditional = incremented*2
    }
    def z = select_first([z_conditional,incremented])

    incremented: incremented
    z_conditional: z_conditional
    z: z
  }

  incremented = x_mapping.incremented
  z_conditional = x_mapping.z_conditional
  z = x_mapping.z
  x_mapping = x.multiMap {element -> 
    def decremented = element-1
    def z1 = select_first([z_conditional,incremented])
    def     z_conditional2 = null
    if (decremented<2) {
    z_conditional2 = decremented*2
    }
    def z2 = select_first([z_conditional,decremented])

    decremented: decremented
    z1: z1
    z_conditional2: z_conditional2
    z2: z2
  }

  decremented = x_mapping.decremented
  z1 = x_mapping.z1
  z_conditional2 = x_mapping.z_conditional2
  z2 = x_mapping.z2
  emit: 
  z_values = z2
}

workflow {
mainWorkflow( createChannelIfNeeded(params.x).flatten()  )
}