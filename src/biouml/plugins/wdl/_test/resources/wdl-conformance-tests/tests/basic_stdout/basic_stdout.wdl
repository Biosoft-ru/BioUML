version 1.0

workflow stdoutWorkflow {
  input {
    String message
  }
  call get_stdout { input: message=message }
  output {
    File check_this = get_stdout.check_this
  }
}

task get_stdout {
  input {
    String message
  }

  command {
    echo '${message}'
  }

 output {
    File check_this = stdout()
 }
}
