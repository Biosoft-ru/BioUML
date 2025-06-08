version 1.1

import "other.wdl" as lib

task repeat {
  input {
    Int i
    String? opt_string
  }
  
  command <<<
  for i in 1..~{i}; do
    printf ~{select_first([opt_string, "default"])}
  done
  >>>

  output {
    Array[String] lines = read_lines(stdout())
  }
}

workflow call_example {
  input {
    String s
    Int i
  }

  # Calls repeat with one required input - it is okay to not
  # specify a value for repeat.opt_string since it is optional.
  call repeat { input: i = 3 }

  # Calls repeat a second time, this time with both inputs.
  # We need to give this one an alias to avoid name-collision.
  call repeat as repeat2 {
    input:
      i = i * 2,
      opt_string = s
  }

  # Calls repeat with one required input using the abbreviated 
  # syntax for `i`.
  call repeat as repeat3 { input: i, opt_string = s }

  # Calls a workflow imported from lib with no inputs.
  call lib.other
  # This call is also valid
  call lib.other as other_workflow2 {}

  output {
    Array[String] lines1 = repeat.lines
    Array[String] lines2 = repeat2.lines
    Array[String] lines3 = repeat3.lines
    Int? results1 = other.results
    Int? results2 = other_workflow2.results  
  }
}