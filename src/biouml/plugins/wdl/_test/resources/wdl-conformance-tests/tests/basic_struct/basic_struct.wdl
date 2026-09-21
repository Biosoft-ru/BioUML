version 1.1

struct sampleStruct {
        Int sample_index
        String sample_string
}

workflow structWorkflow {
    input {

    }

    

    output {
        sampleStruct struct_output = {"sample_index": 10, "sample_string": "hello"}
    }
}