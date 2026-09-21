version 1.1
workflow typePairWorkflow {
  # test conformance with the WDL language specification - Pair Literals

  input {
    # file import
    Pair[String, File] test_pair_file

    # file import with arrays
    Array[Pair[String, File]] test_array_pair_file

    File test_int
    File test_string
  }

  call copy_output {
    input:
      test_pair_file=test_pair_file,
      test_array_pair_file=test_array_pair_file,
      test_int=test_int,
      test_string=test_string
  }

  output {
    File the_output = copy_output.the_output
  }
}

task copy_output {
  input {
    Pair[String, File] test_pair_file
    Array[Pair[String, File]] test_array_pair_file
    File test_int
    File test_string
  }
  # not allowed, move to json input
#  File test_int = 'tests/version_1.0/legacy/testfiles/test_int.txt'
#  File test_string = 'tests/version_1.0/legacy/testfiles/test_string.txt'
  Array[Pair[String, File]] test_array_pair_file_from_wdl_in_task = [
    ('test_A', test_int),
    ('test_B', test_string)
  ]
  # miniwdl will crash as the files technically do not exist
  command {
    cp ${write_json([
                    read_lines(test_pair_file.right),
                    read_lines(select_first(test_array_pair_file).right),
                    read_lines(select_first(test_array_pair_file_from_wdl_in_task).right)])} output.txt
  }
  # empty when run with miniwdl
#  String test = select_first(test_array_pair_file_from_wdl_in_task).right
#  command {
#    echo ${test} | tee -a output.txt
#    cat ${test} | tee -a output.txt
#  }

 output {
    File the_output = 'output.txt'
#   File file_out = select_first(test_array_pair_file_from_wdl_in_task).right
#   String the_output = select_first(test_array_pair_file_from_wdl_in_task).right
 }
}
