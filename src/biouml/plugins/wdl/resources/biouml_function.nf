def basename(path) {
    return path.getName()
}

def sub(input, pattern, replacement) {
    return input.replaceAll(pattern, replacement)
}

def ceil(val) {
	return Math.ceil(val)
}

def get(arr, index) {
    if (arr instanceof java.util.List || arr instanceof java.util.Map) {
        return arr[index]
    }
    else {
        return arr.collect().map{v->v[index]}
    }
}

def length(arr) {
    if (arr instanceof java.util.List)
        return arr.size()
    else 
        return arr.count()
}

def range(n) {
    if (n instanceof Integer)
    {
        return Channel.of(0..<n)
    } 
    else
    {
        return n.map { count -> 0..<count }.flatMap { it }
    }
}

def getDefault(x, y) {
    if( x == "NO_VALUE" ) {
        return y
    }
    else {
        return x
    }
}

def toChannel(arr)
{
    if (arr instanceof java.util.List)
        return Channel.of(arr).flatten()
     else 
        return arr
}

def select_first(arr)
{
	return arr.find { it != null && it != '' }
}

def select_all(array) {
    return array.findAll { it != null }
}

def defined(val) {
    return val != null
}

def read_string(filePath) {
    return new File(filePath).text.trim()
}

def read_int(filePath) {
    return new File(filePath).text.trim() as Integer
}

def read_float(filePath) {
    return new File(filePath).text.trim() as Float
}

def numerate(ch) {
    def counter = -1
    return ch.map { sublist ->
        counter++
        tuple(counter, sublist)
    }
}

def combineAll(inputs) {
    if (inputs.size() == 0) {
        return Channel.empty()
    }
    
    return inputs.drop(1).inject(toChannel(inputs[0])) { result, input ->
        result.combine(toChannel(input))
    }
}