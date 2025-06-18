version 1.1

task hello_task {
  input {
    File infile
    String pattern
  }

  command <<<
    grep -E '~{pattern}' '~{infile}'
  >>>

  runtime {
    container: "ubuntu:latest"
  }

  output {
    Array[String] matches = read_lines(stdout())
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
    Array[String] matches = hello_task.matches
  }
}