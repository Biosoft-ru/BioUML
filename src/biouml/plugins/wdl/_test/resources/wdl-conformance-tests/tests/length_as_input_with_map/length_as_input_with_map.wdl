version 1.1
workflow lengthWorkflow {
  # this workflow should throw an error. length() does not work with Map[X, Y].
  input {
    Map[String, String] in_map
  }
  call copy_output {input: num=length(in_map)}

  output {
    File the_output = copy_output.the_output
  }
}

task copy_output {
  input {
    Int num
  }

  command {
    echo ${num} > output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
