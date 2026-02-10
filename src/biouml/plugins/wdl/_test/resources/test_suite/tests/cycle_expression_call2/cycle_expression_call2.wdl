version 1.0

task write_number {
  input {
    Float i
    Float j
  }

  command {
    echo "~{i} ~{j}" > output_1_~{i}_~{j}.txt
  }

  output {
    File output_file = "output_1_~{i}_~{j}.txt"
  }
}

task write_number2 {
  input {
    Float i
    Float j
  }

  command {
    echo "~{i} ~{j}" > output_2_~{i}_~{j}.txt
  }

  output {
    File output_file = "output_2_~{i}_~{j}.txt"
  }
}

workflow multiply_and_write {
  input {
    Array[Float] numbers  # Input array of numbers
    Float factor          # Multiply factor
  }

  scatter (num in numbers) {
    Float multiplied_number = num * factor
    Float divided = num / factor
    Float divided2 = divided / factor
    
    call write_number {
      input:
        i = multiplied_number,
        j = divided2
    }
	
	call write_number2 {
      input:
        i = divided,
        j = divided2
    }
  }

  
  meta {
    title: "Expression before call 2"
    description: "Tests multiple expressions before calls in cycle."
  }
}