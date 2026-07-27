version 1.1
workflow readBooleanWorkflow {
  input {
    File in_file
  }

  call read_boolean {input: in_file=in_file}

  output {
    File the_output = read_boolean.the_output
  }
}

task read_boolean {
  input {
    File in_file
  }

  command {
    echo "${read_boolean(in_file)}" > output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
