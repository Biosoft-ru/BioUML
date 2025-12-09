version 1.0

task write_number {
  input {
    Float number1
    Float number2
  }

  command {
    echo "~{number1} ~{number2}" > output1.txt
  }

  output {
    File output_file = "output1.txt"
  }
}

task write_number2 {
  input {
    Float number1
    Float number2
  }

  command {
    echo "~{number1} ~{number2}" > output2.txt
  }

  output {
    File output_file = "output2.txt"
  }
}

# Define the workflow
workflow multiply_and_write {
  input {
    Array[Float] numbers  # Input array of numbers
    Float factor          # Multiply factor
  }

  scatter (num in numbers) {
    # Multiply each number by the factor
    Float multiplied_number = num * factor
   
    Float divided = num / factor
    
    Float divided2 = divided / factor
    
    call write_number {
      input:
        number1 = multiplied_number,
        number2 = divided2
    }
	
	call write_number2 {
      input:
        number1 = divided,
        number2 = divided2
    }
	
	
  }

  # Collect all output files
  Array[File] result_files = write_number.output_file
}