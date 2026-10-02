version 1.0

task calculate {
  input {
    Int value
  }

  command {
    echo ~{value} > first.txt
    echo $((~{value} * 10)) > second.txt
  }

  output {
    Int first = read_int("first.txt")
    Int second = read_int("second.txt")
  }
}

task add {
  input {
    Int a
    Int b
  }

  command {
    echo $((~{a} + ~{b})) > result_~{a}_~{b}.txt
  }

  output {
    File res = "result_~{a}_~{b}.txt"
  }
}

workflow multiple_outputs {
  scatter (i in [1,2]) {
    call calculate {
      input: value = i
    }

    call add {
      input:
        a = calculate.first,
        b = calculate.second
    }
  }

  output {
    Array[Int] first_values = calculate.first
    Array[Int] second_values = calculate.second
    Array[File] results = add.res
  }
}
