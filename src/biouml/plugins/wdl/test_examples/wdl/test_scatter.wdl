version 1.0

task say_hello {
  input {
    String greeting
  }

  command <<<
  echo "~{greeting}, how are you?" > result.txt
  >>>

  output {
    File result = "result.txt"
  }
}

task read_file {
  input {
    File file
  }

  command <<<
  echo "~{greeting}, how are you?" > result.txt
  >>>

  output {
    File result = "result.txt"
  }
}

workflow test_scatter {
  input {
    Array[String] name_array = ["Joe", "Bob", "Fred"]
    String salutation = "Hello"
  }

  scatter (name in name_array) {
    String greeting = "~{salutation} ~{name}"
    call say_hello { input: greeting = greeting }
  }

  output {
    Array[File] messages = say_hello.result
  }
}