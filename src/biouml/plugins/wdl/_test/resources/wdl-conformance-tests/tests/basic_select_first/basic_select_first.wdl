version 1.1

workflow selectFirstWorkflow{
    input {
        Int? maybe_four
        Int? maybe_five
        Int? maybe_six

        String? maybe_string1
        String? maybe_string2
        String? maybe_string3

        Float? maybe_int1
        Float? maybe_int2
        Float? maybe_int3

        Boolean? maybe_true1
        Boolean? maybe_true2
        Boolean? maybe_true3

        File? maybe_file1
        File? maybe_file2
        File? maybe_file3
    }

    output {
        Int int_output = select_first([maybe_four, maybe_five, maybe_six])
        String str_output = select_first([maybe_string1, maybe_string2, maybe_string3])
        Float float_output = select_first([maybe_int1, maybe_int2, maybe_int3])
        Boolean bool_output = select_first([maybe_true1, maybe_true2, maybe_true3])
        File file_output = select_first([maybe_file1, maybe_file2, maybe_file3])
    }
}