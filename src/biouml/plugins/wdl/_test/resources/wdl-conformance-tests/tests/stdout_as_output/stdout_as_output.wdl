version 1.1
workflow stdoutWorkflow {
  input {
    String message
  }
  call get_stdout { input: message=message }
  call copy_output { input: in_file=get_stdout.check_this }

  output {
    File the_output = copy_output.the_output
    File check_this = get_stdout.check_this
  }
}

task get_stdout {
  input {
    String message
  }

  command {
    echo "${message}"
  }

  output {
    File check_this = stdout()
 }
}

# comply with builtinTest
task copy_output {
  input {
    File in_file
  }

  command {
    cp ${in_file} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
