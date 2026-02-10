version 1.0

task square {
  input {
      Int input_value
  }

  command {
      echo ~{input_value} * ~{input_value}
  }
}

workflow square_array_files {

  input {
      Int len = 3
  }

  scatter (idx in range(len)) {
      call square {
          input:
          input_value = idx
      }
  }
  
  meta {
    title: "Cycle in range 1"
    description: "Cycle using range function."
  }
  
}