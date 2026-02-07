version 1.2



task square {
  input {
    Int input_value
    Int idx
  }

  runtime {
    docker: "bash"
  }

  command {
     echo $((input_value * input_value)) > squared_${idx}.txt
  }

  output {
    File out_file = "squared_${idx}.txt"
  }
}

workflow scatter_range {
  input {
    Array[Int] arr
  }

  Array[Int] expression = range(length(arr))
  scatter ( idx in expression ) {
  call square  as square {
     input:  idx = idx, input_value = arr  }

  }

  output {
    Array[File] square_files = square.out_file
  }
}
