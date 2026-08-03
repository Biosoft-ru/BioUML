version 1.0

workflow emptyOutputWorkflow {
  input {
    Int num
  }
  call task_with_output {input: num = num}
}

task task_with_output {
  input {
    Int num
  }

  command {

  }

  output {
    Int int_output = num
  }
}