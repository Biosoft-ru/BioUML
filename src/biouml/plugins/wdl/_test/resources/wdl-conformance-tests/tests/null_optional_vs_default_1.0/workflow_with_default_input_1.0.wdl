version 1.0

import "task_without_default_input_1.0.wdl" as task_in

workflow defaultInput {
    input {
        Int minHiFiReadLength=1
    }

    call task_in.noDefault as taskCall {
        input:
            minReadLength = minHiFiReadLength
    }
    
    output {
        Int result = taskCall.result
    }
}
