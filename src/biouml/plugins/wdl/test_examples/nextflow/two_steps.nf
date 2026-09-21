nextflow.enable.dsl=2
include { basename; sub; length; range } from './biouml_function.nf'
params.person_name = "Alice"
params.question = "How are you?"

process ask_how_are_you {
  
  input :
  path greeting_file
  val question


  publishDir "ask_how_are_you", mode: 'copy'

  script:
  """
  cat ${greeting_file} > message.txt
    echo ${question} >> message.txt
  """

  output:
  path "message.txt", emit: message_file 
}

process say_hello {

  
  input :
  val name


  publishDir "say_hello", mode: 'copy'

  script:
  """
  echo "Hello, ${name}!" > greeting.txt
  """

  output:
  path "greeting.txt", emit: greeting_file 
}

workflow two_steps {
  take:
  person_name
  question

  main:
  result_say_hello = say_hello( person_name )

  result_ask_how_are_you = ask_how_are_you( result_say_hello.greeting_file, question )

  emit: 
  final_message = result_ask_how_are_you.message_file
}

workflow {
two_steps( params.person_name, params.question  )
}