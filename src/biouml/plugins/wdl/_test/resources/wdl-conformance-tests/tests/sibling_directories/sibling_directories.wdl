version development

workflow wf {
    input {
    }
    call make_directories as sibling1
    call make_directories as sibling2

    call check as check1 {
        input:
            colliding=sibling1.first_child1,
            sibling1=sibling1.first,
            sibling2=sibling1.second
    }
    call check as check2 {
        input:
            colliding=sibling2.second,
            sibling1=sibling1.first,
            sibling2=sibling1.second
    }
    call check as check3 {
        input:
            sibling1=sibling1.first,
            sibling2=sibling1.second,
            inside_1_file=sibling1.first_file,
            inside_2_file=sibling1.second_file
    }
    call check as check4 {
        input:
            colliding=sibling1.first,
            sibling1=sibling1.first_child1,
            sibling2=sibling1.first_child2,
            inside_1_file=sibling1.first_child1_file,
            inside_2_file=sibling1.first_child2_file
    }
    call check as check5 {
        input:
            colliding=sibling2.first_child2,
            sibling1=sibling1.first_child1,
            sibling2=sibling1.first_child2,
            inside_1_file=sibling1.first_child1_file,
            inside_2_file=sibling1.first_child2_file
    }
    call check as check6 {
        input:
            colliding=sibling2.first,
            sibling1=sibling1.first,
            sibling2=sibling1.second,
            inside_1_dir=sibling1.first_child1,
    }
    output {
        Boolean result1 = check1.result
        Boolean result2 = check2.result
        Boolean result3 = check3.result
        Boolean result4 = check4.result
        Boolean result5 = check5.result
        Boolean result6 = check6.result
    }
}

task make_directories {
    input {
    }

    command <<<
        mkdir dir1
        mkdir dir2
        touch dir1/file.txt
        # To ensure that each file must be materialized inside its directory,
        # we give each a sibling inside its directory that muct appear next to
        # it if both it and the directory are referenced.
        touch dir1/otherfile.txt
        touch dir2/file.txt
        touch dir2/otherfile.txt
        mkdir dir1/dir1
        mkdir dir1/dir2
        touch dir1/dir1/file.txt
        touch dir1/dir1/otherfile.txt
        touch dir1/dir2/file.txt
        touch dir1/dir2/otherfile.txt
    >>>

    output {
        # Grab a file before its directory
        File first_child2_file = "dir1/dir2/file.txt"
        # Grab a subdirectory before the parent
        Directory first_child2 = "dir1/dir2"
        Directory first = "dir1"
        Directory second = "dir2"
        File first_file = "dir1/file.txt"
        File second_file = "dir2/file.txt"
        # Grab a subdirectory after the parent
        Directory first_child1 = "dir1/dir1"
        File first_child1_file = "dir1/dir1/file.txt"
    }
}

task check {
    input {
        File? inside_1_file
        Directory? inside_1_dir
        Directory? colliding
        Directory sibling1
        Directory sibling2
        File? inside_2_file
    }

    command {
        if [[ -n "~{colliding}" ]] ; then
            if [[ ! -f "~{colliding}/file.txt" ]] ; then
                echo >&2 "The colliding directory ~{colliding}, if specified, needs its contents."
                echo >&2 "Instead it has:"
                ls -lah "~{colliding}" >&2
                echo "FAIL" > output.txt
            fi
        fi

        if [[ -n "~{inside_1_file}" ]] ; then
            if [[ "$(realpath "$(dirname ~{inside_1_file})")" != "$(realpath "~{sibling1}")" ]] ; then
                echo >&2 "The file inside sibling 1 needs to actually be there when referenced before it. But ~{inside_1_file} is in $(dirname ~{inside_1_file}) and not ~{sibling1}"
                echo "FAIL" > output.txt
            fi
        fi

        if [[ -n "~{inside_1_dir}" ]] ; then
            if [[ "$(realpath "$(dirname ~{inside_1_dir})")" != "$(realpath "~{sibling1}")" ]] ; then
                echo >&2 "The directory inside sibling 1 needs to actually be there when referenced before it. But ~{inside_1_dir} is in $(dirname ~{inside_1_dir}) and not ~{sibling1}"
                echo "FAIL" > output.txt
            fi
        fi

        if [[ "$(realpath "$(dirname ~{sibling1})")" != "$(realpath "$(dirname ~{sibling2})")" ]] ; then
            echo >&2 "The sibling directories need to be siblings. But ~{sibling1} is in $(dirname ~{sibling1}) and ~{sibling2} is in $(dirname ~{sibling2})"
            echo "FAIL" > output.txt
        fi

        if [[ -n "~{inside_2_file}" ]] ; then
            if [[ "$(realpath "$(dirname ~{inside_2_file})")" != "$(realpath "~{sibling2}")" ]] ; then
                echo >&2 "The file inside sibling 2 needs to actually be there when referenced after it. But ~{inside_2_file} is in $(dirname ~{inside_2_file}) and not ~{sibling2}"
                echo "FAIL" > output.txt
            fi
        fi

        if [[ ! -e "output.txt" ]] ; then
            echo "SUCCESS" > output.txt
        fi
    }

    output {
        Boolean result = read_string("output.txt") == "SUCCESS"
    }
}
