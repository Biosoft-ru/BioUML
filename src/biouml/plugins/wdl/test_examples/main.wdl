version 1.1

import "other.wdl" as other_wf

task echo {
  input {
    String msg = "hello"
  }
  
  command <<<
  printf ~{msg}
  >>>
  
  output {
    File results = stdout()
  }
  
  runtime {
    container: "ubuntu:latest"
  }
}

workflow main {
  Array[String] arr = ["a", "b", "c"]

  call echo
  call echo as echo2
  call other_wf.foobar { input: infile = echo2.results }
  call other_wf.other { input: b = true, f = echo2.results }
  call other_wf.other as other2 { input: b = false }
  
  scatter(x in arr) {
    call echo as scattered_echo {
      input: msg = x
    }
    String scattered_echo_results = read_string(scattered_echo.results)
  }

  output {
    String echo_results = read_string(echo.results)
    Int foobar_results = foobar.results
    Array[String] echo_array = scattered_echo_results
  }
}