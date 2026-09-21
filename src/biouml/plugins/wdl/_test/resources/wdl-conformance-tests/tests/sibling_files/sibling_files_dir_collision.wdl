version 1.1

workflow wf {
    input {
    }
    call make_dir_collision as siblings

    call check_two_siblings as check1 {
        input:
            one_one=siblings.one_one,
            one_two=siblings.one_two,
            two_one=siblings.two_one,
            two_two=siblings.two_two
    }
    output {
        Boolean result1 = check1.result
    }
}

task make_dir_collision {
    input {
    }

    command <<<
        mkdir one
        mkdir two
        touch one/file1.txt
        touch one/file2.txt
        touch two/file1.txt
        touch two/file2.txt
    >>>

    output {
        File one_one = "one/file1.txt"
        File one_two = "one/file2.txt"
        File two_one = "two/file1.txt"
        File two_two = "two/file2.txt"
    }
}

task check_two_siblings {
    input {
        File one_one
        File one_two
        File two_one
        File two_two
    }

    command {
        echo ~{one_one}
        echo ~{one_two}
        echo ~{two_one}
        echo ~{two_two}

        if [[ "$(dirname ~{one_one})" != "$(dirname ~{one_two})" || "$(dirname ~{two_one})" != "$(dirname ~{two_two})" || "$(dirname ~{one_one})" == "$(dirname ~{two_two})" ]] ; then
            echo "FAIL" > output.txt
        else
            echo "SUCCESS" > output.txt
        fi
    }

    output {
        Boolean result = read_string("output.txt") == "SUCCESS"
    }
}