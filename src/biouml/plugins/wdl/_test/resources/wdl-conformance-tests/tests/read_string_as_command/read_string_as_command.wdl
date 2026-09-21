version 1.1
workflow readStringWorkflow {
  input {
    File in_file
  }

  call read_string {input: in_file=in_file}
  output {
    File the_output = read_string.the_output
  }
}

task read_string {
  input {
    File in_file
  }

  command {
    echo "${read_string(in_file)}" > output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
