version 1.1

workflow mapWorkflow {
    input {
        Int first_int
        Int second_int
        Int third_int
        Int fourth_int
    }

    output {
        Map[Int, Int] map_output = {first_int: second_int, third_int: fourth_int}
    }
}