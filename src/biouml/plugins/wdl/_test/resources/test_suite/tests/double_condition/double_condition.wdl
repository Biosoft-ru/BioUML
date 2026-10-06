version 1.0

task make_int {
  input {
    Int value
  }

  command <<<
    echo ~{value} > result.txt
  >>>

  output {
    Int result = read_int("result.txt")
  }
}

workflow conditional_call {
  Array[Int] i_array = [1, 2, 3, 4]

  scatter (i in i_array) {

    if (i == 3 || i == 1) {
      call make_int {
        input:
          value = i * 10
      }
    }

    Boolean exists = defined(make_int.result)
  }

  output {
    Array[Boolean] exists_values = exists
    Array[Int?] values = make_int.result
  }
}