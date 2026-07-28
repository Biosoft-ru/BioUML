version 1.1
workflow readJsonWorkflow {
  # this workflow depends on stdout() and write_lines()
  input {
    File in_file
  }

  call read_json {input: in_file=in_file}
  call copy_output {input: in_json=read_json.out_json}

  output {
    File the_output = copy_output.the_output
    Map[String, String] out_json = read_json.out_json
  }
}

task read_json {
  input {
    File in_file
  }

  command {
    cat ${in_file}
  }

  output {
    Map[String, String] out_json = read_json(stdout())
  }
}

task copy_output {
  input {
    Map[String, String] in_json
  }

  command {
    cp ${write_json(in_json)} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
