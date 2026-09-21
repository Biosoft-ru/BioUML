version 1.1

workflow failWorkflow {
    input {
        String in
    }

    output {
        Array[String] str_output = prefix(in, in)
    }
}