version 1.0

task generate_file {
  input {
    Int index
  }

  command <<<
    echo "This is file number ~{index}" > output_~{index}.txt
  >>>

  output {
    File out_file = "output_~{index}.txt"
  }
}

task process_file {
  input {
    File in_file
  }

  command <<<
    cat ~{in_file} >> processed_~{basename(in_file)}
  >>>

  output {
    File processed_file = "processed_~{basename(in_file)}"
  }
}

workflow scatter_two_steps {
  input {
    Array[Int] indices = [1, 3, 5]
    String extra_info       # New input added here
  }

  scatter (i in indices) {
    call generate_file {
      input: index = i
    }

    call process_file {
      input: 
        in_file = generate_file.out_file
    }
  }

  output {
    Array[File] generated_files = generate_file.out_file
    Array[File] processed_files = process_file.processed_file
  }

  meta {
    title: "Two steps 3"
    description: "Two consequent calls inside cycle."
  }
}