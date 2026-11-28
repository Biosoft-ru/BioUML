version 1.0

task echo_int {
  input { Int x }
  command <<<
    echo ~{x}
  >>>
  output { Int value = read_int(stdout()) }
}

workflow scatter_empty_inner {
  input { Array[Array[Int]] groups }
  scatter (group in groups) {
    scatter (x in group) {
      call echo_int { input: x = x }
    }
  }
  output { Array[Array[Int]] results = echo_int.value }
}
