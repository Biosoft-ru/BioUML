version 1.1

task say_hello {
  input {
    String greeting
    String name
  }

  command <<<
  printf "~{greeting}, ~{name} how are you?"
  >>>
}

workflow test_scatter {
  input {
    Array[String] name_array = ["Joe", "Bob", "Fred"]
    String salutation = "Hello"
  }

  scatter (name in name_array) {
    call say_hello {
      input: greeting = salutation, name = name
    }
  }
}