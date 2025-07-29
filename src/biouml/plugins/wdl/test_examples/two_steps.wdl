version 1.0

task say_hello {
  input {
    String name
  }

  command {
    echo "Hello, ~{name}!" > greeting.txt
  }

  output {
    File greeting_file = "greeting.txt"
  }
}

task ask_how_are_you {
  input {
    File greeting_file
    String question
  }

  command {
    cat ~{greeting_file} > message.txt
    echo ~{question} >> message.txt
  }

  output {
    File message_file = "message.txt"
  }
}

workflow consecutive_steps {
  input {
    String person_name = "Alice"
    String question = "How are you?"
  }

  call say_hello {
    input:
      name = person_name
  }

  call ask_how_are_you {
    input:
      greeting_file = say_hello.greeting_file,
      question = question
  }

  output {
    File final_message = ask_how_are_you.message_file
  }
}