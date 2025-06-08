version 1.1

struct Person {
  String name
  File? cv
}

task input_hint {
  input {
    Person person
  }

  command <<<
  if ~{defined(person.cv)}; then
    grep "WDL" ~{person.cv}
  fi
  >>>
  
  output {
    Array[String] experience = read_lines(stdout())
  }

  runtime {
    inputs: object {
      person: object {
        cv: object {
          localizationOptional: true
        }
      }
    }
  }
}