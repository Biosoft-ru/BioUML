#!/usr/bin/env nextflow

nextflow.enable.dsl = 2

// Define parameters
params.numbers = [1.0, 2.0, 3.0, 4.0, 5.0]
params.numbers2 = [15, 26, 37, 48, 59]
params.factor = 2.0

// Process equivalent to write_number task
process WRITE_NUMBER {
    publishDir 'extra_steps/write_number', mode: 'copy'
    
    input:
    val number1
    val number2
    
    output:
    path "output1_${number1}${number2}.txt"
    
    script:
    """
    echo "${number1} ${number2}" >> output1_${number1}${number2}.txt
    """
}

// Process equivalent to write_number2 task
process WRITE_NUMBER2 {
    publishDir 'extra_steps/write_number2', mode: 'copy'
    
    input:
    val number1
    val number2
    
    output:
    path "output2_${number1}${number2}.txt"
    
    script:
    """
    echo "${number1} ${number2}" > output2_${number1}${number2}.txt
    """
}

process CONCAT_OUTPUTS {
    publishDir 'extra_steps/concat', mode: 'copy'
	
    input:
    path files

    output:
    path 'all_output.txt'

    script:
    """
    cat ${files.join(' ')} > all_output.txt
    """
}

// Main workflow
workflow {
    // Create channel from numbers array
    numbers_ch = Channel.fromList(params.numbers)
    numbers2_ch = Channel.fromList(params.numbers2)
	
    // Perform calculations and split into multiple channels using multiMap
    calculated_ch = numbers_ch.multiMap { num ->
        def multiplied_number = num * params.factor
        def divided = num / params.factor
        def divided2 = divided / params.factor
		def const = 4
        
		//const: const
		divided2: divided2
        write_number: [multiplied_number, divided2]
        write_number2: [divided, divided2]
    }
	
	calculated2_ch = calculated_ch.divided2.combine(numbers2_ch).multiMap { pair ->
	def plus = pair[0] +pair[1]
	plus: plus
	}
	//calculated_ch.const.view()
    //calculated2_ch.plus.view()
	//calculated_ch.divided2.view()
    // Call processes with their respective inputs
    WRITE_NUMBER(calculated_ch.write_number.map { it[0] }, calculated_ch.write_number.map { it[1] })
    WRITE_NUMBER2(calculated_ch.write_number2.map { it[0] } , calculated_ch.write_number2.map { it[1] })
    
    // Collect all output files from WRITE_NUMBER
    result_files = WRITE_NUMBER.out.merge(WRITE_NUMBER2.out)collect()
	
	CONCAT_OUTPUTS(result_files)
}