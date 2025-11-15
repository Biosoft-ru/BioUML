version 1.0

task square_int {
  input {
    Int num
  }
  command <<< 
    echo $((~{num} * ~{num}))
  >>>
  output {
    Int squared = read_int(stdout())
  }
}

task square_int2 {
  input {
    Int num
  }
  command <<< 
    echo $((~{num} * ~{num}))
  >>>
  output {
    Int squared = read_int(stdout())
  }
}

workflow process_array {
  input {
    Array[Int] x
  }

  scatter (element in x) {
    Int incremented = element + 1

    call square_int {
      input:
        num = incremented
    }
	
	call square_int2 {
      input:
        num = element
    }

    # Declare an output variable for z inside scatter
    Int z = square_int.squared
  }

  output {
    # Collect all z values from scatter block
    Array[Int] z_values = z
  }
}