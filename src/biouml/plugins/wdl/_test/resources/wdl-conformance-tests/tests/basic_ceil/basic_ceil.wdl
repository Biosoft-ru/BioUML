version 1.0

workflow ceilWorkflow {
  input { 
    Float num
  }
  output {
    Int int_output = ceil(num)
  }
}