version 1.0

workflow process_array {
  input {
    Array[Int] x
  }

  scatter (element in x) {
    Int incremented = element + 1
    
    if (incremented > 2) {
       Int z_conditional = incremented * 2
    }
    
    Int z = select_first([z_conditional, incremented])
  }
  
  output {
    Array[Int] z_values = z
  }
}