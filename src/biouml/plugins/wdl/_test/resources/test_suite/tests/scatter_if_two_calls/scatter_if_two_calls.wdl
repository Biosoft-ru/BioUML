version 1.0

task echo_int {
  input { Int x }
  command <<<
    echo ~{x}
  >>>
  output { Int value = read_int(stdout()) }
}

task increment {
  input { Int x }
  command <<<
    echo $((~{x} + 1))
  >>>
  output { Int value = read_int(stdout()) }
}

workflow scatter_if_two_calls {
  Array[Int] xs = [1, 2, 3]
  scatter (x in xs) {
    if (x > 1) {
      call echo_int { input: x = x }
      call increment { input: x = echo_int.value }
    }
  }
  output { Array[Int?] results = increment.value }
}
