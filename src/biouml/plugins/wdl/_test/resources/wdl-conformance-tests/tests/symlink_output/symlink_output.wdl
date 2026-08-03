version 1.1

workflow wf {
    input {
    }
    call make_file
    call forward_file {
        input:
            infile = make_file.outfile
    }
    
    output {
        File outfile = forward_file.outfile
    }
}

task make_file {
    input {
    }

    command <<<
        echo "A" >file1.txt
    >>>

    output {
        File outfile = "file1.txt"
    }
}

task forward_file {
    input {
        File infile 
    }

    command <<<
        ln -s ~{infile} file2.txt
    >>>

    output {
        File outfile = "file2.txt"
    }
}
