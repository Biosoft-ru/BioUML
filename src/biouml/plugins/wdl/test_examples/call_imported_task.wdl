version 1.1

import "input_ref_call.wdl" as ns1

workflow call_imported_task {
  input {
    Int x
    Int y = d1.out
  }

  call ns1.double as d1 { input: int_in = x }
  call ns1.double as d2 { input: int_in = y }

  output {
    Int result = d2.out
  }
}