nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.numbers
params.factor

process write_number {

  fair true
  publishDir "write_number", mode: 'copy'

  input:
  val number

  output:
  path "output.txt", emit: output_file 

  script:
  """
  echo "${number}" > output.txt
  """
}

workflow mainWorkflow {

  take:
  numbers
  factor

  main:

 multiplied_number = toChannel(numbers).map { num -> num*factor }
  write_number_input_number = toChannel( multiplied_number )  
result_write_number = write_number( write_number_input_number )

  result_files = result_write_number.output_file

}

workflow {
mainWorkflow( params.numbers, params.factor  )
}
