version 1.1

workflow overrideWorkflow {
  input {
    Int? value
  }
  
  call requiredInputTask {
    input:
      # We send our optional value as an override. If our optional value is
      # itself unset, do we see the null somehow as the Int inside the task, or
      # do we see the default value the task provides, or do we fail to
      # typecheck?
      value_in = value
  }
  
  output {
    Int result = requiredInputTask.value_out
  }
}

task requiredInputTask {
  input {
    Int value_in = 1
  }
  # Command section is required
  command {
  }
  output {
    Int value_out = value_in
  }
}
