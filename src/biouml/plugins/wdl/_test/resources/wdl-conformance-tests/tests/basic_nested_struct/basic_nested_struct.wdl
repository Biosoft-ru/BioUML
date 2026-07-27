version 1.1

struct sampleStruct {
        Int sample_int
        String sample_string
}

struct nestedSampleStruct {
    sampleStruct sample_struct
    Int sample_int
}
workflow structWorkflow {
    input {

    }    

    output {
        nestedSampleStruct struct_output = nestedSampleStruct {
            sample_int : 10,
            sample_struct : sampleStruct {
                sample_int : 20,
                sample_string : "Hello"
            }
        }
    }
}