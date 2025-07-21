def basename(path) {
    return path.tokenize('/').last()
}

def sub(input, pattern, replacement) {
    return input.replaceAll(pattern, replacement)
}