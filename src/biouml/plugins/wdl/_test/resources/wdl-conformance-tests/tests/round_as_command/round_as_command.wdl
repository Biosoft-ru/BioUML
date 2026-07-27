version 1.1
workflow roundWorkflow {
  input {
    Float num
  }
  call get_round { input: num=num }

  output {
    File the_rounding = get_round.the_rounding
  }
}

task get_round {
  input {
    Float num
  }

  command {
    echo ${round(num)} > output.txt
  }

 output {
    File the_rounding = 'output.txt'
 }
}
