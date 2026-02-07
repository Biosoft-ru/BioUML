version 1.0

task generate_files {
       
    input {
       Int num_files
    }

    command <<<
        set -e
        mkdir -p output
        i=1
        while [ $i -le ~{num_files} ]; do
            echo "This is file $i" > output/file_${i}.txt
            i=$((i + 1))
        done
    >>>

    output {
        Array[File] files = glob("output/file_*.txt")
    }
}

task process_files {
    
    input {
       Array[File] input_files
       String output_name
    }
    
    command <<<
   for file in ~{sep=" " input_files}; do
       echo "Processing: $file" >> ~{output_name}
       cat "$file" >> ~{output_name}
   done
>>>

    output {
       File output_file = output_name
    }
}

workflow file_chain_wdl {
    
    input {
       Int num_files
    }
    
    call generate_files {
        input:
            num_files = num_files
    }

    call process_files {
        input:
            input_files = generate_files.files,
            output_name = "res_all.txt"
    }

    output {
        File processed_file = process_files.output_file
    }
        
}
    