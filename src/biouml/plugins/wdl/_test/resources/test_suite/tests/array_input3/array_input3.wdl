version 1.0

task generate_file {
  input {
    Int index
  }

  command <<<
    echo "This is file number ~{index}" > output_~{index}.txt
  >>>

  output {
    File out_files = "output_~{index}.txt"
  }
}

task process_files {

  input {
    Array[File] input_files
    String output_name
  }
    
  command <<<
    for file in ~{sep=" " input_files}; do
      echo "Processing: $file" >> ~{output_name}
    cat "$file" >> ~{output_name}
    done
  >>>

  output {
    File output_file = output_name
  }
}

workflow main {

  scatter (i in [0,1,2]) {
    call generate_file  {
      input: index = i
    }
  }

  call process_files {
    input:
      input_files = generate_file.out_files,
      output_name = "res_all.txt"
  }

  output {
    File processed_file = process_files.output_file
  }

  meta {
    title: "Aray input 3"
    description: "First call generates file and executed in cycle, second call recieves created array."
  }
}
