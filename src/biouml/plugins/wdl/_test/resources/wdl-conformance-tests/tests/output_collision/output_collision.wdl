version 1.1

workflow wf {
    input {
    }
    call make_files as sibling1 {
        input:
            value = "A"
    }
    call make_files as sibling2 {
        input:
            value = "B"
    }

    output {
        File file11 = sibling1.first
        File file12 = sibling1.second
        File file21 = sibling2.first
        File file22 = sibling2.second
    }
}

task make_files {
    input {
        String value
    }

    command <<<
        echo ~{value} >file1.txt
        echo ~{value} >file2.txt
    >>>

    output {
        File first = "file1.txt"
        File second = "file2.txt"
    }
}
