version 1.0

# Input struct with 3 fields
struct InputData {
    String sample_name
    Int score_value
    Float quality_score
}

# Output struct with 2 fields (subset of input)
struct OutputData {
    String sample_name
    Int score_value
}

workflow StructWorkflow {
    input {
        Array[InputData] input_records = [
            {
                "sample_name": "sample_001",
                "score_value": 10,
                "quality_score": 95.5
            },
            {
                "sample_name": "sample_002",
                "score_value": 20,
                "quality_score": 87.3
            },
            {
                "sample_name": "sample_003",
                "score_value": 30,
                "quality_score": 92.1
            }
        ]
    }

    # Scatter over input records
    scatter (record in input_records) {
        call ProcessData {
            input:
                input_data = record
        }
    }

    output {
        Array[OutputData] results = ProcessData.output_data
    }
}

task ProcessData {
    input {
        InputData input_data
    }

    command <<<
        # Process the data
        echo "Processing sample: ~{input_data.sample_name}"
        echo "Score: ~{input_data.score_value}"
        echo "Quality: ~{input_data.quality_score}"
    >>>

    output {
        # Create output struct with 2 fields from input struct
        OutputData output_data = object {
            sample_name: input_data.sample_name,
            score_value: input_data.score_value
        }
    }

    runtime {
        docker: "ubuntu:latest"
    }
}