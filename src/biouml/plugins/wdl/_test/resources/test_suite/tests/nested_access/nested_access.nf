nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
params.my_experiments

workflow mainWorkflow {

  take:
  my_experiments

  main:
  first_experiment = get(my_experiments, 0)

  emit: 
  first_var = get(first_experiment.variables, 0)
  first_var_from_first_experiment = my_experiments[0].variables[0]
  subject_name = first_experiment.data["name"]
  subject_name_from_first_experiment = my_experiments[0].data["name"]
}

workflow {
mainWorkflow( params.my_experiments  )
}
