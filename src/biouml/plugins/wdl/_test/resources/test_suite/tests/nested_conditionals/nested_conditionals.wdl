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

workflow nested_conditionals {
  scatter (i in [1,2,3]) {
    if (i != 3) {
      if (i == 1) {
        call make_int {
          input: value = i * 10
        }
      }
    }
  }

  output {
    Array[Int?] results = make_int.result
  }
}
