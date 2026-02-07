version 1.0

workflow TaskReturningObject {
    input {
        Array[String] sample_ids = ["sample_001", "sample_002", "sample_003"]
        Array[Int] score_values = [10, 20, 30, 40]
    }

    # Double scatter: iterate over both sample_ids and score_values
    scatter (sample_id in sample_ids) {
        scatter (score in score_values) {
            call CreateObject {
                input:
                    sample_name = sample_id,
                    score_value = score
            }
        }
    }

    output {
        Array[Array[Object]] results = CreateObject.my_object
    }
}

task CreateObject {
    input {
        String sample_name
        Int score_value
    }

    command <<<
    >>>

    output {
        Object my_object = object {
            name: sample_name,
            score: score_value,
            value: 32.2
        }
    }
}