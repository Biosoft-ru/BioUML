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

workflow conditional_call {
  scatter (i in [1,2,3]) {
    if (i == 2) {
      call make_int {
        input: value = i * 10
      }
    }

    Boolean exists = defined(make_int.result)
  }

  output {
    Array[Int?] values = make_int.result
    Array[Boolean] exists_values = exists
  }
}
