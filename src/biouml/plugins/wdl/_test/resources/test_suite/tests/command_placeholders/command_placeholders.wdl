version 1.0

task check_placeholders {
    Boolean enabled = true
    Boolean disabled = false

    String? missing = None
    String? present = "ORIGINAL"

    command <<<
        printf '%s' '~{true="YES" false="NO" enabled}' > boolean_true.txt
        printf '%s' '~{true="YES" false="NO" disabled}' > boolean_false.txt
        printf '%s' '~{default="FALLBACK" missing}' > default_missing.txt
        printf '%s' '~{default="FALLBACK" present}' > default_present.txt
    >>>

    output {
        String boolean_true = read_string("boolean_true.txt")
        String boolean_false = read_string("boolean_false.txt")
        String default_missing = read_string("default_missing.txt")
        String default_present = read_string("default_present.txt")
    }
}

workflow command_placeholders {
    call check_placeholders

    output {
        String boolean_true = check_placeholders.boolean_true
        String boolean_false = check_placeholders.boolean_false
        String default_missing = check_placeholders.default_missing
        String default_present = check_placeholders.default_present
    }
}