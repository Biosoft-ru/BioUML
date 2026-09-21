version 1.1
workflow sizeWorkflow {
  input {
    File in_file
  }

  call get_size {input: in_file=in_file}

  output {
    File the_output = get_size.the_output
  }
}

task get_size {
  input {
    File in_file
  }

  command {
    echo "${size(in_file)}" > output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
