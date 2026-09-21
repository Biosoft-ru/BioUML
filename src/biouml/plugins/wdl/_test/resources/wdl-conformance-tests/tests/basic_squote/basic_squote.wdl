version 1.1

workflow squoteWorkflow{
    input {
        Array[String] str_arr
        Array[Int] int_arr
        Array[Float] float_arr
        Array[Boolean] bool_arr
        Array[File] file_arr
    }
    call file_squote {input: file_arr = file_arr}
    output {
        Array[String] str_output = squote(str_arr)
        Array[String] int_output = squote(int_arr)
        Array[String] float_output = squote(float_arr)
        Array[String] bool_output = squote(bool_arr)
        File file_output = file_squote.out
    }
}


task file_squote {
    input {
        Array[File] file_arr
    }

    command <<<
        echo ~{sep(' ', squote(file_arr))} >> output.txt
    >>>

    output {
        File out = "output.txt"
    }
}
