nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.lines = ["A","B","C","D"]

process private_declaration {

  fair true
  publishDir "private_declaration", mode: 'copy'

  input:
  val line1
  val line2

  output:
  path "result.txt", emit: result 

  script:
  concatenator = "~"
  concatenated = line1+concatenator+line2
  """
  echo "${concatenated}" > result.txt
  """
}

workflow mainWorkflow {

  take:
  lines

  main:
  result_private_declaration = private_declaration( get(lines, 0), get(lines, 1) )

  emit: 
  concatenated_output = result_private_declaration.result
}

workflow {
mainWorkflow( params.lines  )
}
