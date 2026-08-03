version 1.1

workflow placeholderWorkflow {
  input { 
  }
  
  Array[String] things = ["1", "2"]
  
  if ("${sep="" things}" == "12"){
    String inside_result1 = "yep"
  }
  if ("${sep="" things}" != "12"){
    String inside_result2 = "nope"
  }
  
  
  output {
    String result = select_first([inside_result1, inside_result2])
  }
}
