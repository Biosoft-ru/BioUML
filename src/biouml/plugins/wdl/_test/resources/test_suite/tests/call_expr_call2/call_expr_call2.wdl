version 1.0

workflow ExampleWorkflow {
    input {
        File input_file
        Array[Int] numbers
    }
    
    scatter (num in numbers) {
        # Task 1: Receives integer and generates a file
        call GenerateFile {
            input:
                count = num
        }
    
        # Expression: Join two files into array
        Array[File] file_array = [input_file, GenerateFile.output_file]
    
        # Task 2: Receives two files (one from input, one from task 1)
       call ProcessFiles {
            input:
               files = file_array,
               index = num
       }
    }
    
    meta {
      title: "Cycle 3"
      description: "Tests extra step between two calls."
  }
}

task GenerateFile {
    input {
        Int count
    }
    
    command <<<
        echo "Generating file with ~{count} lines"
        for i in $(seq 1 ~{count}); do
            echo "Line $i" >> generated~{count}.txt
        done
    >>>
    
    output {
        File output_file = "generated~{count}.txt"
    }
    
    runtime {
        docker: "ubuntu:20.04"
    }
}

task ProcessFiles {
    input {
        Array[File] files
        Int index
    }
    
    command <<<
      cat ~{sep=" " files} > result_~{index}.txt
    >>>
    
    output {
        File result = "result_~{index}.txt"
    }
    
    runtime {
        docker: "ubuntu:20.04"
    }
}