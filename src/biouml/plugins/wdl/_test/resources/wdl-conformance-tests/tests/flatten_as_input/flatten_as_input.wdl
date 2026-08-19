version 1.1
workflow flattenWorkflow {
  input {
    Array[Array[Int]] in_array
  }

  call copy_output {input: in_array=flatten(in_array)}

  output {
    File the_output = copy_output.the_output
  }
}

task copy_output {
  input {
    Array[Int] in_array
  }

#  command {
#    cp ${write_json(in_array)} output.txt
#  }
  command {
    echo ${sep="," in_array} | tee output.txt
  }

  output {
    File the_output = 'output.txt'
  }
}
