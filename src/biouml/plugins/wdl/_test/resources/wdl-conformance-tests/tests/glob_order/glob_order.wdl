version 1.1

workflow globOrder {
    input {
        
    }
    call test_task {
    }
    output {
        Array[File] result = test_task.result
    }
}

task test_task {
    input {
    }

    command <<<
        echo "3" >data3.txt
        echo "1" >data1.txt
        echo "2" >data2.txt
    >>>

    output {
        Array[File] result = glob("data*.txt")
    }

    runtime {
        # Constrain Bash
        container: "ubuntu:24.04"
    }
}
