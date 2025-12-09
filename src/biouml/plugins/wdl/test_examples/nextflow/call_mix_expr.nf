#!/usr/bin/env nextflow
nextflow.enable.dsl=2

// Process to square an integer
process square_int {
  input:
    val num

  output:
    val squared

  script:
  """
  echo ${num} * ${num}
  """
}

// Main workflow
workflow {
    // Input array
    Channel.of(1, 2, 3, 4, 5)
        .set { elements }
    
    // Increment and square
    square_int(elements.map { it + 1 }).view()
    
    // Calculate z = squared - original using merge and collect results
    //square_int.out
    //    .map { it.trim().toInteger() }
    //    .merge(elements)
    //    .map { squared, element -> squared - element }
     //   .collect()
     //   .view { z_values -> "Result z_values: ${z_values}" }
}