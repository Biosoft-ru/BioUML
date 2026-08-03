version 1.1
workflow lengthWorkflow {
  input {
    Array[Int] in_array
  }
  call copy_output {input: num=length(in_array)}

  output {
    File the_output = copy_output.the_output
  }
}

task copy_output {
  input {
    Int num
  }

  command {
    echo ${num} > output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
