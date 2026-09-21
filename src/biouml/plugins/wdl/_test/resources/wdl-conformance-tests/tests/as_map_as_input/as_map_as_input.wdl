version 1.1

workflow asMapWorkflow {
  input {
    Array[Pair[String, Int]] in_array
  }

  Map[String, Int] a_map = as_map(in_array)

  call copy_output {
    input:
      in_map=a_map
  }

  output {
    File the_output = copy_output.the_output
  }
}

task copy_output {
  input {
    Map[String, Int] in_map
  }

  command {
    cp ~{write_json(in_map)} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
