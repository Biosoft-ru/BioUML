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

def createChannelIfNeeded(arr)
{
    if (arr instanceof java.util.List)
        return Channel.of(arr).flatten()
     else 
        return arr
}