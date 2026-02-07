nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.name_array = ["Joe","Bob","Fred"]
params.salutation = "Hello"

process say_hello {

  fair true
  publishDir "say_hello", mode: 'copy'

  input:
  val greeting
  val name

  script:
  """
  printf "${greeting}, ${name} how are you?"
  """
}

workflow mainWorkflow {

  take:
  name_array
  salutation

  main:

  say_hello_input_name = toChannel( name_array )  
result_say_hello = say_hello( salutation, say_hello_input_name )

}

workflow {
mainWorkflow( params.name_array, params.salutation  )
}
