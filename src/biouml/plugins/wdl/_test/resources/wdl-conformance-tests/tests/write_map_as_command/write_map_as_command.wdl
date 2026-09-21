version 1.1
workflow writeMapWorkflow {
  input {
    Map[String, String] in_map
  }

  call write_map {input: in_map=in_map}

  output {
    File the_output = write_map.the_output
  }
}

task write_map {
  input {
    Map[String, String] in_map
  }

  command {
    cp ${write_map(in_map)} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
