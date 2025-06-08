version 1.1

task test_hints {
  input {
    File foo
  }

  command <<<
  wc -l ~{foo}
  >>>

  output {
    Int num_lines = read_int(stdout())
  }

  runtime {
    container: "ubuntu:latest"
    maxMemory: "36 GB"
    maxCpu: 24
    shortTask: true
    localizationOptional: false
    inputs: object {
      foo: object { 
        localizationOptional: true
      }
    }
  }
}