version 1.0

task echo_int {
  input { Int x }
  command <<<
    echo ~{x}
  >>>
  output { Int value = read_int(stdout()) }
}

workflow scatter_depth_three {
  Array[Int] xs = [1, 2]
  scatter (a in xs) {
    scatter (b in xs) {
      scatter (c in xs) {
        call echo_int { input: x = a * 100 + b * 10 + c }
      }
    }
  }
  output { Array[Array[Array[Int]]] results = echo_int.value }
}
