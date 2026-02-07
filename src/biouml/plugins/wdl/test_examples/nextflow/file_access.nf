#!/usr/bin/env nextflow
nextflow.enable.dsl=2

/*
 * Pipeline parameters
 */
params.numbers = [1, 2, 3, 4, 5]  // Array of numbers
params.outdir = "results"

/*
 * Process 1: Generate files from numbers
 * Takes a number as input and creates a file with that number
 */
process generateFile {
    publishDir "${params.outdir}/generated_files", mode: 'copy'
    
    input:
    val number
    
    output:
    path "file_${number}.txt"
    
    script:
    """
    echo "This is file number ${number}" > file_${number}.txt
    echo "Generated at: \$(date)" >> file_${number}.txt
    echo "Content: Number squared is \$((${number} * ${number}))" >> file_${number}.txt
    """
}

/*
 * Process 2: Process a specific file (the second file from the array)
 * Takes one file as input and processes it
 */
process processFile {
    publishDir "${params.outdir}/processed", mode: 'copy'
    
    input:
    path input_file
    
    output:
    path "processed_${input_file}"
    
    script:
    """
    echo "=== Processing ${input_file} ===" > processed_${input_file}
    echo "" >> processed_${input_file}
    cat ${input_file} >> processed_${input_file}
    echo "" >> processed_${input_file}
    echo "=== Processing complete ===" >> processed_${input_file}
    echo "Processed at: \$(date)" >> processed_${input_file}
    """
}

/*
 * Main workflow
 */
workflow {
    // Create a channel from the array of numbers
    numbers_ch = Channel.fromList(params.numbers)
    
    // Generate files for all numbers
    generated_files_ch = generateFile(numbers_ch)
    
    // Collect all generated files into a list and select the second one (index 1)
    second_file_ch = generated_files_ch
        .collect()
        .map { files -> files[1] }  // Get the second file (index 1)
    
    // Process only the second file
    processFile(second_file_ch)
}