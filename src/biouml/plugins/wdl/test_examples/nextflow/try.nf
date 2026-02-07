#!/usr/bin/env nextflow
nextflow.enable.dsl=2

/*
 * Emulating nested loop structure:
 * x = [[1,2,3], [4,5,6], [7,8,9]]
 * for each sublist s in x:
 *   k = s.length
 *   for each element in s:
 *     task1(element)
 */

process task1 {
    tag "Processing: $value"
    
    input:
    val value
    
    output:
    tuple val(value), val("processed_${value}")
    
    script:
    """
    echo "Processing value: ${value}"
    """
}

workflow {
    // x = [[1,2,3], [4,5,6], [7,8,9]]
    x = Channel.of(
        [1, 2, 3],
        [4, 5, 6],
        [7, 8, 9]
    )
    
    // Using multiMap to preserve intermediate variables
    // This emulates the outer loop iteration
    x_mapped = x.multiMap { s ->
        // s = x[i] (current sublist)
        //sublist: s
        // k = s.length
        //length: s.size()
        // Flatten for inner loop - emulates iterating through s[j]
        elements: s
    }
    
    // Flatten the sublists to get individual elements
    // This emulates the inner loop: for (int j=0; j<k; j++)
    elements_flattened = x_mapped.elements.flatten()
    
    // Process each element with task1
    // This emulates: task1(s[j])
    results = task1(elements_flattened)
    
    // View the results
    results.view { value, result -> 
        "task1($value) -> $result"
    }
}