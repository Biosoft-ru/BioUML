import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

def fileOrNull(value)
{
    if (value == null || value == "NO_VALUE")
        return null

    return file(value)
}

def orNull(x) {
    return x == null ? "NO_VALUE" : x
}

def basename_wdl(path, suffix = null) {
    def name = new File(path.toString()).getName()

    if (suffix != null && name.endsWith(suffix))
        return name.substring(0, name.length() - suffix.length())

    return name
}

def sub_wdl(input, pattern, replacement) {
    return input.toString().replaceAll(pattern, replacement)
}

def ceil_wdl(val) {
    return Math.ceil(val as Double).intValue()
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

def getDefault(value, defaultValue) {
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
    if (value instanceof DataflowReadChannel)
    {
        return value.map { items ->
            select_first_value_wdl(items)
        }
    }

    return select_first_value_wdl(value)
}


private def select_first_value_wdl(value)
{
    if (!(value instanceof Collection))
    {
        throw new IllegalArgumentException(
            "select_first_wdl expects an array, received: " +
            "${value?.getClass()?.name}"
        )
    }

    for (def item : value)
    {
        if (item != null && item != "NO_VALUE")
            return item
    }

    throw new IllegalArgumentException(
        "select_first_wdl: array contains no defined values"
    )
}

def select_all_wdl(array) {
    return array.findAll { it != null && it != "NO_VALUE"}
}

def defined_wdl(val) {
    return val != null && val != "NO_VALUE"
}

def read_string_wdl(filePath) {
    if (filePath == null)
        return null

    if (filePath instanceof java.nio.file.Path)
        return filePath.toFile().text.trim()

    if (filePath instanceof File)
        return filePath.text.trim()

    return new File(filePath.toString()).text.trim()
}

def read_map_wdl(filePath) {
    def file = filePath instanceof File
            ? filePath
            : new File(filePath.toString())

    if (!file.exists()) {
        file = new File(task.workDir.toString(), filePath.toString())
    }

    def result = [:]

    file.eachLine { line ->
        if (line.trim().isEmpty()) {
            return
        }

        def parts = line.split('\t', 2)

        if (parts.size() != 2) {
            throw new IllegalArgumentException(
                    "Invalid WDL map line, expected two tab-separated columns: '${line}'"
            )
        }

        result[parts[0]] = parts[1]
    }

    return result
}

def read_int_wdl(filePath) {
    return new File(filePath).text.trim() as Integer
}

def read_lines_groovy(filePath) {
    return "\$(cat ${filePath})"
}

def read_int_groovy(filePath) {
    return "\$(cat ${filePath} | tr -d '\\r\\n')"
}

def read_string_groovy(filePath) {
    return "\$(cat ${filePath})"
}

def read_float_groovy(filePath) {
    return "\$(cat ${filePath} | tr -d '\\r\\n')"
}

def read_boolean_groovy(filePath) {
    return "\$(cat ${filePath} | tr -d '\\r\\n')"
}

def read_float_wdl(filePath) {
    return new File(filePath).text.trim() as Float
}

def read_boolean_wdl(filePath) {
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

import groovyx.gpars.dataflow.DataflowReadChannel

def toArray(inputs)
{
    if (inputs == null)
        return null

    List values = inputs instanceof Collection
        ? inputs.toList()
        : [inputs]

    if (values.isEmpty())
        return []

    if (!values.any { it instanceof DataflowReadChannel })
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

def combineAll(inputs) {
    if (inputs.size() == 0) {
        return Channel.empty()
    }
    
    return inputs.drop(1).inject(toChannel(inputs[0])) { result, input -> result.combine(toChannel(input)) }
}
def wdl_to_string(value) {
    if (value == null)
        return null

    if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
        return String.format(Locale.US, "%.6f", value as BigDecimal)
    }

    if (value instanceof Boolean) {
        return value.toString()
    }

    return value.toString()
}

def quote_wdl(values) {
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "\"${s}\""
    }
}

def squote_wdl(values) {
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "\'${s}\'"
    }
}

def sep_wdl(delimiter, values) {
    values.collect { wdl_to_string(it) }
          .join(delimiter)
}

// ------------------------------------
// as_pairs(Map[K,V]) -> Array[Pair[K,V]]
// WDL: {"a":1,"b":2} -> [["a",1],["b",2]]
// ------------------------------------
def as_pairs_wdl(map) {
    map.collect { key, value -> pair(key, value) }
}

def as_map_wdl(array) {
    def result = [:]

    array.each { item ->
        def key
        def value

        if (item instanceof Map) {
            key = item.left
            value = item.right
        } else {
            key = item[0]
            value = item[1]
        }

        if (key == null) {
            throw new IllegalArgumentException("as_map() got pair with null left/key: ${item}")
        }

        result[key] = value
    }

    return result
}

// ------------------------------------
// keys(Map[K,V]) -> Array[K]
// ------------------------------------
def keys_wdl(map) {
    map.keySet().toList()
}


def zip_wdl(a, b) {
    if (a.size() != b.size()) {
        throw new IllegalArgumentException(
            "zip(): arrays must have equal size " +
            "(${a.size()} != ${b.size()})"
        )
    }

    (0..<a.size()).collect { i ->
        [left: a[i], right: b[i]]
    }
}

def floor_wdl(val) {
    return Math.floor(val as Double).intValue()
}

def round_wdl(val) {
    return Math.round(val as Double).intValue()
}

// ------------------------------------
// write_json(X) -> File
// Writes JSON and returns path
// ------------------------------------
def write_json_wdl(value, fileName) {
    def file = new File(fileName)
    file.text = JsonOutput.toJson(value)
    return file.path
}

def write_json_wdl(value) {
    def file = File.createTempFile("wdl_", ".json")
    file.text = addSpacesOutsideStrings(JsonOutput.toJson(value))
    return file.path
}

def read_json_wdl(path) {
    if (path == null)
        return null

    def file = path instanceof File ? path : new File(path.toString())
    return new JsonSlurper().parse(file)
}

def addSpacesOutsideStrings(String json) {
    def out = new StringBuilder()
    boolean inString = false
    boolean escaped = false

    for (int i = 0; i < json.length(); i++) {
        char c = json.charAt(i)

        if (escaped) {
            out.append(c)
            escaped = false
            continue
        }

        if (c == '\\' as char && inString) {
            out.append(c)
            escaped = true
            continue
        }

        if (c == '"' as char) {
            inString = !inString
            out.append(c)
            continue
        }

        if (!inString && (c == ':' as char || c == ',' as char)) {
            out.append(c).append(' ')
            continue
        }

        out.append(c)
    }

    return out.toString()
}


def write_map_wdl(map)
{
    def file = File.createTempFile("map_", ".tsv")

    file.text = map.collect { key, value ->
        "${key}\t${value}"
    }.join('\n')

    return file
}

def prefix_wdl(prefixValue, values) {
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "${prefixValue}${s}"
    }
}

def suffix_wdl(suffixValue, values) {
    values.collect {
        def s = wdl_to_string(it)
        s == null ? null : "${s}${suffixValue}"
    }
}

def read_lines_wdl(filePath) {
    if (filePath == null || filePath == "NO_VALUE")
        return "NO_VALUE"

    if (filePath instanceof groovyx.gpars.dataflow.DataflowReadChannel)
        return filePath.map { f -> read_lines_wdl(f) }

    if (filePath instanceof groovyx.gpars.dataflow.DataflowVariable)
        return read_lines_wdl(filePath.val)

    if (filePath instanceof String)
        return new File(filePath).readLines()

    return new String(filePath.readBytes()).readLines()
}

def write_lines_wdl(value) {
    def file = File.createTempFile("wdl_", ".txt")

    file.text = value
        .collect { v -> wdl_to_string(v) }
        .join('\n') + '\n'

    return file.path
}


def collect_by_key_wdl(pairs) {
    def result = [:].withDefault { [] }

    pairs.each { p ->
        def key = p instanceof Map ? p.left : p[0]
        def value = p instanceof Map ? p.right : p[1]

        result[key] << value
    }

    return result
}

def size_wdl2(value, unit = 'B') {
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
 
def size_wdl(pathExpr, unit = 'B')
{
    def divisor

    switch(unit)
    {
        case 'B':
            divisor = '1'
            break
        case 'KB':
            divisor = '1000'
            break
        case 'MB':
            divisor = '1000000'
            break
        case 'GB':
            divisor = '1000000000'
            break
        case 'KiB':
            divisor = '1024'
            break
        case 'MiB':
            divisor = '1024*1024'
            break
        case 'GiB':
            divisor = '1024*1024*1024'
            break
        default:
            throw new IllegalArgumentException("Unsupported size unit: '${unit}'")
    }

    return "\$(bytes=\$(wc -c < ${pathExpr}); awk -v n=\"\$bytes\" 'BEGIN{printf \"%.6f\", n/(${divisor})}')"
}

 def read_tsv_wdl(filePath) {
    def file = new File(filePath.toString())

    if (!file.exists())
        throw new IllegalArgumentException("File does not exist: ${filePath}")

    return file.readLines().collect { line ->
        line.split('\t', -1).toList()
    }
}

def write_tsv_wdl(value, fileName) {
    def file = new File(fileName)

    file.text = value.collect { row -> row.collect { cell -> wdl_to_string(cell) }.join('\t') }.join('\n') + '\n'

    return file.path
}

def write_tsv_wdl(value) {
    def file = File.createTempFile("wdl_", ".tsv")

    file.text = value.collect { row -> row.collect { cell -> wdl_to_string(cell) }.join('\t')}.join('\n') + '\n'

    return file.path
}

def cross_wdl(left, right) {
    def result = []
    for (l in left) {
        for (r in right) {
            result << pair(l, r)
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

def contains_wdl(array, value) {
    if (array == null)
        return false

    return array.contains(value)
}

def unzip_wdl(array) {
    if (array == null || array.isEmpty()) {
        return [[], []]
    }

    [
        array.collect { it.left },
        array.collect { it.right }
    ]
}

def flatten_wdl(value) {
    if (value == null) {
        return null
    }

    return value.flatten()
}

def range_wdl(n) {
    if (n == null)
        return null

    if (!(n instanceof Integer || n instanceof Long || n instanceof BigInteger)) {
        throw new IllegalArgumentException("range() expects Int, got ${n.getClass().getName()}: ${n}")
    }

    int size = n as int

    if (size < 0)
        throw new IllegalArgumentException("range() argument must be non-negative: ${n}")

    return (0..<size).collect { it as Integer }
}

def length_wdl(value) {
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

    throw new IllegalArgumentException(
        "length() is not defined for ${value.getClass().name}"
    )
}

def saveFile(ch, path, name)
{
    ch.subscribe { f -> 
	   Files.createDirectories(Path.of(path))
       Files.copy(
          f instanceof Path ? f : f.toPath(),
          Path.of("{path}/${name}"),
          StandardCopyOption.REPLACE_EXISTING
       )
    }
}

def saveOutput2(ch, outputDir, name) {
    ch.subscribe { value ->
        Path targetDir = Path.of(outputDir)
        Files.createDirectories(targetDir)

        if (value instanceof Path || value instanceof File) {
            copyOutputFile2(value, targetDir, name)
        }
        else if (value instanceof Collection &&
                 value.every { it instanceof Path || it instanceof File }) {
            value.eachWithIndex { file, index ->
                Path source = file instanceof Path
                    ? file
                    : file.toPath()

                String fileName = source.fileName.toString()

                Files.copy(
                    source,
                    targetDir.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
        else {
            Files.writeString(
                targetDir.resolve("${name}.json"),
                JsonOutput.toJson(normalizeOutputValue(value))
            )
        }
    }
}

def copyOutputFile2(value, Path targetDir, String name) {
    Path source = value instanceof Path
        ? value
        : value.toPath()

    Files.copy(
        source,
        targetDir.resolve(name),
        StandardCopyOption.REPLACE_EXISTING
    )
}

def normalizeOutputValue(value) {
    if (value instanceof Path)
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

def pair(left, right) {
    return [left: left, right: right]
}

def stringify_wdl(x) {
    if (x instanceof List)
        return '[' + x.collect { stringify_wdl(it) }.join(', ') + ']'
    if (x instanceof String)
        return '"' + x + '"'
    return x == null ? null : x.toString()
}

import groovy.transform.Field
import java.nio.file.AtomicMoveNotSupportedException

@Field
private final Object SAVE_OUTPUT_LOCK = new Object()


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
def saveOutput(
    channel,
    String publishDirectory,
    String outputDirectory,
    String outputName
)
{
    channel.subscribe { value ->

        Path outputDir = Path.of(outputDirectory)
            .toAbsolutePath()
            .normalize()

        Files.createDirectories(outputDir)

        /*
         * An empty string must not become Path.of(""), because that
         * represents the current working directory.
         */
        Path publishDir = null

        if (publishDirectory != null &&
            !publishDirectory.isBlank())
        {
            publishDir = Path.of(publishDirectory)
                .toAbsolutePath()
                .normalize()
        }

        def serializedValue = serializeOutputValue(
            value,
            publishDir,
            outputDir
        )

        updateOutputsJson(
            outputDir.resolve("outputs.json"),
            outputName,
            serializedValue
        )
    }
}


/**
 * Recursively converts a Nextflow/WDL output into a JSON-compatible value.
 */
private def serializeOutputValue(
    def value,
    Path publishDir,
    Path outputDir
)
{
    if (value == null)
    {
        return null
    }

    /*
     * Nextflow TaskPath implements java.nio.file.Path.
     */
    if (value instanceof Path)
    {
         Path path = ((Path)value)
        .toAbsolutePath()
        .normalize()

    /*
     * WDL Directory output is represented by its recursive listing,
     * not by a string path.
     */
    if (Files.isDirectory(path))
    {
        return serializeDirectory(path)
    }

    if (publishDir != null)
    {
        return publishedFilePath(
            path,
            publishDir,
            outputDir
        )
    }

    return existingFilePath(
        path,
        outputDir
    )
    }

    if (value instanceof File)
    {
        Path path = ((File)value)
        .toPath()
        .toAbsolutePath()
        .normalize()

    if (Files.isDirectory(path))
    {
        return serializeDirectory(path)
    }

    if (publishDir != null)
    {
        return publishedFilePath(
            path,
            publishDir,
            outputDir
        )
    }

    return existingFilePath(
        path,
        outputDir
    )
    }

    if (value instanceof Map)
    {
        Map result = new LinkedHashMap()

        value.each { key, item ->
            result[String.valueOf(key)] = serializeOutputValue(
                item,
                publishDir,
                outputDir
            )
        }

        return result
    }

    if (value instanceof Collection)
    {
        return value.collect { item ->
            serializeOutputValue(
                item,
                publishDir,
                outputDir
            )
        }
    }

    if (value.getClass().isArray())
    {
        List result = []

        int length = java.lang.reflect.Array.getLength(value)

        for (int i = 0; i < length; i++)
        {
            result.add(
                serializeOutputValue(
                    java.lang.reflect.Array.get(value, i),
                    publishDir,
                    outputDir
                )
            )
        }

        return result
    }

    if (value instanceof Number ||
        value instanceof CharSequence ||
        value instanceof Boolean)
    {
        return value
    }

    return value.toString()
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
private String publishedFilePath(
    Path taskFile,
    Path publishDir,
    Path outputDir
)
{
    /*
     * publishDir receives the task output under its file name.
     *
     * Using taskFile.fileName is intentional here because taskFile
     * usually points into the Nextflow work directory.
     */
    Path publishedFile = publishDir
        .resolve(taskFile.fileName.toString())
        .toAbsolutePath()
        .normalize()

    return relativeOrAbsolutePath(
        publishedFile,
        outputDir
    )
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
private String existingFilePath(
    Path file,
    Path outputDir
)
{
    Path absoluteFile = file
        .toAbsolutePath()
        .normalize()

    return relativeOrAbsolutePath(
        absoluteFile,
        outputDir
    )
}


/**
 * Converts an absolute file path into a path relative to the directory
 * containing outputs.json.
 *
 * Falls back to an absolute path when relativization is impossible,
 * for example when paths are on different Windows drives.
 */
private String relativeOrAbsolutePath(
    Path file,
    Path outputDir
)
{
    Path absoluteFile = file
        .toAbsolutePath()
        .normalize()

    Path absoluteOutputDir = outputDir
        .toAbsolutePath()
        .normalize()

    try
    {
        Path relativePath = absoluteOutputDir.relativize(
            absoluteFile
        )

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
private void updateOutputsJson(
    Path jsonFile,
    String outputName,
    def outputValue
)
{
    synchronized (SAVE_OUTPUT_LOCK)
    {
        Map<String, Object> outputs = new LinkedHashMap<>()

        if (Files.exists(jsonFile) &&
            Files.size(jsonFile) > 0)
        {
            String currentJson = Files.readString(jsonFile)

            def parsed = new JsonSlurper().parseText(
                currentJson
            )

            if (!(parsed instanceof Map))
            {
                throw new IllegalStateException(
                    "Output manifest must contain a JSON object: ${jsonFile}"
                )
            }

            parsed.each { key, value ->
                outputs[String.valueOf(key)] = value
            }
        }

        outputs[outputName] = outputValue

        String json = JsonOutput.prettyPrint(
            JsonOutput.toJson(outputs)
        )

        Files.createDirectories(jsonFile.parent)

        Path temporaryFile = Files.createTempFile(
            jsonFile.parent,
            jsonFile.fileName.toString(),
            ".tmp"
        )

        try
        {
            Files.writeString(
                temporaryFile,
                json
            )

            try
            {
                Files.move(
                    temporaryFile,
                    jsonFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            }
            catch (AtomicMoveNotSupportedException ignored)
            {
                Files.move(
                    temporaryFile,
                    jsonFile,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
        finally
        {
            Files.deleteIfExists(temporaryFile)
        }
    }
}

/**
 * Converts a WDL Directory output into the directory-listing structure
 * expected by the conformance tests.
 */
private Map serializeDirectory(Path directory)
{
    return [
        listing: directoryListing(directory)
    ]
}


/**
 * Recursively lists direct children of a directory.
 *
 * Children are sorted by basename to make the result deterministic.
 */
private List directoryListing(Path directory)
{
    List<Path> children

    def stream = Files.list(directory)

    try
    {
        children = stream
            .sorted { left, right ->
                left.fileName.toString() <=>
                    right.fileName.toString()
            }
            .toList()
    }
    finally
    {
        stream.close()
    }

    return children.collect { Path child ->

        if (Files.isDirectory(child))
        {
            return [
                type    : "Directory",
                basename: child.fileName.toString(),
                listing : directoryListing(child)
            ]
        }

        return [
            type    : "File",
            basename: child.fileName.toString()
        ]
    }
}


/**
 * JSON file paths always use forward slashes,
 * including when Nextflow is launched on Windows.
 */
private String normalizeJsonPath(Path path)
{
    return path.toString().replace('\\', '/')
}