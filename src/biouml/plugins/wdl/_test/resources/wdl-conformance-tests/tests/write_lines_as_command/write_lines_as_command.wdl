version 1.1
workflow writeLinesWorkflow {
  input {
    Array[String] in_array
  }

  call write_lines {input: in_array=in_array}

  output {
    File the_output = write_lines.the_output
  }
}

task write_lines {
  input {
    Array[String] in_array
  }

  command {
    cp ${write_lines(in_array)} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
