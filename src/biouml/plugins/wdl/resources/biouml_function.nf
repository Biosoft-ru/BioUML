def basename(path) {
    return path.getName()
}

def sub(input, pattern, replacement) {
    return input.replaceAll(pattern, replacement)
}

def ceil(val) {
	return Math.ceil(val)
}

def length(arr) {
    return arr.size()
}

def range(n) {
    return (0..<n).toList()
}