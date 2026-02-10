nextflow.enable.dsl=2

process combine_numbers {
    input:
    val number1
	val number2
	
    publishDir "combine_numbers", mode: 'copy'

    script:
    """
    echo "Number1: ${number1} "Number1: ${number2}
    """
	
	output:
    stdout
	
}

workflow  {

     main:
     array1 = [1, 2, 3, 9]
     array2 = [4, 5, 6, 11]
    Channel.from(array1).set { ch_array1 }
    Channel.from(array2).set { ch_array2 }

    combined = ch_array1.combine(ch_array2)
	combine_numbers(combined).view()

	}