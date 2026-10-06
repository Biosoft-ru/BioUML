version 1.0

task make_int {
  input {
    Int value
  }

  command {
    echo ~{value} > result.txt
  }

  output {
    Int result = read_int("result.txt")
  }
}

task use_int {
  input {
    Int value
  }

  command {
    echo ~{value} > result_~{value}.txt
  }

  output {
    File res = "result_~{value}.txt"
  }
}

workflow outer_call {
  scatter (i in [1,2]) {
    call make_int {
      input: value = i * 10
    }

    scatter (j in [3,4]) {
      call use_int {
        input: value = make_int.result + j
      }
    }
  }

  output {
    Array[Array[File]] results = use_int.res
  }
}
