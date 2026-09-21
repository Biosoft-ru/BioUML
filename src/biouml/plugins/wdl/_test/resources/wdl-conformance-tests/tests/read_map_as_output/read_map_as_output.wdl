version 1.1
workflow readMapWorkflow {
  # this workflow depends on stdout() and write_lines()
  input {
    File in_file
  }

  call read_map {input: in_file=in_file}
  call copy_output {input: in_map=read_map.out_map}

  output {
    File the_output = copy_output.the_output
    Map[String, String] out_map = read_map.out_map
  }
}

task read_map {
  input {
    File in_file
  }

  command {
    cat ${in_file}
  }

  output {
    Map[String, String] out_map = read_map(stdout())
  }
}

task copy_output {
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
