version 1.0

task sayHello {
  command {
     echo "Hello World" > hello.txt
  }

  output {
    File result = "hello.txt"
  }

}
workflow mainWorkflow {
  call sayHello  as sayHello {
  }

 output {
    File result = sayHello.result
  }

}