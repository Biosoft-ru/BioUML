version 1.0

# Task 1: Generate a file based on an integer input
task generate_file_a {
  input {
    Int index
  }
  command {
    echo "File A: ~{index}" > output_a_~{index}.txt
  }
  output {
    File outFile = "output_a_~{index}.txt"
  }
}

# Task 2: Generate another file based on an integer input
task generate_file_b {
  input {
    Int index
  }
  command {
    echo "File B: ~{index}" > output_b_~{index}.txt
  }
  output {
    File outFile = "output_b_~{index}.txt"
  }
}

# Task 3: Process two files into a third
task process_files {
  input {
    File fileA
    File fileB
  }
  command {
    cat ~{fileA} ~{fileB} > combined_~{basename(fileA)}_~{basename(fileB)}.txt
  }
  output {
    File combinedFile = "combined_~{basename(fileA)}_~{basename(fileB)}.txt"
  }
}

# Workflow definition
workflow simple_generate_and_process {
  input {
    Array[Int] arrayA = [1,2,3]
    Array[Int] arrayB = [4,5,6]
  }

  # Generate files for arrayA
  scatter(i in range(length(arrayA))) {
    call generate_file_a {
      input:
        index = arrayA[i]
    }
  }

  # Generate files for arrayB
  scatter(j in range(length(arrayB))) {
    call generate_file_b {
      input:
        index = arrayB[j]
    }
  }

  # Assuming you want to process pairs (i-th from arrayA with i-th from arrayB)
  # Only works if both arrays are of the same length
  scatter( k in range(length(arrayA)) ) {
    call process_files {
      input:
        fileA = generate_file_a.outFile[k],
        fileB = generate_file_b.outFile[k]
    }
  }

  # Outputs: all combined files
  output {
    Array[File] results = process_files.combinedFile
  }
}