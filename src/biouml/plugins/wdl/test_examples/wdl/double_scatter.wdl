version 1.0

task add_two_ints {
input {
    Int a
    Int b
    Int c
  }
  command {
    echo $((~{a} + ~{b}+ ~{c})) > result.txt
  }

  output {
    File res = "result.txt"
  }
}

workflow nested_scatters {
  Array[Int] outer_range = range(3)
  Array[Int] inner_range = range(2)
  
  scatter (i in outer_range) {
    scatter (j in inner_range) {
      Int k = i*j
      call add_two_ints {
        input:
          a = i+1,
          b = j,
          c = k
      }
    }
  }
}