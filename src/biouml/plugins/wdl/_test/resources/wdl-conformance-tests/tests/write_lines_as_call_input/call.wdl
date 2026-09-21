version 1.1

workflow wf {
    input {
    }
    call make_file_with_write_lines

    call file_to_string {
        input:
            first=make_file_with_write_lines.out
    }
    output {
        String out = file_to_string.out
    }
}

task make_file_with_write_lines {
    input {
    }

    command <<<
    >>>

    output {
        File out = write_lines(["file"])
    }
}

task file_to_string {
    input {
        File first
    }

    command {
    }

    output {
        String out = read_string(first)
    }
}