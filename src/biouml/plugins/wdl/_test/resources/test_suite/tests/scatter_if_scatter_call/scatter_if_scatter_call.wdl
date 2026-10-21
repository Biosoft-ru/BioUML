version 1.0

task make_value {
    input {
        Int sample
        Int item
    }

    command <<<
        echo $((~{sample} * 10 + ~{item})) > result.txt
    >>>

    output {
        Int result = read_int("result.txt")
    }
}

task sum_values {
    input {
        Array[Int] values
    }

    command <<<
        sum=0
        for value in ~{sep=" " values}; do
            sum=$((sum + value))
        done
        echo "$sum" > result.txt
    >>>

    output {
        Int result = read_int("result.txt")
    }
}

workflow nested_scatter_then_call {
    Array[Int] samples = [1, 2, 3]
    Array[Int] items = [1, 2]

    scatter (sample in samples) {
        if (sample >= 2) {

            scatter (item in items) {
                call make_value {
                    input:
                        sample = sample,
                        item = item
                }
            }

            call sum_values {
                input:
                    values = make_value.result
            }
        }
    }

    output {
        Array[Array[Int]?] item_results = make_value.result
        Array[Int?] sums = sum_values.result
    }
}