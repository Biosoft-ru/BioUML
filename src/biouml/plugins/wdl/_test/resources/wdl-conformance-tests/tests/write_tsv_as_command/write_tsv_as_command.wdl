version 1.1
workflow writeTsvWorkflow {
  input {
    Array[Array[String]] in_tsv
  }

  call write_tsv {input: in_tsv=in_tsv}

  output {
    File the_output = write_tsv.the_output
  }
}

task write_tsv {
  input {
    Array[Array[String]] in_tsv
  }

  command {
    cp ${write_tsv(in_tsv)} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
