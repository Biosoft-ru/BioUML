import groovy.json.JsonOutput

def basename(path, suffix = null) {
    def name = new File(path.toString()).getName()

    if (suffix != null && name.endsWith(suffix))
        return name.substring(0, name.length() - suffix.length())

    return name
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
	return arr.find { it != null && it != "NO_VALUE" }
}

def select_all(array) {
    return array.findAll { it != null && it != "NO_VALUE"}
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

def read_boolean(filePath) {
    def text = new File(filePath).text.trim()

    if (text == 'true')
        return true

    if (text == 'false')
        return false

    throw new IllegalArgumentException(
        "Invalid boolean value: '${text}'. Expected 'true' or 'false'."
    )
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
    
    return inputs.drop(1).inject(toChannel(inputs[0])) { result, input -> result.combine(toChannel(input)) }
}

def wdl_to_string(value) {
    if (value == null)
        return null
    if (value instanceof Boolean)
        return value ? 'true' : 'false'
    return value.toString()
}

def quote(values) {
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "\"${s}\""
    }
}

def squote(values) {
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "\'${s}\'"
    }
}

def sep(delimiter, values) {
    values.collect { wdl_to_string(it) }
          .join(delimiter)
}

// ------------------------------------
// as_pairs(Map[K,V]) -> Array[Pair[K,V]]
// WDL: {"a":1,"b":2} -> [["a",1],["b",2]]
// ------------------------------------
def as_pairs(map) {
    map.collect { key, value -> [key, value] }
}

// ------------------------------------
// as_map(Array[Pair[K,V]]) -> Map[K,V]
// WDL: [["a",1],["b",2]] -> {"a":1,"b":2}
// ------------------------------------
def as_map(pairs) {
    pairs.collectEntries { pair ->
        [(pair[0]): pair[1]]
    }
}

// ------------------------------------
// keys(Map[K,V]) -> Array[K]
// ------------------------------------
def keys(map) {
    map.keySet().toList()
}

// ------------------------------------
// zip(Array[X], Array[Y]) -> Array[Pair[X,Y]]
// WDL requires equal length
// ------------------------------------
def zip(a, b) {
    if (a.size() != b.size()) {
        throw new IllegalArgumentException(
            "zip(): arrays must have equal size " +
            "(${a.size()} != ${b.size()})"
        )
    }

    (0..<a.size()).collect { i ->
        [a[i], b[i]]
    }
}

// ------------------------------------
// round(Float) -> Int
// WDL rounds halves away from zero
// ------------------------------------
def round(value) {
    if (value >= 0)
        return Math.floor(value + 0.5d) as Integer
    else
        return Math.ceil(value - 0.5d) as Integer
}

// ------------------------------------
// write_json(X) -> File
// Writes JSON and returns path
// ------------------------------------
def write_json(value, fileName) {
    def file = new File(fileName)
    file.text = JsonOutput.prettyPrint(JsonOutput.toJson(value))
    return file.path
}

def write_json(value) {
    def file = File.createTempFile("wdl_", ".json")

    file.text = JsonOutput.prettyPrint(
        JsonOutput.toJson(value)
    ) + '\n'

    return file.path
}

def prefix(prefixValue, values) {
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "${prefixValue}${s}"
    }
}

def suffix(suffixValue, values) {
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "${s}${suffixValue}"
    }
}

def read_lines(filePath) {
    return new File(filePath).readLines()
}

def write_lines(value) {
    def file = File.createTempFile("wdl_", ".txt")

    file.text = value
        .collect { v -> wdl_to_string(v) }
        .join('\n') + '\n'

    return file.path
}


def collect_by_key(pairs) {
    def result = [:].withDefault { [] }

    pairs.each { pair ->
        result[pair[0]] << pair[1]
    }

    return result
}

def size(value, unit = 'B') {
    double bytes

    if (value instanceof Collection) {
        bytes = value.sum { new File(it.toString()).length() ?: 0 } as double
    }
    else {
        bytes = new File(value.toString()).length() as double
    }

    switch (unit) {
        case 'B':
            return bytes
        case 'KB':
            return bytes / 1_000d
        case 'MB':
            return bytes / 1_000_000d
        case 'GB':
            return bytes / 1_000_000_000d
        case 'TB':
            return bytes / 1_000_000_000_000d

        case 'KiB':
            return bytes / 1024d
        case 'MiB':
            return bytes / (1024d * 1024d)
        case 'GiB':
            return bytes / (1024d * 1024d * 1024d)
        case 'TiB':
            return bytes / (1024d * 1024d * 1024d * 1024d)

        default:
            throw new IllegalArgumentException("Unsupported size unit: '${unit}'")
    }
 }

 def read_tsv(filePath) {
    def file = new File(filePath.toString())

    if (!file.exists())
        throw new IllegalArgumentException("File does not exist: ${filePath}")

    return file.readLines().collect { line ->
        line.split('\t', -1).toList()
    }
}

def write_tsv(value, fileName) {
    def file = new File(fileName)

    file.text = value.collect { row -> row.collect { cell -> wdl_to_string(cell) }.join('\t') }.join('\n') + '\n'

    return file.path
}

def write_tsv(value) {
    def file = File.createTempFile("wdl_", ".tsv")

    file.text = value.collect { row -> row.collect { cell -> wdl_to_string(cell) }.join('\t')}.join('\n') + '\n'

    return file.path
}

def cross_wdl(left, right) {
    def result = []
    for (l in left) {
        for (r in right) {
            result << [l, r]
        }
    }
    return result
}


def transpose_wdl(array) {
    if (array == null || array.size() == 0)
        return []

    def width = array[0].size()
    def result = []

    for (int i = 0; i < width; i++) {
        def row = []
        for (int j = 0; j < array.size(); j++) {
            row << array[j][i]
        }
        result << row
    }

    return result
}

def contains(array, value) {
    if (array == null)
        return false

    return array.contains(value)
}

def unzip(array) {
    if (array == null || array.isEmpty()) {
        return [[], []]
    }

    def left = []
    def right = []

    for (item in array) {
        left << item[0]
        right << item[1]
    }

    return [left, right]
}

def flatten_wdl(value) {
    if (value == null) {
        return null
    }

    return value.flatten()
}