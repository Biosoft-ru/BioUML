version 1.0



task ask_how_are_you {
  input {
    File greeting_file
  }

  command {
     # Read greeting, append question, write to message.txt
    cat ~{greeting_file} > message.txt
    echo " How are you?" >> message.txt
  }

  output {
    File message_file = "message.txt"
  }

}
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
workflow mainWorkflow {
  input {
    String person_name = "Alice"
  }

  call say_hello  as say_hello {
     input:  name = person_name  }

  call ask_how_are_you  as ask_how_are_you {
     input:  greeting_file = say_hello.greeting_file  }
 output {
  File final_message = ask_how_are_you.message_file
}
}