version 1.1
workflow readIntWorkflow {
  input {
    File in_file
  }

  call read_int {input: in_file=in_file}

  output {
    File the_output = read_int.the_output
  }
}

task read_int {
  input {
    File in_file
  }

  command {
    echo "${read_int(in_file)}" > output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
