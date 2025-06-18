version 1.1

task ex_paramter_meta {
  input {
    File infile
    Boolean lines_only = false
    String? region
  }

  meta {
    description: "A task that counts the number of words/lines in a file"
  }

  parameter_meta {
    infile: {
      help: "Count the number of words/lines in this file"
    }
    lines_only: { 
      help: "Count only lines"
    }
    region: {
      help: "Cloud region",
      suggestions: ["us-west", "us-east", "asia-pacific", "europe-central"]
    }
  }

  command <<<
    wc ~{if lines_only then '-l' else ''} ~{infile}
  >>>

  output {
     String result = stdout()
  }

  runtime {
    container: "ubuntu:latest"
  }
}