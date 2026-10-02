version 1.0

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

workflow outer_expression {
  scatter (i in [1,2]) {
    Int x = i * 10

    scatter (j in [3,4]) {
      call add {
        input:
          a = x,
          b = j
      }
    }
  }

  output {
    Array[Array[File]] results = add.res
  }
}
