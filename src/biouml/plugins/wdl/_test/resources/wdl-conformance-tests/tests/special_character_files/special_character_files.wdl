version 1.1

workflow wf {
    input {
    }
    call make_files as files_made

    call check as check1 {
        input:
            files = files_made.found
    }
    output {
        Boolean success = check1.success
    }
}

task make_files {
    input {
    }

    command <<<
        set -ex
        touch 'GRCh38#0#chm1.fa'
        touch 'GRCh38%230%23chm1.fa'
        touch "'"'"!@#$%^&*()_[]{};<>~`+=?—🌍'.fa
    >>>

    output {
        Array[File] found = glob("*.fa") 
    }
}

task check {
    input {
        Array[File] files
    }

    command <<<
        set -ex
        # Since the filenames can't appear on the command line bare or just
        # enclosed in either single-quotes or double-quotes, we can't use
        # quote() or squote(). We should be able to use a heredoc, but MiniWDL
        # can't handle that along with an idnented command (see
        # <https://github.com/chanzuckerberg/miniwdl/issues/674>). So we use
        # write_lines() to get a file name list. We make sure to use it in a
        # placeholder in order to be agnostic as to whether the local decls see
        # the same filesystem the command does.
        while IFS="" read -r FILENAME || [ -n "$FILENAME" ] ; do
            cp "$FILENAME" .
        done <~{write_lines(files)}
        stat 'GRCh38#0#chm1.fa'
        stat 'GRCh38%230%23chm1.fa'
        stat "'"'"!@#$%^&*()_[]{};<>~`+=?—🌍'.fa
        echo "SUCCESS" >"output.txt"
    >>>

    output {
        Boolean success = read_string("output.txt") == "SUCCESS"
    }
}
