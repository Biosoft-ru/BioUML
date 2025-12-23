version 1.0

task generate_file {

  command {
     echo "Hello!" > greeting.txt
  }

  output {
    File out = "greeting.txt"
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

  call generate_file {}

  call process_files {
    input:
      input_files = [generate_file.out],
      output_name = "res_all.txt"
  }

  output {
    File processed_file = process_files.output_file
  }

  meta {
    title: "Aray input 2"
    description: "First call generates file, second call recieves array generated on fly."
  }
}