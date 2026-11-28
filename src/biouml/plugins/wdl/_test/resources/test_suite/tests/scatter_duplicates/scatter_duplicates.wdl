version 1.0

task echo_int {
  input { Int x }
  command <<<
    echo ~{x}
  >>>
  output { Int value = read_int(stdout()) }
}

workflow scatter_duplicates {
  Array[Int] xs = [2, 1, 2]
  scatter (x in xs) {
    if (x == 2) {
      call echo_int { input: x = x }
    }
  }
  output { Array[Int?] results = echo_int.value }
}
