version 1.1
workflow subWorkflow {
  input {
    File in_file
  }
  call get_sub {input: in_file=in_file}

  output {
    File the_output = get_sub.the_output
  }
}

task get_sub {
  input {
    File in_file
  }
  String out_file_name = sub(in_file, "\\.tsv$", ".csv")

  command {
    echo "${out_file_name}" > output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
