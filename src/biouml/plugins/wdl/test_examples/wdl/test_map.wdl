version 1.1

workflow test_map {
  Map[Int, Int] int_to_int = {1: 10, 2: 11}
  Map[String, Int] string_to_int = { "a": 1, "b": 2 }
  Map[String, String] string_to_string = { "a": "1", "b": "2" }
  Map[File, Array[Int]] file_to_ints = {
    "/path/to/file1": [0, 1, 2],
    "/path/to/file2": [9, 8, 7]
  }
  
  Map[Int,Map[Int,Int]] int_to_map = {1: {1:2, 2:3}, 2: {1:3,2:4}, 5: {1:6,2:7} }
  
  output {
    Int ten = int_to_int[1]  # evaluates to 10
    Int b = string_to_int["b"]  # evaluates to 2
    Array[Int] ints = file_to_ints["/path/to/file1"]  # evaluates to [0, 1, 2]
    Map[Int, Int] map = int_to_map[2]
    Int from_map = int_to_map[2][1]
  }
}