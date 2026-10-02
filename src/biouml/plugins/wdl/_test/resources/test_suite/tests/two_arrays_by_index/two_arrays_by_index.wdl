version 1.0

task process_pair {
    input {
        Int a
        Int b
    }

    command <<<
        echo "~{a},~{b}" > result.txt
    >>>

    output {
        String result = read_string("result.txt")
    }
}

workflow two_arrays_by_index {

    Array[Int] arr1 = [10, 20, 30, 40]
    Array[Int] arr2 = [1, 2, 3, 4]

    Array[Int] indexes = range(length(arr1))

    scatter (i in indexes) {

        call process_pair {
            input:
                a = arr1[i],
                b = arr2[i]
        }
    }

    output {
        Array[String] results = process_pair.result
    }
}