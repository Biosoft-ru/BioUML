nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.numbers
params.factor

process write_number {

  fair true
  publishDir "write_number", mode: 'copy'

  input:
  val number1
  val number2

  output:
  path "output1.txt", emit: output_file 

  script:
  """
  echo "${number1} ${number2}" > output1.txt
  """
}

process write_number2 {

  fair true
  publishDir "write_number2", mode: 'copy'

  input:
  val number1
  val number2

  output:
  path "output2.txt", emit: output_file 

  script:
  """
  echo "${number1} ${number2}" > output2.txt
  """
}

workflow mainWorkflow {

  take:
  numbers
  factor

  main:

 multiplied_number = toChannel(numbers).map { num -> num*factor }
 divided = toChannel(numbers).map { num -> num/factor }
 divided2 = divided.map { divided -> divided/factor }
  write_number2_input_number1 = toChannel( divided )
  write_number2_input_number2 = toChannel( divided2 )  
result_write_number2 = write_number2( write_number2_input_number1, write_number2_input_number2 )

  write_number_input_number1 = toChannel( multiplied_number )
  write_number_input_number2 = toChannel( divided2 )  
result_write_number = write_number( write_number_input_number1, write_number_input_number2 )

  result_files = result_write_number.output_file

}

workflow {
mainWorkflow( params.numbers, params.factor  )
}
