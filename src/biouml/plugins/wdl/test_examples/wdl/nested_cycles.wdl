version 1.0

workflow NestedLoops {
    input {
        Array[Int] I = [1, 2, 3]
        Array[Int] J = [10, 20]
        Array[Int] K = [100, 200, 300]
    }

    scatter (i in I) {
        
        call Call1 {
            input:
                i = i
        }
        
        scatter (j in J) {

            call Call2 {
                input:
                    i = j + 2
            }
            
            scatter (k in K) {
                
                call Call3 {
                    input:
                        value = i + j,
                        file1 = Call1.result,
                        file2 = Call2.result
                }
                
                call Call4 {
                    input:
                        files = [ Call1.result, Call2.result]
                }
            }
        }
    }

    output {
        Array[Array[Array[File]]] all_results = Call3.result
        Array[File] call1_results = Call1.result
        Array[Array[File]] call2_results = Call2.result
    }
}

task Call1 {
    input {
        Int i
    }

    command <<<
        echo "Call1: Processing i = ~{i}" > result~{i}.txt
    >>>

    output {
        File result = "result~{i}.txt"
    }
}

task Call2 {
    input {
        Int i
    }

    command <<<
        echo "Call1: Processing i = ~{i}" > result~{i}.txt
    >>>

    output {
        File result = "result~{i}.txt"
    }
}

task Call3 {
    input {
        Int value
        File file1
        File file2
    }

    command <<<
        echo "Call3: Processing value = ~{value}" > result.txt
        echo "" >> result.txt
        cat ~{file1} >> result.txt
        echo "" >> result.txt
        echo "File2 content:" >> result.txt
        cat ~{file2} >> result.txt
    >>>

    output {
        File result = "result.txt"
    }
}

task Call4 {
    input {
        Array[File] files
    }

    command <<<  
        # Iterate through all files in the array
        for file in ~{sep=' ' files}; do
            echo "File content:" >> result_array.txt
            cat $file >> result_array.txt
            echo "" >> result_array.txt
        done
    >>>

    output {
        File result = "result_array.txt"
    }
}
