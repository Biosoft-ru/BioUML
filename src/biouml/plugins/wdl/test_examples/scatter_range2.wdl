version 1.1

task square {
    input {
        Int input_value
        Int other_input
    }

    command {
        echo ~{input_value} * ~{other_input} > result.txt
    }

}

workflow square_array_files {
    input {
        Array[Int] arr = [ 1,4,2]
        Int constant = 3
    }

    scatter (idx in range(length(arr))) {
        call square {
            input:
                input_value = arr[idx],
                other_input = constant
        }
    }

}