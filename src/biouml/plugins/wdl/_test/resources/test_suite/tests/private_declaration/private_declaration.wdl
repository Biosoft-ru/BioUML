version 1.0

task private_declaration {
  input {
    String line1
    String line2
  }

  String concatenator = "~"
  String concatenated = line1 + concatenator + line2

  command <<<
    echo "~{concatenated}" > result.txt
  >>>

  output {
    File result = "result.txt"
  }
}

workflow run_private_declaration {
  input {
    Array[String] lines = ["A", "B", "C", "D"]
  }

  call private_declaration {
    input:
      line1 = lines[0],
      line2 = lines[1]
  }
  
  output {
    String concatenated_output = private_declaration.result
  }

  meta {
    title: "Private declaration"
    description: "Private declaration in task before command."
  }
}