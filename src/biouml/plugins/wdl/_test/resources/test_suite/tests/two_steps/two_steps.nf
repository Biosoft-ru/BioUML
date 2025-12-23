nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.person_name = "Alice"

process ask_how_are_you {

  fair true
  publishDir "ask_how_are_you", mode: 'copy'

  input:
  path greeting_file

  output:
  path "message.txt", emit: message_file 

  script:
  """
  # Read greeting, append question, write to message.txt
    cat ${greeting_file} > message.txt
    echo " How are you?" >> message.txt
  """
}

process say_hello {

  fair true
  publishDir "say_hello", mode: 'copy'

  input:
  val name

  output:
  path "greeting.txt", emit: greeting_file 

  script:
  """
  echo "Hello, ${name}!" > greeting.txt
  """
}

workflow mainWorkflow {

  take:
  person_name

  main:
  result_say_hello = say_hello( person_name )

  result_ask_how_are_you = ask_how_are_you( result_say_hello.greeting_file )

  emit: 
  final_message = result_ask_how_are_you.message_file
}

workflow {
mainWorkflow( params.person_name  )
}
