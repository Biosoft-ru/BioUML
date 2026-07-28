version 1.1

# In version 1.1 we are allowed to have substitution expression placeholders in
# strings.

# They have to be parsed Bash-like, so quotes can be used unescaped inside of
# the placeholder inside of the quote-delimited string.

workflow placeholderWorkflow {
  input { 
    String one_in = "1"
  }
  
  Array[String] one_one = ["1"]
  
  Array[String] ones_inside = ["1", "${one_in}", "~{one_in}", '${"1"}', '~{"1"}', "${'1'}", "~{'1'}", "${"1"}", "~{"1"}", '${'1'}', '~{'1'}', "${sep="" one_one}", '${sep="" one_one}']
  
  output {
    Array[String] ones = ones_inside
  }
}
