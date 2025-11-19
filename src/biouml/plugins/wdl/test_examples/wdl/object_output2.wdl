version 1.0

workflow TaskReturningObject {
    input {
        String sample_id = "sample_001"
    }

    call CreateObject {
        input:
            sample_name = sample_id
    }
CreateObject.my_object.view()
    output {
        Object result = CreateObject.my_object
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