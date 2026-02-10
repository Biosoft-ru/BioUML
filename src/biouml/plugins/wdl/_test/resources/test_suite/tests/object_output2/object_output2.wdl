version 1.0

workflow TaskReturningObject {
  input {
     String sample_id = "sample_001"
  }

  call CreateObject {
    input:
      sample_name = sample_id
  }

  output {
    Object result = CreateObject.my_object
  }
  meta {
    title: "Object test 2"
    description: "Tests object creation in call."
  }
}

task CreateObject {
  input {
      String sample_name
  }

  command <<<
  >>>

  output {
    Object my_object = object {
       name: "my name",
       score: 10,
       value: 32.2
    }
  }
}