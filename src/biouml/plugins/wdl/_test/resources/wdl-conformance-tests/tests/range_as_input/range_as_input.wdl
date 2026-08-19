version 1.1
workflow rangeWorkflow {
  # this workflow depends on write_lines()
  input {
    Int num
  }
  call copy_output {input: in_array=range(num)}

  output {
    File the_output = copy_output.the_output
  }
}

task copy_output {
  input {
    Array[Int] in_array
  }

  command {
    cp ${write_lines(in_array)} output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
