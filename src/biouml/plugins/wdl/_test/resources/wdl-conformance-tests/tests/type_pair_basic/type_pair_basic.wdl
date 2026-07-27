version 1.1
workflow typePairWorkflow {
  # test conformance with the WDL language specification - Pair Literals

  input {
    Pair[Int, String] test_pair

    # a pair inside a pair
    Pair[String, Pair[String, String]] test_pair_pair
  }

  # a pair defined inside WDL
  Pair[Int, String] test_pair_from_wdl = (23, "twenty-three")

  output {
    Int left_pair_output = test_pair.left
    String right_pair_output = test_pair.right
    String nested_pair_output = test_pair_pair.right.right
    Pair[Int, String] full_pair_output = test_pair_from_wdl
  }
}
