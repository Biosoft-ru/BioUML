nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.sample_id = "sample_001"

process CreateObject {

  fair true
  publishDir "CreateObject", mode: 'copy'

  input:
  val sample_name

  output:
  val [
name:"my name",
score:10,
value:32.2
], emit: my_object 

  script:
  """
  
  """
}

workflow mainWorkflow {

  take:
  sample_id

  main:
  result_CreateObject = CreateObject( sample_id )

  emit: 
  result = result_CreateObject.my_object
}

workflow {
mainWorkflow( params.sample_id  )
}
