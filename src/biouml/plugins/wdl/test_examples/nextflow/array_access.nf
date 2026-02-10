#!/usr/bin/env nextflow

nextflow.enable.dsl=2

/*
 * Nextflow translation of WDL array_access workflow
 * Demonstrates accessing array elements by index
 */

workflow {
    // Define input parameters
    params.strings = ['hello', 'world', 'nextflow', 'dsl2']
    params.index = 2
    
    // Access array element by index
    def strings = params.strings
    def index = params.index
    
    // Get the element at the specified index
    def s = strings[index]
    
    // Output the result
    println "String at index ${index}: ${s}"
    
    // Emit as channel for further processing if needed
    Channel
        .of(s)
        .view { "Selected string: ${it}" }
}