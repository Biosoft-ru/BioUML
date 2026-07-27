version 1.1

workflow basenameWorkflow {
    input {
        String path
        File f
    }

    output {
        String str_output = basename(path)
        String str_output_removed = basename(path, ".txt")
        String str_output_file = basename(f)
        String str_output_file_removed = basename(f, ".json")
    }
}