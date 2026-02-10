nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll;  length } from './biouml_function.nf'
workflow mainWorkflow {

  main:
  new_experiment_2 = [ "id": "exp_002", "variables": ["pH","concentration","absorbance"], "data": [ "name": 42.0, "pH": 7.4, "concentration": 0.5 ] ]

  new_experiment_1 = [ "id": "exp_001", "variables": ["temperature","pressure","volume"], "data": [ "temperature": 37.0, "name": 85.5, "pressure": 101.3 ] ]

  new_experiment_3 = [ "id": "exp_003", "variables": ["mass","density","viscosity"], "data": [ "density": 1.05, "name": 100.0, "mass": 250.5 ] ]

  all_experiments = [new_experiment_1,new_experiment_2,new_experiment_3]

  first_experiment = get(all_experiments, 0)

  emit: 
  experiments = all_experiments
  first_var = get(first_experiment.variables, 0)
  first_var_from_first_experiment = all_experiments[0].variables[0]
  second_experiment_id = new_experiment_2.id
  subject_name = first_experiment.data["name"]
  subject_name_from_first_experiment = all_experiments[0].data["name"]
  total_experiment_count = length(all_experiments)
}

workflow {
mainWorkflow(  )
}
