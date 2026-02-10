nextflow.enable.dsl=2
include { toChannel; get; getDefault; combineAll } from './biouml_function.nf'
workflow mainWorkflow {

  main:
  int_to_int = [ 1: 10, 2: 11 ]

  int_to_map = [ 1: [ 1: 2, 2: 3 ], 2: [ 1: 3, 2: 4 ], 5: [ 1: 6, 2: 7 ] ]

  string_to_string = [ "b": "2", "a": "1" ]

  file_to_ints = [ "/path/to/file2": [9,8,7], "/path/to/file1": [0,1,2] ]

  string_to_int = [ "b": 2, "a": 1 ]

  emit: 
  b = string_to_int["b"]
  from_map = int_to_map[2][1]
  ints = file_to_ints["/path/to/file1"]
  map = get(int_to_map, 2)
  ten = get(int_to_int, 1)
}

workflow {
mainWorkflow(  )
}
