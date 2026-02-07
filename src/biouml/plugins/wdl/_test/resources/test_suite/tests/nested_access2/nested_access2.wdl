version 1.0

struct Experiment {
  String id
  Array[String] variables
  Map[String, Float] data
}

workflow nested_access {
  # Dynamically create new Experiment structs in the workflow
  
  Experiment new_experiment_1 = {"id": "exp_001", "variables": ["temperature", "pressure", "volume"], "data": {"name": 85.5, "temperature": 37.0, "pressure": 101.3}}
  
  Experiment new_experiment_2 = { "id": "exp_002", "variables": ["pH", "concentration", "absorbance"], "data": {"name": 42.0, "pH": 7.4, "concentration": 0.5}}
  
  Experiment new_experiment_3 = {"id": "exp_003", "variables": ["mass", "density", "viscosity"],"data": {"name": 100.0, "mass": 250.5, "density": 1.05}}
  
  # Create array of experiments
  Array[Experiment] all_experiments = [new_experiment_1, new_experiment_2, new_experiment_3]
  
  # Access the first experiment
  Experiment first_experiment = all_experiments[0]
  
  output {
    # Access nested data - these are equivalent
    String first_var = first_experiment.variables[0]
    String first_var_from_first_experiment = all_experiments[0].variables[0]

    # Access map data - these are equivalent
    String subject_name = first_experiment.data["name"]
    String subject_name_from_first_experiment = all_experiments[0].data["name"]
    
    # Additional outputs
    Array[Experiment] experiments = all_experiments
    Int total_experiment_count = length(all_experiments)
    String second_experiment_id = new_experiment_2.id
  }
  
  meta {
    title: "Structure 2"
    description: "Tests dynamic creation of structure."
  }
}