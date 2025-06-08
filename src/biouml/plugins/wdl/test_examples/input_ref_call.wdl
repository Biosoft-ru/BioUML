version 1.1

task double {
  input {
    Int int_in
  }

  command <<< >>>

  output {
    Int out = int_in * 2
  }
}

workflow input_ref_call {
  input {
    Int x
    Int y = d1.out
  }

  call double as d1 { input: int_in = x }
  call double as d2 { input: int_in = y }

  output {
    Int result = d2.out
  }
}