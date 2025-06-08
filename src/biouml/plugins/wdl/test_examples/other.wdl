version 1.1

task foobar {
  input {
    File infile
  }

  command <<<
  wc -l ~{infile}
  >>>

  output {
    Int results = read_int(stdout())
  }

  runtime {
    container: "ubuntu:latest"
  }
}

workflow other {
  input {
    Boolean b = false
    File? f
  }

  if (b && defined(f)) {
    call foobar { input: infile = select_first([f]) }
  }

  output {
    Int? results = foobar.results
  }
}