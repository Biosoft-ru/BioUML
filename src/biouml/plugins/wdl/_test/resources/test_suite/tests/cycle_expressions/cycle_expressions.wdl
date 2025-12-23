version 1.0

workflow process_array {
  input {
    Array[Int] x
  }

  scatter (element in x) {
    Int incremented = element + 1
    Int z = incremented*2
  }
  
  Array[Int] result = z

  output {
    Array[Int] z_values = z
    Array[Int] result_values = result
  }
  
    meta {
      title: "Expressions in cycle 1"
      description: "Two consequent expressions executed in cycle."
  }
}