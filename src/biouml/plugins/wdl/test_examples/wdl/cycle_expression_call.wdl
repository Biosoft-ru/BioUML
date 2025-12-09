version 1.2

task write_number {
  input {
    Float number
  }

  command {
    echo "~{number}" > output.txt
  }

  output {
    File output_file = "output.txt"
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

    call write_number {
      input:
        number = multiplied_number
    }
  }

  # Collect all output files
  Array[File] result_files = write_number.output_file
}