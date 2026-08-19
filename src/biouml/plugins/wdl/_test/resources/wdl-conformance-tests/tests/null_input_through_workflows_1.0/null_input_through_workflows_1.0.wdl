version 1.1

import "workflow_with_default_input_1.0.wdl" as wf_in

workflow nullInput {
    input {
        Int? minHiFiReadLength
    }
    
    call wf_in.defaultInput as wfCall {
        input:
            minHiFiReadLength = minHiFiReadLength
    }
 
    output {
        Int result = wfCall.result
    }
 
}

