version 1.1

workflow selectAllWorkflow {
    input {
        Int? maybe_int1
        Int? maybe_int2
        Int? maybe_int3

        String? maybe_string1
        String? maybe_string2
        String? maybe_string3

        Float? maybe_float1
        Float? maybe_float2
        Float? maybe_float3

        Boolean? maybe_bool1
        Boolean? maybe_bool2
        Boolean? maybe_bool3

        File? maybe_file1
        File? maybe_file2
        File? maybe_file3
    }

    output {
        Array[Int] int_output = select_all([maybe_int1, maybe_int2, maybe_int3])
        Array[String] str_output = select_all([maybe_string1, maybe_string2, maybe_string3])
        Array[Float] float_output = select_all([maybe_float1, maybe_float2, maybe_float3])
        Array[Boolean] bool_output = select_all([maybe_bool1, maybe_bool2, maybe_bool3])
        Array[File] file_output = select_all([maybe_file1, maybe_file2, maybe_file3])
    }
}