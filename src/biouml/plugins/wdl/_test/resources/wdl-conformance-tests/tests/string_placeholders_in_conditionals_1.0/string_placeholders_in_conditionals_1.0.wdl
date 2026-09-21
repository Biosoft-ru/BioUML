version 1.0

# This is not actually allowed in WDL 1.0, but Cromwell sort of supports it and at least will parse the workflow.

workflow placeholderWorkflow {
  input { 
  }
  
  Array[String] things = ["1", "2"]
  
  if ("${sep="" things}" == '["1", "2"]'){
    String inside_result0 = "cromwell"
  }
  if ("${sep="" things}" == "12"){
    String inside_result1 = "yep"
  }
  if ("${sep="" things}" != "12"){
    String inside_result2 = "nope"
  }
  
  
  output {
    String result = select_first([inside_result0, inside_result1, inside_result2])
  }
}

