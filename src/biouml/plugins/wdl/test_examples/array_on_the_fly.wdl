version 1.2

workflow concat_files_workflow {
  input {
        Array[Int] arr = [ 1,4,2]
    }

  scatter (idx in range(length(arr))) {
    
    call write {
      input:
        input_value = arr[idx],
        index = idx }
    }
    
  call concat_files {
    input: files = write.out_file }

  output {
    File concatenated = concat_files.out_file
  }
}

task concat_files {
  input {
    Array[File] files
  }

  command <<<
       echo "List of file names:" > concatenated.txt
    for f in ${files.join(' ')}; do
      echo \$(basename \$f)
    done >> concatenated.txt
  >>>

  output {
    File out_file = "concatenated.txt"
  }

  runtime {
    docker: "ubuntu:latest"
  }
}

task write {
    input {
        Int input_value
        Int index
    }

    command {
        echo ~{input_value} > result_~{index}.txt
    }
    
    output {
    File out_file = "result_~{index}.txt"
    }
}