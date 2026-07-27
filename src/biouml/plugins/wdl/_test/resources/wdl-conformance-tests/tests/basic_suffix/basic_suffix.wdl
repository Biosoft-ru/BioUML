version 1.1

workflow suffixWorkflow {
    input {
        String s
        Array[String] in_arr
    }

    output {
        Array[String] str_output = suffix(s, in_arr)
    }
}
