version 1.1
workflow readTsvWorkflow {
  # this workflow depends on stdout() and write_lines()
  input {
    File in_file
  }

  call read_tsv {input: in_file=in_file}
  call copy_output {input: in_tsv=read_tsv.out_tsv}
  
  output {
    File the_output = copy_output.the_output
    Array[Array[String]] out_tsv = read_tsv.out_tsv
  }
}

task read_tsv {
  input {
    File in_file
  }

  command {
    cat ${in_file}
  }

  output {
    Array[Array[String]] out_tsv = read_tsv(stdout())
  }
}

task copy_output {
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
