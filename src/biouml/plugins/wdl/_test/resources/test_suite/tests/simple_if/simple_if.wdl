version 1.0

task write_true_file {
  command {
    echo "This is true" > true.txt
  }

  output {
    File out = "true.txt"
  }
}

task write_false_file {
  command {
    echo "This is false" > false.txt
  }

  output {
    File out = "false.txt"
  }
}

workflow conditional_run {
  input {
    Boolean flag
  }

  if (flag) {
    call write_true_file
  } 

 if (!flag) {
    call write_false_file
  }

  output {
    File? result = if (flag) then write_true_file.out else write_false_file.out
  }
  
  meta {
    title: "Simple if"
    description: "Workflow with two tasks, input flag decides which task will be executed."
  }
}