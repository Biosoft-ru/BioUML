version 1.1
workflow ceilWorkflow {
  input {
    Float num
  }
  call get_ceil { input: num=ceil(num) }

  output {
    File the_ceiling = get_ceil.the_ceiling
  }
}

task get_ceil {
  input {
    Float num
  }

  command {
    echo ${num} > output.txt
  }

 output {
    File the_ceiling = 'output.txt'
 }
}
