version 1.0

task write_files {
    command <<<
        echo "Done"
    >>>

    output {
        File lines = write_lines(["apple", "banana", "cherry"])
        File tsv = write_tsv([ ["name", "age"], ["Alice", "25"], ["Bob", "30"] ])
        File map_file = write_map({ "apple": "red", "banana": "yellow" })
        File object_file = write_object(object { name: "Alice", age: "25" })
        File objects_file = write_objects([ object {name: "Alice", age: "25"}, object {name: "Bob", age: "30"} ])
        File json_file = write_json({ "name": "Alice", "age": 25, "active": true })
    }
}

workflow write_functions {
    call write_files

    output {
        File lines = write_files.lines
        File tsv = write_files.tsv
        File map_file = write_files.map_file
        File object_file = write_files.object_file
        File objects_file = write_files.objects_file
        File json_file = write_files.json_file
    }
}