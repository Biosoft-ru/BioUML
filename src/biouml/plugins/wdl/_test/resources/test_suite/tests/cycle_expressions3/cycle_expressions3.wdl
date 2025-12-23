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
    
    Int z1 = select_first([z_conditional, incremented])
  }
  
    scatter (element in x) {
    Int decremented = element - 1
    
    if (decremented < 2) {
      Int z_conditional2 = decremented * 2
    }
    
    Int z2 = select_first([z_conditional2, decremented])
  }
  
  output {
    Array[Int] z_values1 = z1
    Array[Int] z_values2 = z2
  }

  meta {
    title: "Expressions in cycle 3"
    description: "Expressions in cycle with condition repeated."
  }
}