version 1.0

workflow wf {
    input {
    }
    call make_file {
    input:
        content = "toplevel"
    }
    
    if (1 == 1) {
        call make_file as make_file_if {
        input:
            content = "if"
        }
    }

    scatter (i in [1, 2, 3]) {
        if (i == 2) {
            call make_file as make_file_deep {
            input:
                content = "scatter" + i
            }
        }
    }

    scatter (i in [1, 2, 3]) {
        if (i != 3) {
            if (i == 1) {
                call make_file as make_file_deeper {
                input:
                    content = "otherscatter" + i
                }
            }
        }
    }
}

task make_file {
    input {
        String content
    }

    command <<<
        echo "~{content}" >file.txt
    >>>

    output {
        File outfile = "file.txt"
    }
}


