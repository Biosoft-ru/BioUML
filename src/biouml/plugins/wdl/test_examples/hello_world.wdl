version 1.0 

task sayHello {
  command {
     echo "Hello World"
  }
  output {
     File output_greeting = stdout()
  }
}

workflow HelloWorld {
  call sayHello
}