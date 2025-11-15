
params.numbers = [1.0, 2.0, 3.0, 4.0, 5.0]

workflow {
    numbers_ch = Channel.fromList(params.numbers)
	
    calculated_ch = numbers_ch.multiMap { num ->
	    constant:  4
    }
	
	calculated_ch.constant.view()
}