version 1.1

task private_declaration {
  input {
    String line1
    String line2
  }

  String concatenated = "~"

  command <<<
  head -~{num_lines_clamped} ~{write_lines(lines)}
  >>>

  output {
    Array[String] out_lines = read_lines(stdout())
  }
}

workflow run_private_declaration {

    input {
        Array[String] lines  = ["A", "B", "C", "D"]
    }

        call private_declaration {
            input:
            lines = lines
        }
    
}