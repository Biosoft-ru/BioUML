nextflow.enable.dsl=2
include { basename; sub; length; range; toChannel; getDefault; read_int; numerate; select_first } from './biouml_function.nf'
params.my_experiments

workflow mainWorkflow {
  take:
  my_experiments



  main:
  first_experiment = toChannel(my_experiments).collect().map{v->v[0]}

first_experiment.view()
  emit: 
  first_var = toChannel(first_experiment.variables).collect().map{v->v[0]}
  first_var_from_first_experiment = my_experiments[0].variables[0]
  subject_name = first_experiment.data["name"]
  subject_name_from_first_experiment = my_experiments[0].data["name"]
}

workflow {
mainWorkflow( params.my_experiments  )
}