nextflow.enable.dsl=2
include { basename; sub; length; range; getDefault; read_int; numerate } from './biouml_function.nf'
params.x = [1,2,3]

def createChannelIfNeeded(arr)
{
    if (arr instanceof java.util.List)
        return Channel.of(arr).flatten()
     else 
        return arr
}

def select_first(arr)
{
	return arr.find { it != null && it != '' }
}

workflow mainWorkflow {
  take:
  x

  main:
  x_mapping = x.multiMap {element -> 
    incremented = element+1

    def z_conditional = null

	if ( incremented>2 ) {
         z_conditional = incremented*2
    }
    z = select_first([z_conditional,incremented])

    incremented: incremented
    z: z
	z_conditional: z_conditional
  }
  incremented = x_mapping.incremented
  z = x_mapping.z
  z_conditional = x_mapping.z_conditional

z.view()
  emit: 
  z_values = z
}

workflow {
mainWorkflow( createChannelIfNeeded(params.x).flatten()  )
}

