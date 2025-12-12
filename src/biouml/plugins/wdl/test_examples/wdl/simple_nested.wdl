version 1.0

## Workflow with nested scatter (loop) structure
## Outer loop: for each i, call Task_1
## Inner loop: for each j, call Task_2 using results from Task_1

workflow NestedLoopWorkflow {
    input {
        Array[String] outer_items  # Items for outer loop (i)
        Array[String] inner_items  # Items for inner loop (j)
    }

    # Outer scatter: for (i)
    scatter (i in outer_items) {
        # Call Task_1 for each i
        call Task_1 {
            input:
                item = i
        }
        
        # Inner scatter: for (j)
        scatter (j in inner_items) {
            # Call Task_2 for each j, using result from Task_1
            call Task_2 {
                input:
                    item = j,
                    result_from_task1 = Task_1.result
            }
        }
    }

    output {
        Array[String] task1_results = Task_1.result
        Array[Array[String]] task2_results = Task_2.result
    }
}

## Task 1: Processes outer loop items
task Task_1 {
    input {
        String item
    }

    command <<<
        set -e
        echo "Processing Task_1 with item: ~{item}"
        
        # Your processing logic here
        # This example creates a result file
        echo "Result from Task_1 for ~{item}" > result_1.txt
        
        # Process the item
        echo "~{item}_processed" > output.txt
    >>>

    output {
        String result = read_string("output.txt")
        File result_file = "result_1.txt"
    }

    runtime {
        docker: "ubuntu:20.04"
        memory: "2 GB"
        cpu: 1
    }
}

## Task 2: Processes inner loop items using Task_1 results
task Task_2 {
    input {
        String item
        String result_from_task1
    }

    command <<<
        set -e
        echo "Processing Task_2 with item: ~{item}"
        echo "Using result from Task_1: ~{result_from_task1}"
        
        # Your processing logic here that uses both inputs
        # This example combines the inputs
        echo "Task_2 processed: ~{item} with context from ~{result_from_task1}" > output.txt
    >>>

    output {
        String result = read_string("output.txt")
    }

    runtime {
        docker: "ubuntu:20.04"
        memory: "2 GB"
        cpu: 1
    }
}