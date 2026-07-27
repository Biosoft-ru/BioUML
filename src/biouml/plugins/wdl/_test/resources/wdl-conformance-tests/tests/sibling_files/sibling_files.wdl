version 1.1

workflow wf {
    input {
    }
    call make_files as sibling1
    call make_files as sibling2

    call check as check1 {
        input:
            collide1=sibling2.second,
            real1=sibling1.first,
            real2=sibling1.second,
            collide2=sibling2.first
    }
    call check as check2 {
        input:
            collide1=sibling2.first,
            real1=sibling1.second,
            real2=sibling1.first,
            collide2=sibling2.second
    }
    call check as check3 {
        input:
            collide1=sibling1.first,
            real1=sibling1.first,
            real2=sibling1.second,
            collide2=sibling1.second
    }
    call check as check4 {
        input:
            collide1=sibling1.first,
            real1=sibling2.second,
            real2=sibling2.first,
            collide2=sibling1.second
    }
    call check as check5 {
        input:
            collide1=sibling1.second,
            real1=sibling2.first,
            real2=sibling2.second,
            collide2=sibling1.first
    }
    output {
        Boolean result1 = check1.result
        Boolean result2 = check2.result
        Boolean result3 = check3.result
        Boolean result4 = check4.result
        Boolean result5 = check5.result
    }
}

task make_files {
    input {
    }

    command <<<
        touch file1.txt
        touch file2.txt
    >>>

    output {
        File first = "file1.txt"
        File second = "file2.txt"
    }
}

task check {
    input {
        File collide1
        File real1
        File real2
        File collide2
    }

    command {
        echo ~{collide1}
        echo ~{real1}
        echo ~{real2}
        echo ~{collide2}

        if [[ "$(dirname ~{real1})" != "$(dirname ~{real2})" ]] ; then
            echo "FAIL" > output.txt
        else
            echo "SUCCESS" > output.txt
        fi
    }

    output {
        Boolean result = read_string("output.txt") == "SUCCESS"
    }
}