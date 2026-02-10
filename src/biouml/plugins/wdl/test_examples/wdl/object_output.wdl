version 1.0

workflow SimpleObjectExample {
    input {
        String name = "sample_001"
        Int quality_score = 95
        Float coverage = 42.5
    }

    # Create an object with three fields
    Object my_object = object {
        sample_name: name,
        quality: quality_score,
        avg_coverage: coverage
    }


    output {
        Object result_object = my_object
    }
}
