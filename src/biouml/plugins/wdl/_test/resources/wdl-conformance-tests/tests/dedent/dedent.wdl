version 1.1

workflow wf {
    input {
    }
    call the_task
    output {
        Array[String] lines = read_lines(the_task.result)
    }
}

task the_task {
    input {
    }

    Array[String] lines = ["a", "b", "c"]

    command <<<
        set -ex
        cat >temp.txt <<EOF2
        ~{sep="\n" lines}
        EOF2
        echo "Task ran"
        cp temp.txt result.txt
    >>>

    output {
        File result = "result.txt"
    }
}

