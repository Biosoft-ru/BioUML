version 1.0

task echo_int {
  input { Int x }
  command <<<
    echo ~{x}
  >>>
  output { Int value = read_int(stdout()) }
}

workflow scatter_if_false {
  Array[Int] xs = [1, 2]
  scatter (x in xs) {
    if (x < 0) {
      call echo_int { input: x = x }
    }
  }
  output { Array[Int?] results = echo_int.value }
}
