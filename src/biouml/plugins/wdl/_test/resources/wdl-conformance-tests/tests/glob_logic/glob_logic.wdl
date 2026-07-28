version 1.1

workflow globLogic {
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
        echo "-is-*.txt" >globdata.txt
        echo "cooool" >math-is-cool.txt
        echo "uhoh" >math-is-hard.txt
        echo "yum" >lunch-is-cool.txt
        echo "lunch" >>globdata.txt
    >>>

    output {
        # Do some logic in the glob argument that is hard to fake
        Array[File] result = glob(read_lines("globdata.txt")[1] + read_lines("globdata.txt")[0])
    }

    runtime {
        # Constrain Bash
        container: "ubuntu:24.04"
    }
}
