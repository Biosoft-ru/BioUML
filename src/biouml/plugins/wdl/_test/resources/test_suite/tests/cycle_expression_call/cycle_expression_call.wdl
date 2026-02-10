version 1.0

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

workflow multiply_and_write {
  input {
    Array[Float] numbers  # Input array of numbers
    Float factor          # Multiply factor
  }

  scatter (num in numbers) {
    Float multiplied_number = num * factor

    call write_number {
      input:
        number = multiplied_number
    }
  }

  output {
    Array[File] result_files = write_number.output_file
  }
  
  meta {
    title: "Expression before call"
    description: "Tests expression before call in cycle."
  }
}