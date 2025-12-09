params.numbers = [1.0, 2.0, 3.0, 4.0, 5.0]
params.numbers2 = [6.0, 6.0, 7.0, 8.0, 9.0]

workflow {
    numbers_ch = Channel.fromList(params.numbers)
	numbers_ch2 = Channel.fromList(params.numbers2)
		
	 calculated_i_ch = numbers_ch.map { i ->
        i1 = 1  
        i       
    }
    
    combined_ch = calculated_i_ch.combine(numbers_ch2)
    
    // Map to final structure
    result_ch = combined_ch.multiMap { i, j ->  // Now back to just i, j
        constant: 4
        s: [i, j]
    }
	
	result_ch.s.view()
}