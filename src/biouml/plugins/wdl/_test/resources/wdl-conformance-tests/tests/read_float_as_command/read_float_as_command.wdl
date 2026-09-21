version 1.1
workflow readFloatWorkflow {
  input {
    File in_file
  }

  call read_float {input: in_file=in_file}

  output {
    File the_output = read_float.the_output
  }
}

task read_float {
  input {
    File in_file
  }

  command {
    echo "${read_float(in_file)}" > output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
