def noNull(value)
{
    if (value == null || value == "NO_VALUE")
    {
        error("ERROR: Required input was not provided")
    }
    return value
}

def fileOrNull(value) 
{
    if (value == null || value == "NO_VALUE")
        return null

    return file(value)
}

def orNull(x) 
{
    return x == null ? "NO_VALUE" : x
}

def basename_wdl(path, suffix = null) 
{
    def name = new File(path.toString()).getName()

    if (suffix != null && name.endsWith(suffix))
        return name.substring(0, name.length() - suffix.length())

    return name
}

def sub_wdl(input, pattern, replacement) 
{
    return input.toString().replaceAll(pattern, replacement)
}

def matches_wdl(input, pattern) 
{
    if (input == null || pattern == null)
        return false

    return java.util.regex.Pattern.compile(pattern.toString()).matcher(input.toString()).find()
}

def find_wdl(input, pattern) 
{
    if (input == null || pattern == null)
        return "NO_VALUE"

    def matcher = java.util.regex.Pattern.compile(pattern.toString()).matcher(input.toString())

    if (!matcher.find())
        return "NO_VALUE"

    return matcher.group()
}

def ceil_wdl(val) 
{
    return Math.ceil(val as Double).intValue()
}

def get(arr, index) 
{
    if (arr instanceof java.util.List || arr instanceof java.util.Map) 
        return arr[index]
    else
        return arr.collect().map{v->v[index]}
}

def length(arr) 
{
    if (arr instanceof java.util.List)
        return arr.size()
    else 
        return arr.count()
}

def range(n) 
{
    if (n instanceof Integer)
        return Channel.of(0..<n)
    else
        return n.map { count -> 0..<count }.flatMap { it }
}

def min_wdl(a, b) 
{
    if (!(a instanceof Number) || !(b instanceof Number))
        throw new IllegalArgumentException("min() expects numeric arguments" )

    if (a instanceof Integer &&b instanceof Integer)
        return Math.min(a as int, b as int)

    return Math.min(a as double, b as double)
}

def max_wdl(a, b) 
{
    if (!(a instanceof Number) || !(b instanceof Number))
        throw new IllegalArgumentException("max() expects numeric arguments")

    if (a instanceof Integer && b instanceof Integer)
        return Math.max(a as int, b as int)
    return Math.max(a as double, b as double)
}

def chunk_wdl(array, n)
{
    if (array == null)
        throw new IllegalArgumentException("chunk() expects an array")

    int size = n as int

    if (size <= 0)
        throw new IllegalArgumentException("chunk() size must be greater than zero: ${n}")

    List values = array instanceof List? array: array.toList()
    return values.collate(size)
}

def getDefault(value, defaultValue) 
{
    return value == null || value == "NO_VALUE" ? defaultValue : value
}

def toChannel(arr) 
{
    if (arr instanceof java.util.List)
        return Channel.of(arr).flatten()
     else 
        return arr
}

def select_first_wdl(value) 
{
    if (value instanceof groovyx.gpars.dataflow.DataflowReadChannel)
        return value.map { items -> select_first_value_wdl(items) }

    return select_first_value_wdl(value)
}


def select_first_value_wdl(value) 
{
    if (!(value instanceof Collection))
        throw new IllegalArgumentException("select_first_wdl expects an array, received: ${value?.getClass()?.name}")
    def item = value.find { it != null && it != "NO_VALUE" }
    if (item != null)
        return item
    else   
        throw new IllegalArgumentException("select_first_wdl: array contains no defined values")
}

def select_all_wdl(array) 
{
    if (array instanceof groovyx.gpars.dataflow.DataflowReadChannel)
    {
        return array.map { value -> select_all_wdl(value) }
    }
    return array.findAll { it != null && it != "NO_VALUE" }
}

def defined_wdl(val) 
{
    return val != null && val != "NO_VALUE"
}

def read_string_wdl(filePath) 
{
    if (filePath == null)
        return null

    if (filePath instanceof java.nio.file.Path)
        return filePath.toFile().text.trim()

    if (filePath instanceof File)
        return filePath.text.trim()

    return new File(filePath.toString()).text.trim()
}

def read_map_wdl(filePath)
{
    File file = filePath instanceof java.nio.file.Path
        ? filePath.toFile()
        : filePath instanceof File
            ? filePath
            : new File(filePath.toString())

    def result = [:]

    file.eachLine { line ->
        if (!line.trim().isEmpty())
        {
            def parts = line.split('\t', 2)

            if (parts.size() != 2)
                throw new IllegalArgumentException(
                    "Invalid WDL map line, expected two tab-separated columns: '${line}'"
                )

            result[parts[0]] = parts[1]
        }
    }

    return result
}

def read_object_wdl(filePath)
{
    File file = filePath instanceof java.nio.file.Path? filePath.toFile(): filePath instanceof File? filePath: new File(filePath.toString())

    List<String> lines = file.readLines()

    if (lines.size() != 2)
        throw new IllegalArgumentException("read_object(): expected exactly 2 rows, got ${lines.size()}")

    List<String> keys = lines[0].split('\t', -1).toList()
    List<String> values = lines[1].split('\t', -1).toList()

    if (keys.size() != values.size()) 
            throw new IllegalArgumentException("read_object(): header and value rows have different lengths")

    if (keys.toSet().size() != keys.size())
        throw new IllegalArgumentException("read_object(): duplicate field names")

    Map result = new LinkedHashMap()

    keys.eachWithIndex { key, i -> result[key] = values[i] }

    return result
}

def write_object_wdl(object)
{
    if (!(object instanceof Map))
       throw new IllegalArgumentException("write_object() expects Object/Struct")

    validateObjectPrimitiveValues(object, "write_object()")
    File file = File.createTempFile("wdl_object_",".tsv")
    String header = object.keySet().collect { it.toString() }.join('\t')
    String values = object.values().collect { wdl_to_string(it) }.join('\t')
    file.text = header + '\n' + values + '\n'
    return file.path
}

def read_objects_wdl(filePath)
{
    File file = filePath instanceof File
        ? filePath
        : new File(filePath.toString())

    List<String> lines = file.readLines()

    if (lines.isEmpty())
        return []

    List<String> keys = lines[0].split('\t', -1).toList()

    if (keys.toSet().size() != keys.size())
        throw new IllegalArgumentException(
            "read_objects(): duplicate field names"
        )

    List result = []

    lines.drop(1).eachWithIndex { line, index ->
        List<String> values = line.split('\t', -1).toList()

        if (values.size() != keys.size())
            throw new IllegalArgumentException(
                "read_objects(): row ${index + 2} contains ${values.size()} fields, expected ${keys.size()}"
            )

        Map object = new LinkedHashMap()

        keys.eachWithIndex { key, i ->
            object[key] = values[i]
        }

        result << object
    }

    return result
}

def write_objects_wdl(objects)
{
    if (!(objects instanceof Collection))
        throw new IllegalArgumentException("write_objects() expects an array")

    File file = File.createTempFile("wdl_objects_", ".tsv")

    if (objects.isEmpty())
    {
        file.text = ""
        return file.path
    }

    List<Map> rows = objects.collect { object ->

        if (!(object instanceof Map))
            throw new IllegalArgumentException("write_objects() expects Object/Struct elements")
        validateObjectPrimitiveValues( object, "write_objects()")
        return object
    }

    List keys = rows[0].keySet().toList()

    rows.eachWithIndex { object, index ->

        if (object.keySet().toSet() != keys.toSet())
            throw new IllegalArgumentException("write_objects(): object ${index} has different fields")
    }

    StringBuilder result = new StringBuilder()

    result.append( keys.collect { it.toString() }.join('\t') ).append('\n')

    rows.each { object ->

        result.append(
            keys.collect { key ->
                wdl_to_string(object[key])
            }.join('\t')
        ).append('\n')
    }

    file.text = result.toString()
    return file.path
}

def glob_wdl(pattern, workDir)
{
    if (pattern == null || pattern == "NO_VALUE")
        return []

    def baseDir = workDir.toAbsolutePath().normalize()
    def matcher = java.nio.file.FileSystems.getDefault().getPathMatcher( "glob:" + pattern.toString() )
    def result = []
    def stream = java.nio.file.Files.list(baseDir)
    stream.each { file ->

        if (matcher.matches(file.getFileName()))
            result.add(file)
    }
    stream.close()
    return result.sort { a, b -> a.toString() <=> b.toString() }
}

def read_int_wdl(filePath)
{
    return new File(filePath).text.trim() as Integer
}

def read_lines_bash(filePath) 
{
    return "\$(cat ${filePath})"
}

def read_int_bash(filePath) 
{
    return "\$(cat ${filePath} | tr -d '\\r\\n')"
}

def read_string_bash(filePath) 
{
    return "\$(cat ${filePath})"
}

def read_float_bash(filePath) 
{
    return "\$(cat ${filePath} | tr -d '\\r\\n')"
}

def read_boolean_bash(filePath) 
{
    return "\$(cat ${filePath} | tr -d '\\r\\n')"
}

def read_float_wdl(filePath) 
{
    return new File(filePath).text.trim() as Float
}

def read_boolean_wdl(filePath) 
{
    def text = new File(filePath).text.trim().toLowerCase(Locale.ROOT)

    if (text == 'true')
        return true

    if (text == 'false')
        return false

    throw new IllegalArgumentException("Invalid boolean value: '${text}'.")
}

def numerate(ch) 
{
    def counter = new java.util.concurrent.atomic.AtomicInteger(-1)

    return ch.map { sublist ->
        tuple(counter.incrementAndGet(), sublist)
    }
}

def toArray(inputs) 
{
    if (inputs == null)
        return null

    List values = inputs instanceof Collection? inputs.toList(): [inputs]

    if (values.isEmpty())
        return []

    if (!values.any { it instanceof groovyx.gpars.dataflow.DataflowReadChannel })
        return values

    def result = toChannel(values[0]).map { value ->
        [items: [value]]
    }

    values.drop(1).each { input ->
        result = result
            .combine(toChannel(input))
            .map { box, value ->
                [items: box.items + [value]]
            }
    }
    return result.map { box -> box.items }
}

def combineAll(inputs) 
{
    if (inputs.size() == 0)
        return Channel.empty()
    
    return inputs.drop(1).inject(toChannel(inputs[0])) { result, input -> result.combine(toChannel(input)) }
}

def wdl_to_string(value) 
{
    if (value == null)
        return null

    if (value instanceof Float || value instanceof Double || value instanceof BigDecimal)
        return String.format(Locale.US, "%.6f", value as BigDecimal)

    if (value instanceof Boolean)
        return value.toString()

    return value.toString()
}

def quote_wdl(values) 
{
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "\"${s}\""
    }
}

def squote_wdl(values) 
{
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "\'${s}\'"
    }
}

def sep_wdl2(delimiter, values) 
{
    values.collect { wdl_to_string(it) }.join(delimiter)
}

// ------------------------------------
// as_pairs(Map[K,V]) -> Array[Pair[K,V]]
// WDL: {"a":1,"b":2} -> [["a",1],["b",2]]
// ------------------------------------
def as_pairs_wdl(map) 
{
    map.collect { key, value -> pair(key, value) }
}

def as_map_wdl(array) 
{
    def result = [:]

    array.each { item ->
        def key
        def value

        if (item instanceof Map) 
        {
            key = item.left
            value = item.right
        } 
        else 
        {
            key = item[0]
            value = item[1]
        }

        if (key == null)
            throw new IllegalArgumentException("as_map() got pair with null left/key: ${item}")

        result[key] = value
    }
    return result
}

def keys_wdl(map) 
{
    if (!(map instanceof Map))
        throw new IllegalArgumentException("keys() expects Map, received: ${map?.getClass()?.name}")
    map.keySet().toList()
}

def values_wdl(map) 
{
    if (!(map instanceof Map))
        throw new IllegalArgumentException("values() expects Map, received: ${map?.getClass()?.name}")
    return map.values().toList()
}

def contains_key_wdl(value, key) 
{
    if (!(value instanceof Map))
        throw new IllegalArgumentException("contains_key() expects Map/Object, received: ${value?.getClass()?.name}")

    if (key instanceof Collection)
    {
        def current = value

        return key.every { part ->
            if (!(current instanceof Map))
                return false
 
            if (!current.containsKey(part))
                return false

            current = current[part]
            return true
        }
    }
    return value.containsKey(key)
}


def zip_wdl(a, b) 
{
    if (a.size() != b.size()) 
        throw new IllegalArgumentException( "zip(): arrays must have equal size (${a.size()} != ${b.size()})")

    (0..<a.size()).collect { i ->
        [left: a[i], right: b[i]]
    }
}

def floor_wdl(val) 
{
    return Math.floor(val as Double).intValue()
}

def round_wdl(val) 
{
    return Math.round(val as Double).intValue()
}

// ------------------------------------
// write_json(X) -> File
// Writes JSON and returns path
// ------------------------------------
def write_json_wdl(value, fileName) 
{
    def file = new File(fileName)
    file.text = groovy.json.JsonOutput.toJson(value)
    return file.path
}

def write_json_wdl(value) 
{
    def file = File.createTempFile("wdl_", ".json")
    file.text = addSpacesOutsideStrings(groovy.json.JsonOutput.toJson(value))
    return file.path
}

def read_json_wdl(path) 
{
    if (path == null)
        return null

    def file = path instanceof File ? path : new File(path.toString())
    return new groovy.json.JsonSlurper().parse(file)
}

def addSpacesOutsideStrings(String json)
{
    def out = new StringBuilder()

    boolean inString = false
    boolean escaped = false

    (0..<json.length()).each { i ->
        char c = json.charAt(i)

        if (escaped)
        {
            out.append(c)
            escaped = false
        }
        else if (c == '\\' as char && inString)
        {
            out.append(c)
            escaped = true
        }
        else if (c == '"' as char)
        {
            inString = !inString
            out.append(c)
        }
        else if (!inString && (c == ':' as char || c == ',' as char))
        {
            out.append(c).append(' ')
        }
        else
        {
            out.append(c)
        }
    }

    return out.toString()
}


def write_map_wdl(mapp) 
{
    def file = File.createTempFile("map_", ".tsv")

    file.text = mapp.collect { key, value ->
        "${key}\t${value}"
    }.join('\n')

    return file
}

def prefix_wdl(prefixValue, values)
{
    if (!(values instanceof Collection) && !values.getClass().isArray())
        throw new IllegalArgumentException("prefix() expects Array as second argument, got: " +values.getClass().getName())

    def items = values instanceof Collection? values: values.toList()

    return items.collect { 
		def s = wdl_to_string(it)
        s == null ? null : "${prefixValue}${s}"
    }
}

def suffix_wdl(suffixValue, values) 
{
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "${s}${suffixValue}"
    }
}

def read_lines_wdl2(filePath)
{
    if (filePath == null || filePath == "NO_VALUE")
        return "NO_VALUE"

    if (filePath instanceof groovyx.gpars.dataflow.DataflowReadChannel)
        return filePath.map { f -> read_lines_wdl(f) }

    if (filePath instanceof groovyx.gpars.dataflow.DataflowVariable)
        return read_lines_wdl(filePath.val)

    return new File(filePath.toString()).readLines()
}

def read_lines_wdl(filePath)
{
    if (filePath == null || filePath == "NO_VALUE")
        return "NO_VALUE"

    if (filePath instanceof groovyx.gpars.dataflow.DataflowReadChannel)
        return filePath.map { f -> read_lines_wdl(f) }

    if (filePath instanceof groovyx.gpars.dataflow.DataflowVariable)
        return read_lines_wdl(filePath.val)

    if (filePath instanceof File)
        return filePath.readLines()

    if (filePath instanceof java.nio.file.Path)
    {
        java.nio.file.Path realPath = filePath.toRealPath()
        return new File(realPath.toString()).readLines()
    }

    return new File(filePath.toString()).readLines()
}

def write_lines_wdl(value) 
{
    def file = File.createTempFile("wdl_", ".txt")
    if (value.isEmpty())
    {
        file.text = ""
    }
    else
    {
        file.text = value.collect { v -> wdl_to_string(v) }.join('\n') + '\n'
    }
    return file.path
}

def write_lines_bash(List<String> lines, String fileName) 
{
    String content = lines.join('\n') + '\n'
    String escapedContent = content.replace("'", "'\"'\"'")
    String escapedFileName = fileName.replace("'", "'\"'\"'")
    return "printf '%s' '${escapedContent}' > '${escapedFileName}'"
}


def collect_by_key_wdl(pairs) 
{
    def result = [:].withDefault { [] }

    pairs.each { p ->
        def key = p instanceof Map ? p.left : p[0]
        def value = p instanceof Map ? p.right : p[1]

        result[key] << value
    }
    return result
}

def size_wdl(value, unit = 'B')
{
    double bytes

    if (value instanceof Collection)
    {
        bytes = value.sum { new File(it.toString()).length() ?: 0 } as double
    }
    else
    {
        bytes = new File(value.toString()).length() as double
    }

    if (unit == 'B')
        return bytes

    if (unit == 'K')
        return bytes / 1_000d

    if (unit == 'M')
        return bytes / 1_000_000d

    if (unit == 'G')
        return bytes / 1_000_000_000d

    if (unit == 'T')
        return bytes / 1_000_000_000_000d

    if (unit == 'Ki')
        return bytes / 1024d

    if (unit == 'Mi')
        return bytes / (1024d * 1024d)

    if (unit == 'Gi')
        return bytes / (1024d * 1024d * 1024d)

    if (unit == 'Ti')
        return bytes / (1024d * 1024d * 1024d * 1024d)

    throw new IllegalArgumentException(
        "Unsupported size unit: '${unit}'"
    )
}
 
def size_bash(pathExpr, unit = 'B')
{
    def divisor

    if (unit == 'B')
        divisor = '1'

    else if (unit == 'K')
        divisor = '1000'

    else if (unit == 'M')
        divisor = '1000000'

    else if (unit == 'G')
        divisor = '1000000000'

    else if (unit == 'Ki')
        divisor = '1024'

    else if (unit == 'Mi')
        divisor = '1024*1024'

    else if (unit == 'Gi')
        divisor = '1024*1024*1024'
    else
        throw new IllegalArgumentException("Unsupported size unit: '${unit}'")    
    return "\$(bytes=\$(wc -c < ${pathExpr}); awk -v n=\"\$bytes\" 'BEGIN{printf \"%.6f\", n/(${divisor})}')"
}

def read_tsv_wdl(filePath) 
 {
    def file = new File(filePath.toString())

    if (!file.exists())
        throw new IllegalArgumentException("File does not exist: ${filePath}")

    return file.readLines().collect { line ->
        line.split('\t', -1).toList()
    }
}

def write_tsv_wdl(value, fileName) 
{
    def file = new File(fileName)
    file.text = value.collect { row -> row.collect { cell -> wdl_to_string(cell) }.join('\t') }.join('\n') + '\n'
    return file.path
}

def write_tsv_wdl(value) 
{
    def file = File.createTempFile("wdl_", ".tsv")
    file.text = value.collect { row -> row.collect { cell -> wdl_to_string(cell) }.join('\t')}.join('\n') + '\n'
    return file.path
}

def cross_wdl(left, right)
{
    def result = []

    left.each { l ->
        right.each { r ->
            result << pair(l, r)
        }
    }

    return result
}

def transpose_wdl(array)
{
    if (array == null || array.size() == 0)
        return []

    def width = array[0].size()
    def result = []

    (0..<width).each { i ->
        def row = []

        (0..<array.size()).each { j ->
            row << array[j][i]
        }

        result << row
    }

    return result
}

def contains_wdl(array, value) 
{
    if (array == null)
        return false
    return array.contains(value)
}

def unzip_wdl(array) 
{
    if (array == null || array.isEmpty()) {
        return [[], []]
    }

    [
        array.collect { it.left },
        array.collect { it.right }
    ]
}

def flatten_wdl(value) 
{
    if (value == null)
        return null
    return value.flatten()
}

def range_wdl(n) 
{
    if (n == null)
        return null

    if (!(n instanceof Integer || n instanceof Long || n instanceof BigInteger))
        throw new IllegalArgumentException("range() expects Int, got ${n.getClass().getName()}: ${n}")

    int size = n as int

    if (size < 0)
        throw new IllegalArgumentException("range() argument must be non-negative: ${n}")

    return (0..<size).collect { it as Integer }
}

def length_wdl(value) 
{
    if (value == null)
        return 0

    if (value instanceof Collection)
        return value.size()

    if (value instanceof Map)
        return value.size()

    if (value.getClass().isArray())
        return value.length

    if (value instanceof CharSequence)
        return value.length()

    throw new IllegalArgumentException("length() is not defined for ${value.getClass().name}")
}

def saveFile(ch, path, name)
{
    ch.subscribe { f -> 
	   java.nio.file.Files.createDirectories(java.nio.file.Path.of(path))
       java.nio.file.Files.copy(
          f instanceof java.nio.file.Path ? f : f.toPath(),
          java.nio.file.Path.of("{path}/${name}"),
          java.nio.file.StandardCopyOption.REPLACE_EXISTING
       )
    }
}

def saveOutput2(ch, outputDir, name) 
{
    ch.subscribe { value ->
        java.nio.file.Path targetDir = java.nio.file.Path.of(outputDir)
        java.nio.file.Files.createDirectories(targetDir)

        if (value instanceof java.nio.file.Path || value instanceof File)
        {
            copyOutputFile2(value, targetDir, name)
        }
        else if (value instanceof Collection && value.every { it instanceof java.nio.file.Path || it instanceof File })
        {
            value.eachWithIndex { file, index ->
                Path source = file instanceof Path
                    ? file
                    : file.toPath()

                String fileName = source.fileName.toString()
                java.nio.file.Files.copy(source, targetDir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
        }
        else 
        {
            java.nio.file.Files.writeString(targetDir.resolve("${name}.json"), groovy.json.JsonOutput.toJson(normalizeOutputValue(value)) )
        }
    }
}

def copyOutputFile2(value, java.nio.file.Path targetDir, String name) 
{
    java.nio.file.Path source = value instanceof java.nio.file.Path ? value: value.toPath()
    java.nio.file.Files.copy(source, targetDir.resolve(name), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
}

def normalizeOutputValue(value) 
{
    if (value instanceof java.nio.file.Path)
        return value.toString()

    if (value instanceof File)
        return value.toString()

    if (value instanceof Map)
        return value.collectEntries { key, item ->
            [(key): normalizeOutputValue(item)]
        }

    if (value instanceof Collection)
        return value.collect { normalizeOutputValue(it) }

    return value
}

def pair(left, right) 
{
    return [left: left, right: right]
}

def stringify_wdl(x) 
{
    if (x instanceof List)
        return '[' + x.collect { stringify_wdl(it) }.join(', ') + ']'
    if (x instanceof String)
        return '"' + x + '"'
    return x == null ? null : x.toString()
}


def saveOutputs(outputs, outputDirectory, isWSL)
{
    if (outputs.isEmpty())
        return

    def values = new java.util.concurrent.ConcurrentHashMap()
    def counter = new java.util.concurrent.atomic.AtomicInteger(0)

    outputs.each { output ->

        output.channel.subscribe { value ->

            values[output.name] = value

            if (counter.incrementAndGet() == outputs.size())
            {
                def outputDir = java.nio.file.Path.of(outputDirectory)
                    .toAbsolutePath()
                    .normalize()

                java.nio.file.Files.createDirectories(outputDir)

                def result = new LinkedHashMap()

                outputs.each { item ->

                    result[item.name] = [
                        type: item.type,
                        value: serializeOutputValue(
                            values[item.name],
                            outputDir,
                            isWSL
                        )
                    ]
                }

                def json = groovy.json.JsonOutput.prettyPrint(
                    groovy.json.JsonOutput.toJson(result)
                )

                java.nio.file.Files.writeString(
                    outputDir.resolve("outputs.json"),
                    json
                )
            }
        }
    }
}


//@Field
//private final Object SAVE_OUTPUT_LOCK = new Object()


/**
 * Saves a workflow output to outputs.json.
 *
 * publishDirectory:
 *   Non-empty when the output file was copied by process publishDir.
 *
 *   Empty or null when the output is an existing input/workflow file
 *   and its real path must be written to outputs.json.
 *
 * outputDirectory:
 *   Directory containing outputs.json.
 */
def saveOutput(channel, String outputType, String outputName, String outputDirectory, boolean isWSL) 
{
    channel.subscribe { value ->

        java.nio.file.Path outputDir = java.nio.file.Path.of(outputDirectory).toAbsolutePath().normalize()

        java.nio.file.Files.createDirectories(outputDir)

        def serializedValue = serializeOutputValue( value, outputDir, isWSL)

        def result = [
            type: outputType,
            value: serializedValue
        ]

        updateOutputsJson(outputDir.resolve("outputs.json"), outputName, result)
    }
}

def serializeOutputValue2(def value, java.nio.file.Path outputDir, boolean isWSL) 
{
    if (value == null)
        return null

    /*
     * Nextflow TaskPath implements java.nio.file.Path.
     */
    if (value instanceof java.nio.file.Path)
    {
        java.nio.file.Path path = value.toAbsolutePath().normalize()

        /*
         * WDL Directory output is represented by its recursive listing,
         * not by a string path.
         */
        if (java.nio.file.Files.isDirectory(path))
            return serializeDirectory(path)

        return serializePath(path, isWSL)
    }

    if (value instanceof File)
    {
        java.nio.file.Path path = value.toPath().toAbsolutePath().normalize()

        if (java.nio.file.Files.isDirectory(path))
            return serializeDirectory(path)

        return serializePath(path, isWSL)
    }

    if (value instanceof Map)
    {
        Map result = new LinkedHashMap()
        value.each { key, item -> result[String.valueOf(key)] = serializeOutputValue(item, outputDir, isWSL) }
        return result
    }

    if (value instanceof Collection)
    {
        return value.collect { item -> serializeOutputValue(item, outputDir, isWSL ) }
    }

    if (value.getClass().isArray())
    {
        List result = []
        int length = java.lang.reflect.Array.getLength(value)
        (0..<length).each { i ->
            result.add(serializeOutputValue(java.lang.reflect.Array.get(value, i), outputDir, isWSL))
        }
        return result
    }

    if (value instanceof Number || value instanceof CharSequence || value instanceof Boolean)
        return value

    return value.toString()
}

String serializePath(java.nio.file.Path path, boolean isWSL) 
{
    String p = path.toAbsolutePath().normalize().toString()
    if (isWSL && p ==~ /^\/mnt\/[a-zA-Z]\/.*/)
        return p.substring(5, 6).toUpperCase() + ":" + p.substring(6)
    return p
}

/**
 * Returns the expected location of a file copied by process publishDir.
 *
 * taskFile:
 *   /project/work/ab/cd/output.txt
 *
 * publishDir:
 *   /project/results/workflow/output/process
 *
 * outputDir:
 *   /project/results/workflow/output
 *
 * result:
 *   process/output.txt
 */
String publishedFilePath(java.nio.file.Path taskFile, java.nio.file.Path publishDir, java.nio.file.Path outputDir) 
{
    /*
     * publishDir receives the task output under its file name.
     * Using taskFile.fileName is intentional here because taskFile
     * usually points into the Nextflow work directory.
     */
    java.nio.file.Path publishedFile = publishDir.resolve(taskFile.fileName.toString()).toAbsolutePath().normalize()
    return relativeOrAbsolutePath(publishedFile, outputDir)
}


/**
 * Returns the real path of an existing workflow/input file.
 *
 * Unlike publishedFilePath(), this method must retain the complete
 * original path and must not reduce it to fileName.
 *
 * For example:
 *
 * file:
 *   /project/tests/basic_select_first/basic_select_first.json
 *
 * outputDir:
 *   /project/results/select_first/output
 *
 * result:
 *   ../../../tests/basic_select_first/basic_select_first.json
 */
String existingFilePath(java.nio.file.Path file, Path outputDir) 
{
    java.nio.file.Path absoluteFile = file.toAbsolutePath().normalize()
    return relativeOrAbsolutePath(absoluteFile,outputDir)
}


/**
 * Converts an absolute file path into a path relative to the directory
 * containing outputs.json.
 *
 * Falls back to an absolute path when relativization is impossible,
 * for example when paths are on different Windows drives.
 */
String relativeOrAbsolutePath(java.nio.file.Path file, java.nio.file.Path outputDir) 
{
    java.nio.file.Path absoluteFile = file.toAbsolutePath().normalize()

    java.nio.file.Path absoluteOutputDir = outputDir.toAbsolutePath().normalize()

    try
    {
        java.nio.file.Path relativePath = absoluteOutputDir.relativize(absoluteFile)
        return normalizeJsonPath(relativePath)
    }
    catch (IllegalArgumentException ignored)
    {
        return normalizeJsonPath(absoluteFile)
    }
}

/**
 * Reads the existing outputs.json, adds or replaces one output,
 * and atomically writes the complete JSON object.
 */
def updateOutputsJson(jsonFile, outputName, outputValue)
{
    def outputs = new LinkedHashMap()

    if (java.nio.file.Files.exists(jsonFile) && java.nio.file.Files.size(jsonFile) > 0)
    {
        String currentJson = java.nio.file.Files.readString(jsonFile)

        def parsed = new groovy.json.JsonSlurper().parseText(currentJson)

        if (!(parsed instanceof Map))
            throw new IllegalStateException("Output manifest must contain a JSON object: ${jsonFile}")

        parsed.each { key, value ->
            outputs[String.valueOf(key)] = value
        }
    }

    outputs[outputName] = outputValue

    String json = groovy.json.JsonOutput.prettyPrint(
        groovy.json.JsonOutput.toJson(outputs)
    )

    java.nio.file.Files.createDirectories(jsonFile.parent)

    java.nio.file.Files.writeString(jsonFile, json)
}

/**
 * Converts a WDL Directory output into the directory-listing structure
 * expected by the conformance tests.
 */
Map serializeDirectory(java.nio.file.Path directory) 
{
    return [ listing: directoryListing(directory) ]
}

/**
 * Recursively lists direct children of a directory.
 *
 * Children are sorted by basename to make the result deterministic.
 */
def directoryListing(java.nio.file.Path directory)
{
    def children = directory.toFile().listFiles().toList()

    children = children.sort { left, right ->
        left.name <=> right.name
    }

    return children.collect { child ->
        if (child.isDirectory())
        {
            return [
                type    : "Directory",
                basename: child.name,
                listing : directoryListing(child.toPath())
            ]
        }

        return [
            type    : "File",
            basename: child.name
        ]
    }
}

/**
 * JSON file paths always use forward slashes,
 * including when Nextflow is launched on Windows.
 */
String normalizeJsonPath(java.nio.file.Path path) 
{
    return path.toString().replace('\\', '/')
}

def sep_wdl(separator, values, workDir = null) 
{
    if (values == null || values == "NO_VALUE")
        return ""

    List items

    if (values instanceof File || values instanceof java.nio.file.Path)
    {
        items = [values]
    }
    else if (values instanceof Collection)
    {
        items = values.collect { it }
    }
    else if (values.getClass().isArray())
    {
        items = values.toList()
    }
    else if (values instanceof Iterable)
    {
        items = values.collect { it }
    }
    else
    {
        throw new IllegalArgumentException("sep_wdl expects an iterable or array, received: " + values.getClass().getName())
    }
    String actualSeparator = separator == null ? "" : separator.toString()

    return items.collect { value ->

        if (value instanceof CharSequence)
            return value.toString()

        return sep_element_wdl(value, workDir)

    }.join(actualSeparator)
}

def sep_element_wdl(value, workDir = null) 
{
    if (value == null || value == "NO_VALUE")
        return ""

    String className = value.getClass().getName()

    if ( value instanceof File || value instanceof java.nio.file.Path || className == "nextflow.processor.TaskPath" || className.endsWith(".TaskPath") )
    {
        String valuePath = value.toString()

        if (valuePath.startsWith("/"))
            return valuePath

        /*
         * task.workDir is not yet available here.
         * $PWD will be expanded by Bash inside the task work directory.
         */
        return '$PWD/' + valuePath
    }
    return stringify_wdl(value)
}

def validateObjectPrimitiveValues(object, functionName)
{
    object.each { key, value ->
        if (value instanceof Map || value instanceof Collection || (value != null && value.getClass().isArray()))
            throw new IllegalArgumentException( "${functionName}: compound value is not allowed for field '${key}'")
    }
}

def collectScatterValues(values, dimensions, condition)
{
    def valueIndex = [0]
    return collectScatterLevel( 0, [], values, dimensions, condition, valueIndex)
}

def collectScatterLevel( level, args, values, dimensions, condition, valueIndex)
{
    if (level == dimensions.size())
    {
        if (condition.call(args))
        {
            def result = values[valueIndex[0]]
            valueIndex[0] = valueIndex[0] + 1
            return result
        }
        return null
    }
    def dimension = dimensions[level].call(args)
    return dimension.collect { item -> collectScatterLevel( level + 1, args + [item], values,  dimensions, condition, valueIndex ) }
}

def materializeSymlink(java.nio.file.Path path, java.nio.file.Path outputDir)
{
    if (!java.nio.file.Files.isSymbolicLink(path))
        return path

    java.nio.file.Path target = java.nio.file.Files.readSymbolicLink(path)

    if (!target.isAbsolute())
        target = path.getParent().resolve(target)

    target = target.toAbsolutePath().normalize()
    java.nio.file.Path destination = outputDir.resolve(path.getFileName().toString())
    java.nio.file.Files.copy( target, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    return destination
}

def serializeOutputValue(
    def value,
    java.nio.file.Path outputDir,
    boolean isWSL
)
{
    if (value == null)
        return null

    if (value instanceof java.nio.file.Path)
    {
        java.nio.file.Path path =
            value.toAbsolutePath().normalize()

        if (java.nio.file.Files.isDirectory(path))
            return serializeDirectory(path)

        path = materializeSymlink(path, outputDir)

        return serializePath(path, isWSL)
    }

    if (value instanceof File)
    {
        java.nio.file.Path path =
            value.toPath().toAbsolutePath().normalize()

        if (java.nio.file.Files.isDirectory(path))
            return serializeDirectory(path)

        path = materializeSymlink(path, outputDir)

        return serializePath(path, isWSL)
    }

    if (value instanceof Map)
    {
        Map result = new LinkedHashMap()

        value.each { key, item ->
            result[String.valueOf(key)] =
                serializeOutputValue(item, outputDir, isWSL)
        }

        return result
    }

    if (value instanceof Collection)
    {
        return value.collect { item ->
            serializeOutputValue(item, outputDir, isWSL)
        }
    }

    if (value.getClass().isArray())
    {
        List result = []

        int length =
            java.lang.reflect.Array.getLength(value)

        (0..<length).each { i ->
            result.add(
                serializeOutputValue(
                    java.lang.reflect.Array.get(value, i),
                    outputDir,
                    isWSL
                )
            )
        }

        return result
    }

    if (
        value instanceof Number ||
        value instanceof CharSequence ||
        value instanceof Boolean
    )
        return value

    return value.toString()
}