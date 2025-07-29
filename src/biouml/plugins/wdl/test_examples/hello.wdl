version 1.1

task hello_task {
  input {
    File infile
    String pattern
  }

  command {
    grep -E '~{pattern}' '~{infile}' > result.txt
  }

  runtime {
    container: "ubuntu:latest"
  }

  output {
    File result = "result.txt"
  }
}

workflow hello {
  input {
    File infile
    String pattern
  }

  call hello_task {
    input: infile, pattern
  }

  output {
    File result = hello_task.result
  }
}