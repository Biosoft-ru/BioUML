nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.name = "sample_001"
params.quality_score = 95
params.coverage = 42.5

workflow mainWorkflow {

  take:
  name
  quality_score
  coverage

  main:
  my_object = [
sample_name:name,
quality:quality_score,
avg_coverage:coverage
]

  emit: 
  result_object = my_object
}

workflow {
mainWorkflow( params.name, params.quality_score, params.coverage  )
}
