version 1.1

workflow prefixWorkflow {
  input {
    Array[String] str_arr
    String pre
  }
  output {
    Array[String] str_output = prefix(pre, str_arr)
  }
}
