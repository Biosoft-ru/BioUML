version 1.0 

task sayHello {
  command {
     echo "Hello World" > hello.txt
  }
  output {
     File result = "hello.txt"
  }
}

workflow HelloWorld {
  call sayHello
}