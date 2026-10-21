version 1.0

workflow complex_pair_optional_array_test {

    Pair[Array[Int?], Array[Pair[String, Int?]]] value = ( [1, None, 3],[ ("a", 10),("b", None), ("c", 30)])

    Int? missing_number = value.left[1]
    String second_key = value.right[1].left
    Int? second_value = value.right[1].right

    output {
        Pair[Array[Int?], Array[Pair[String, Int?]]] result = value
        Int? missing = missing_number
        String key = second_key
        Int? optional_value = second_value
    }
}