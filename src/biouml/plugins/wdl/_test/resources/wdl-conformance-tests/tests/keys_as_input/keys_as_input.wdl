version 1.1

workflow keysWorkflow {
  input {
    Map[String, Int] in_map
  }

  call copy_output {input: in_array=keys(in_map)}

  output {
    File the_output = copy_output.the_output
  }
}

task copy_output {
  input {
    Array[String] in_array
  }

  command {
    cp ~{write_json(in_array)} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}

