version 1.1

workflow inputOverride {
    input {
        
    }
    call test_task {
        input:
            value_in = 5
    }
    output {
        Int result = test_task.result
    }
}

task test_task {
    input {
        Int value_in = 4
    }

    command <<<
        
    >>>

    output {
        Int result = value_in
    }
}
