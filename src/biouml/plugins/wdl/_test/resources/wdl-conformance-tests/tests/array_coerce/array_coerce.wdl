version 1.1

workflow arrayCoerce {

    input {
    }

    output {
        File json = write_json([[1], "test"])
    }
}