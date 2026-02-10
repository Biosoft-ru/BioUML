version 1.0

task add_two_ints {
input {
    Int a
    Int b
	Int c
  }
  command {
    echo $((~{a} + ~{b} + ~{c})) > result1.txt
  }

  output {
    File sum = "result1.txt"
  }
}

task subtract_two_ints {
input {
    Int a
    Int b
  }
  command {
    echo $((~{a} - ~{b} )) > result2.txt
  }

  output {
    File sum = "result2.txt"
  }
}

workflow nested_scatters {
  Array[Int] outer_range = range(3)
  Array[Int] inner_range = range(2)
  
  scatter (i in outer_range) {
    scatter (j in inner_range) {
      call add_two_ints {
        input:
          a = i,
          b = j,
		  c = 3
      }
	  
	  call subtract_two_ints {
        input:
          a = i,
          b = j,
      }
    }
  }
}