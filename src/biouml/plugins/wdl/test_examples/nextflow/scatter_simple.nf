nextflow.enable.dsl=2
include { basename; sub; length; range } from './biouml_function.nf'
params.name_array = ["Joe","Bob","Fred"]
params.salutation = "Hello"

process say_hello {

  input :
  val greeting
  val name


  publishDir "say_hello", mode: 'copy'

  script:
  """
  printf "${greeting}, ${name} how are you?"
  """
}

workflow scatter_simple {
  take:
  name_array
  salutation

  main:
  name_ch = Channel.from(name_array)
  result_say_hello = say_hello( salutation, name_ch )

}

workflow {
scatter_simple( params.name_array, params.salutation  )
}