version 1.1

workflow asPairsWorkflow {
  input {
    Map[String, Int] in_map
  }

  call copy_output {input: in_pairs=as_pairs(in_map)}

  output {
    File the_output = copy_output.the_output
  }
}

task copy_output {
  input {
    Array[Pair[String, Int]] in_pairs
  }

  command {
    cp ~{write_json(in_pairs)} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
