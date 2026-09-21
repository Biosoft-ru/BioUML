version 1.1

# Basic declaration example modified from the WDL 1.0 spec:
# https://github.com/openwdl/wdl/blob/main/versions/1.0/SPEC.md#declarations

task test {
  input {
    String var
  }
  command <<<
    echo "Hello, ~{var}!"
  >>>

  output {
    String value = read_string(stdout())
  }
}

task test2 {
  input {
    Array[String] array
  }

  command <<<
    echo "~{sep='; ' array}" > output.txt
  >>>

  output {
    File the_output = 'output.txt'
  }
}

workflow wf {
  call test as x {input: var="x"}
  call test as y {input: var="y"}
  Array[String] strs = [x.value, y.value]
  call test2 as z {input: array=strs}

  output {
    String x_value = x.value
    String y_value = y.value
    File z_the_output = z.the_output
  }
}
