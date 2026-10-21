version 1.0

workflow scatter_if_scatter {

    Array[Int] outer_values = [1, 2, 3]
    Array[Int] inner_values = [10, 20]

    scatter (i in outer_values) {

        if (i == 2) {

            scatter (j in inner_values) {
                Int result = i + j
            }
        }
    }

    output {
        Array[Array[Int]?] results = result
    }
}