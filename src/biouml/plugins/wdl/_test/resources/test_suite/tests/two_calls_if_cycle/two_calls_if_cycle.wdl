version 1.0

task call1 {
    input {
        Int value
    }

    command <<<
        echo $((~{value} * 10)) > result.txt
    >>>

    output {
        Int result = read_int("result.txt")
    }
}

task call2 {
    input {
        Int value
        Int original
    }

    command <<<
        echo $((~{value} + ~{original})) > result.txt
    >>>

    output {
        Int result = read_int("result.txt")
    }
}

workflow two_calls_if_cycle {

    Array[Int] values = [1, 2, 3, 4, 5]

    scatter (i in values) {

        if (i > 2) {

            call call1 {
                input:
                    value = i
            }

            call call2 {
                input:
                    value = call1.result,
                    original = i
            }
        }
    }

    output {
        Array[Int?] first_results = call1.result
        Array[Int?] second_results = call2.result
    }
}