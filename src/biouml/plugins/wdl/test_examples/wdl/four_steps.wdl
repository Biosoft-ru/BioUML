version 1.0



task step1 {
  command {
     echo "Step 1 output" > step1.txt
  }

  output {
    File out_file = "step1.txt"
  }
}

task step2 {
  input {
    File input_file
  }

  command {
     cat ~{input_file} > step2.txt
    echo "Step 2 output" >> step2.txt
  }

  output {
    File out_file = "step2.txt"
  }
}

task step3 {
  input {
    File input_file
  }

  command {
     cat ~{input_file} > step3.txt
    echo "Step 3 output" >> step3.txt
  }

  output {
    File out_file = "step3.txt"
  }
}

task step4 {
  input {
    File input_file
  }

  command {
     cat ~{input_file} > step4.txt
    echo "Step 4 output" >> step4.txt
  }

  output {
    File out_file = "step4.txt"
  }
}

workflow four_steps {
  call step1  {
  }

  call step2  {
     input:  input_file = step1.out_file  }

  call step3  {
     input:  input_file = step2.out_file  }

  call step4  {
     input:  input_file = step3.out_file  }

  output {
    File final_output = step4.out_file
  }
}