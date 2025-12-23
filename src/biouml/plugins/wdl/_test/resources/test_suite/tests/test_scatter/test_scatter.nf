nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.name_array = ["Joe","Bob","Fred"]
params.salutation = "Hello"

process read_file {

  fair true
  publishDir "read_file", mode: 'copy'

  input:
  path file

  output:
  path "result.txt", emit: result 

  script:
  """
  echo "${greeting}, how are you?" > result.txt
  """
}

process say_hello {

  fair true
  publishDir "say_hello", mode: 'copy'

  input:
  val greeting

  output:
  path "result.txt", emit: result 

  script:
  """
  echo "${greeting}, how are you?" > result.txt
  """
}

workflow mainWorkflow {

  take:
  name_array
  salutation

  main:

 greeting = toChannel(name_array).map { name -> "${salutation} ${name}" }
  say_hello_input_greeting = toChannel( greeting )  
result_say_hello = say_hello( say_hello_input_greeting )

  emit: 
  messages = result_say_hello.result
}

workflow {
mainWorkflow( params.name_array, params.salutation  )
}
