package ru.biosoft.plugins.jri.rdirect;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableBoolean;

import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.access.script.NullScriptEnvironment;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl.BreakType;
import ru.biosoft.plugins.jri.RUtility;
import ru.biosoft.util.TextUtil;
import ru.biosoft.plugins.jri.rdirect.Message.MessageType;

public class RDirectSession implements AutoCloseable
{
    private static final int CHUNK_SIZE = 4000;
    private static final Pattern DEBUG_AT_PATTERN = Pattern.compile( "^((debug at (.*?)#(\\d+)|exiting from)\\: |debug: (.Internal|UseMethod)\\()" );
    private static final Pattern DEBUGGING_IN_PATTERN = Pattern.compile( "^debugging in: " );
    private static final Pattern DEBUG_BROWSE_PATTERN = Pattern.compile( "Browse\\[\\d+\\]\\> " );

    private static interface Printer
    {
        void print(String line);
    }

    private static class StringPrinter implements Printer
    {
        StringBuilder sb = new StringBuilder();

        @Override
        public void print(String line)
        {
            sb.append( line ).append('\n');
        }

        public StringPrinter chomp()
        {
            if(sb.length() > 0)
                sb.setLength( sb.length()-1 );
            return this;
        }

        @Override
        public String toString()
        {
            return sb.toString();
        }
    }

    private final RProcessLauncher launcher;
    private final String endMarker = "$BioUML$OutputEnd$"+System.currentTimeMillis();
    private ScriptEnvironment env;
    private boolean inDebugger = false;
    private int lineNum = 0;
    private boolean valid = true;
    private volatile boolean cancel = false;
    private final AtomicInteger nRef = new AtomicInteger();
    private final String version;

    private final Printer envPrint = line -> {
        env.print( line );
    };
    private final Printer envError = line -> {
        if(!line.isEmpty())
            env.error( line );
    };
    private static final Printer nullPrinter = line -> {};

    private RDirectSession(RProcessLauncher launcher)
    {
        this.launcher = launcher;
        this.env = new NullScriptEnvironment();
        addRef();
        evalSilent("options(prompt="+RWriter.getRString( endMarker+"\n" )+",keep.source=TRUE);.BioUML.savedPlots <<- list();require(rbiouml);");
        this.version = expr("getRversion()");
    }

    public String getVersion()
    {
        return version;
    }

    public boolean isR3()
    {
        return version != null && version.compareTo( "3." ) > 0;
    }

    public boolean isR3_1()
    {
        return version != null && version.compareTo( "3.1." ) > 0;
    }

    public void setEnvironment(ScriptEnvironment env)
    {
        this.env = env;
    }

    public void help(String expression)
    {
        String html = capture( "local({\n"
                + "f <- utils:::index.search(" + RWriter.getRString( expression )+", find.package(NULL,NULL))[1];\n"
                + "if(is.na(f)) {stop('No help found');}\n"
                + "tools::Rd2HTML(utils:::.getHelpFile(f));\n"
                + "})" );
        if(!html.isEmpty())
            env.showHtml(html);
    }

    public String getAsString(String varName)
    {
        StringPrinter result = new StringPrinter();
        StringPrinter errors = new StringPrinter();
        eval("write(as.character(get("+RWriter.getRString( varName )+")), stdout())", result, errors);
        String err = errors.toString();
        if(!err.trim().isEmpty())
            return null;
        return StringUtils.chomp( result.toString() );
    }

    public Object getAsObject(String varName)
    {
        StringPrinter result = new StringPrinter();
        StringPrinter errors = new StringPrinter();
        eval("write(class("+varName+"), stdout());write(length("+varName+"), stdout());", result, errors);
        String err = errors.toString();
        if(!err.trim().isEmpty())
            return null;
        String[] results = TextUtil.split( result.toString(), '\n' );
        if(results.length < 2)
            return null;
        String type = results[0];
        int length = Integer.parseInt( results[1] );
        switch(type)
        {
            case "numeric":
                return readDoubles(varName, length);
            case "integer":
                return readInts(varName, length);
            case "logical":
                return readBooleans(varName, length);
            case "matrix":
                return readMatrix(varName, length);
            case "list":
            case "data.frame":
                return readList(varName, length);
            case "character":
            default:
                return readStrings(varName, length);
        }
    }

    private Object readMatrix(String varName, int length)
    {
        StringPrinter result = new StringPrinter();
        StringPrinter errors = new StringPrinter();

        eval("write(dim("+varName+"), stdout());write(typeof("+varName+"), stdout());", result, errors);
        String err = errors.toString();
        if(!err.trim().isEmpty())
            return null;
        String[] results = result.toString().trim().split( "\\s+", 3 );
        if(results.length != 3)
            return null;
        int nRows = Integer.parseInt( results[0] );
        int nCols = Integer.parseInt( results[1] );
        String type = results[2];
        switch(type)
        {
            case "double":
            {
                double[][] m = new double[nRows][nCols];
                Object doubles = readDoubles("as.numeric(" + varName + ")", length);
                if(doubles instanceof double[])
                {
                    double[] v = (double[])doubles;
                    for(int j = 0; j < nCols; j++)
                        for(int i = 0; i < nRows; i++)
                            m[i][j] = v[j*nRows + i];
                }else
                    m[0][0] = (double)doubles;
                return m;
            }
            case "integer":
            {
                int[][] m = new int[nRows][nCols];
                Object ints = readInts("as.integer(" + varName + ")", length);
                if(ints instanceof int[])
                {
                    int[] v = (int[])ints;
                    for(int j = 0; j < nCols; j++)
                        for(int i = 0; i < nRows; i++)
                            m[i][j] = v[j*nRows + i];
                }else
                    m[0][0] = (int)ints;
                return m;
            }
            case "logical":
            {
                boolean[][] m = new boolean[nRows][nCols];
                Object bools = readInts("as.logical(" + varName + ")", length);
                if(bools instanceof boolean[])
                {
                    boolean[] v = (boolean[])bools;
                    for(int j = 0; j < nCols; j++)
                        for(int i = 0; i < nRows; i++)
                            m[i][j] = v[j*nRows + i];
                }else
                    m[0][0] = (boolean)bools;
                return m;
            }
            case "character":
            default:
            {
                String[][] m = new String[nRows][nCols];
                Object strings = readStrings("as.character(" + varName + ")", length);
                if(strings instanceof String[])
                {
                    String[] v = (String[])strings;
                    for(int j = 0; j < nCols; j++)
                        for(int i = 0; i < nRows; i++)
                            m[i][j] = v[j*nRows + i];
                }else
                    m[0][0] = (String)strings;
                return m;
            }
        }
    }

    private Object readStrings(String varName, int length)
    {
        StringPrinter result = new StringPrinter();
        StringPrinter errors = new StringPrinter();
        eval( "write(as.character("
                + "gsub(\"\\r\", \"\\\\\\\\r\", " // replace \r with \\r
                + "gsub(\"\\n\", \"\\\\\\\\n\", " // replace \n with \\n
                + "gsub(\"\\\\\\\\\", \"\\\\\\\\\\\\\\\\\", " + varName + ")))), stdout());", // replace \ with \\. Yeah, 16 slashes!
                result, errors );
        String err = errors.toString();
        if(!err.trim().isEmpty())
            return null;
        String[] results = StreamEx.split(result.chomp().toString(), '\n')
                .map( s -> s.replace( "\\n", "\n" ).replace( "\\r", "\r" ).replace( "\\\\", "\\" ) )
                .toArray( String[]::new );
        return results.length == 1 ? results[0] : results;
    }

    private Object readList(String varName, int length)
    {
        List<Object> result = new ArrayList<>(length);
        for(int i=1; i<=length; i++)
            result.add(getAsObject( varName+"[["+i+"]]" ));
        return result;
    }

    private Object readDoubles(String varName, int length)
    {
        StringPrinter result = new StringPrinter();
        StringPrinter errors = new StringPrinter();
        eval("write(as.character("+varName+"), stdout());", result, errors);
        String err = errors.toString();
        if(!err.trim().isEmpty())
            return null;
        double[] results = StreamEx.split(result.chomp().toString(), '\n').mapToDouble( this::parseDouble ).toArray();
        return results.length == 1 ? results[0] : results;
    }

    private Object readInts(String varName, int length)
    {
        StringPrinter result = new StringPrinter();
        StringPrinter errors = new StringPrinter();
        eval("write(as.character("+varName+"), stdout());", result, errors);
        String err = errors.toString();
        if(!err.trim().isEmpty())
            return null;
        int[] results = StreamEx.split(result.chomp().toString(), '\n').mapToInt( Integer::parseInt ).toArray();
        return results.length == 1 ? results[0] : results;
    }

    private Object readBooleans(String varName, int length)
    {
        StringPrinter result = new StringPrinter();
        StringPrinter errors = new StringPrinter();
        eval("write(as.character("+varName+"), stdout());", result, errors);
        String err = errors.toString();
        if(!err.trim().isEmpty())
            return null;
        boolean[] results = new boolean[length];
        String[] split = TextUtil.split( result.chomp().toString(), '\n');
        for(int i=0; i<results.length; i++)
            results[i] = Boolean.parseBoolean( split[i] );
        return results.length == 1 ? results[0] : results;
    }

    private double parseDouble(String s)
    {
        switch(s)
        {
            case "Inf":
                return Double.POSITIVE_INFINITY;
            case "-Inf":
                return Double.NEGATIVE_INFINITY;
            case "NaN":
                return Double.NaN;
            default:
                return Double.parseDouble( s );
        }
    }

    public void assign(String name, Object object)
    {
        if(!name.matches( "\\w+" ))
            throw new IllegalArgumentException( "Illegal name: "+name );
        StringWriter cmd = new StringWriter();
        cmd.write( name );
        cmd.write( " <- " );
        try
        {
            RWriter.writeObject( cmd, object );
        }
        catch( IOException e )
        {
            throw new InternalException( e );
        }
        cmd.write( ";" );
        eval(cmd.toString());
    }

    /**
     * @param expression expression (R code) to debug
     * @return true if debugger is initialized, false otherwise (e.g. parse error in expression)
     */
    public boolean initDebug(String expression)
    {
        String input = ".BioUML.debug.fn <- function() {"+expression+"};debug(.BioUML.debug.fn)";
        MutableBoolean hasError = new MutableBoolean( false );
        eval(input, envPrint, line -> {
            if(!line.isEmpty())
            {
                envError.print( line );
                hasError.setValue( true );
            }
        });
        if(hasError.booleanValue())
        {
            return false;
        }
        inDebugger = true;
        debugCommand( ".BioUML.debug.fn()", nullPrinter, nullPrinter );
        lineNum = 0;
        return inDebugger;
    }

    public boolean isDebug()
    {
        return inDebugger;
    }

    public int getCurrentLine()
    {
        return lineNum;
    }

    public boolean step(BreakType type)
    {
        if(type == BreakType.NONE)
        {
            do {} while(debugCommand("c", envPrint, envError));
            return false;
        } else if(type == BreakType.STEP_IN)
        {
            // STEP_IN is unsupported in R2
            boolean res = debugCommand(isR3_1()?"s":"n", envPrint, envError);
            if(res && lineNum == -1)
            {
                res = debugCommand("c", envPrint, envError);
            }
            return res;
        } else if(type == BreakType.STEP_OVER)
        {
            return debugCommand("n", envPrint, envError);
        } else if(type == BreakType.STEP_OUT)
        {
            return debugCommand(isR3_1()?"f":"c", envPrint, envError);
        } else
        {
            throw new UnsupportedOperationException();
        }
    }

    public void eval(String expression)
    {
        eval(expression, envPrint, envError);
    }

    public void evalSilent(String expression)
    {
        eval(expression, nullPrinter, nullPrinter);
    }

    public String expr(String expression)
    {
        StringPrinter resultPrinter = new StringPrinter();
        StringPrinter errPrinter = new StringPrinter();
        eval( "write(as.character("+expression+"),stdout())", resultPrinter, errPrinter );
        String error = errPrinter.toString();
        if(resultPrinter.toString().isEmpty() && !error.trim().isEmpty())
        {
            throw new BiosoftCustomException( null, error );
        }
        return resultPrinter.toString();
    }

    /**
     * @param command R debugger command (like 'n', 'c', etc.)
     * @param outPrinter printer to consume stdout messages
     * @param errPrinter printer to consume stderr messages
     * @return false if debugger finished
     */
    private boolean debugCommand(String command, Printer outPrinter, Printer errPrinter)
    {
        if( cancel )
            throw new CancellationException();
        if( !valid )
            throw new IllegalStateException( "Session is invalid" );
        if(!inDebugger)
            throw new IllegalStateException( "Debugger is inactive" );
        write( command );
        int state = 0; // 0 = echo, 1 = running, 2 = "debugging in..." is printed, 3 = parsing "debug at..." message
        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        List<String> swallowed = new ArrayList<>();
        while(true)
        {
            Message msg = getMessage(true);
            StringBuilder cur = msg.type == MessageType.OUT ? out : err;
            String str = msg.data.replace( "\r", "" );
            while(true)
            {
                int eol = str.indexOf( '\n' );
                if(eol == -1)
                {
                    cur.append( str );
                    break;
                }
                cur.append( str.substring( 0, eol ) );
                str = str.substring( eol+1 );
                if(msg.type == MessageType.OUT)
                {
                    if(state == 0)
                    {
                        if(!cur.toString().equals( command ))
                        {
                            errPrinter.print( "Unexpected output: "+cur );
                        }
                        state = 1;
                        cur.setLength( 0 );
                        continue;
                    }
                    if(state > 0)
                    {
                        Matcher matcher = DEBUGGING_IN_PATTERN.matcher( cur );
                        if(matcher.find())
                        {
                            if(state >= 2)
                            {
                                for(String swallowedLine : swallowed)
                                {
                                    // "debug at ..." happened to be normal output: recover
                                    outPrinter.print( swallowedLine );
                                }
                                swallowed.clear();
                            }
                            state = 2;
                        }
                        matcher = DEBUG_AT_PATTERN.matcher( cur );
                        if(matcher.find())
                        {
                            String line = matcher.group( 4 );
                            if(line != null)
                            {
                                lineNum = Integer.parseInt( line );
                            }
                            if(matcher.group( 1 ).startsWith( "debug: " ))
                            {
                                lineNum = -1; // Internal R method
                            }
                            if(state == 3)
                            {
                                for(String swallowedLine : swallowed)
                                {
                                    // "debug at ..." happened to be normal output: recover
                                    outPrinter.print( swallowedLine );
                                }
                                swallowed.clear();
                            }
                            state = 3;
                        }
                    }
                    if(cur.toString().equals(endMarker))
                    {
                        errPrinter.print( err.toString() );
                        inDebugger = false;
                        return false;
                    }
                    if(state == 1)
                    {
                        outPrinter.print( cur.toString() );
                    } else if(state > 1)
                    {
                        swallowed.add( cur.toString() );
                    }
                } else if(msg.type == MessageType.ERR)
                {
                    errPrinter.print( err.toString() );
                }
                cur.setLength( 0 );
            }
            if(cur == out && state == 3 && DEBUG_BROWSE_PATTERN.matcher( cur ).matches())
            {
                // Debugging session continues
                return true;
            }
        }
    }

    private void eval(String expression, Printer outPrinter, Printer errPrinter)
    {
        if( cancel )
            throw new CancellationException();
        if( !valid )
            throw new IllegalStateException( "Session is invalid" );
        if( inDebugger )
            throw new IllegalStateException( "Debugger is active" );
        String rString = RWriter.getRString( expression );
        if(rString.length() < CHUNK_SIZE)
        {
            String input =
                    "tryCatch("
                    + "eval(parse(text="+rString+")),"
                    + "error = function(e) {"
                    + "write(e$message, stderr());});"
                    + "write(getOption('prompt'), stderr());";
            // Consume anything which appeared since last command
            read( null, nullPrinter, nullPrinter, false );
            write( input );
            read( input, outPrinter, errPrinter, true );
        } else
        {
            List<String> chunks = RWriter.getRStringChunks( expression, CHUNK_SIZE );
            read( null, nullPrinter, nullPrinter, false );
            write( ".BioUML.command.text <- \"\";");
            for(String chunk : chunks)
            {
                read( null, nullPrinter, nullPrinter, false );
                write( ".BioUML.command.text <- paste(.BioUML.command.text, "+chunk+", sep=\"\");");
            }
            String input =
                    "tryCatch("
                    + "eval(parse(text=.BioUML.command.text)),"
                    + "error = function(e) {"
                    + "write(e$message, stderr());});"
                    + "write(getOption('prompt'), stderr());";
            read( null, nullPrinter, nullPrinter, false );
            write( input );
            read( input, outPrinter, errPrinter, true );
        }
    }

    private void write(String input)
    {
        try
        {
            launcher.write( input+System.lineSeparator() );
        }
        catch( IOException e )
        {
            close();
            throw ExceptionRegistry.translateException( e );
        }
    }

    private void read(@Nullable String input, Printer outPrinter, Printer errPrinter, boolean tillPrompt)
    {
        boolean echoRead = false;
        boolean endOutCandidate = !tillPrompt;
        boolean endErrCandidate = !tillPrompt;
        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        while(true)
        {
            Message msg = getMessage(!(endOutCandidate && endErrCandidate));
            if(msg == null)
            {
                errPrinter.print( err.toString() );
                return;
            }
            StringBuilder cur = msg.type == MessageType.OUT ? out : err;
            String str = msg.data.replace( "\r", "" );
            while(true)
            {
                int eol = str.indexOf( '\n' );
                if(eol == -1)
                {
                    cur.append( str );
                    break;
                }
                cur.append( str.substring( 0, eol ) );
                str = str.substring( eol+1 );
                String curStr = cur.toString();
                cur.setLength( 0 );
                if(msg.type == MessageType.OUT)
                {
                    if(!echoRead && curStr.equals( input ))
                        echoRead = true;
                    else
                    {
                        endOutCandidate = !tillPrompt || curStr.equals(endMarker);
                        if(!endOutCandidate)
                        {
                            outPrinter.print( curStr );
                        }
                    }
                } else if(msg.type == MessageType.ERR)
                {
                    if(!endErrCandidate || !curStr.isEmpty())
                    {
                        endErrCandidate = !tillPrompt || curStr.equals(endMarker);
                        if(!endErrCandidate)
                        {
                            errPrinter.print( curStr );
                        }
                    }
                }
            }
        }
    }

    private Message getMessage(boolean block)
    {
        Message msg = block ? launcher.read() : launcher.readNonBlocking();
        if(msg == null)
        {
            return null;
        }
        if(msg.type == MessageType.TIME_OUT)
        {
            valid = false;
            close();
            throw new InternalException( "Time out waiting the response from R" );
        }
        if(msg.type == MessageType.CANCEL)
        {
            throw new CancellationException();
        }
        if(msg.type == MessageType.EXIT)
        {
            valid = false;
            throw new InternalException( "Premature termination of R process" );
        }
        return msg;
    }

    private String capture(String expression)
    {
        final StringPrinter result = new StringPrinter();
        eval(expression, result, envError);
        return result.toString();
    }

    @Override
    protected void finalize()
    {
        try
        {
            close();
        }
        catch( Throwable t )
        {
            // Ignore
        }
    }

    @Override
    public void close()
    {
        if(!launcher.isRunning())
        {
            return;
        }
        try
        {
            if(inDebugger)
            {
                launcher.write( "Q\n" );
            }
            launcher.write("q();\n");
        }
        catch( IOException e )
        {
            // Ignore
        }
        launcher.terminate();
        valid = false;
    }

    public boolean isValid()
    {
        return valid;
    }

    public static RDirectSession create()
    {
        return create(0);
    }

    public static RDirectSession create(long timeOutMillis)
    {
        // TODO: detect encoding
        Charset encoding = Charset.forName( "cp1251" );
        String rLocation = RUtility.getRLocation();
        if(rLocation == null)
        {
            throw new InternalException( "Unable to find R installation" );
        }
        RProcessLauncher launcher = new RProcessLauncher(encoding, timeOutMillis);
        launcher.setCommand( "\""+rLocation+"\" --quiet --no-save --no-restore" );
        launcher.addEnv( "LC_ALL", "C" );
        launcher.execute();
        return new RDirectSession(launcher);
    }

    public void unlink()
    {
        int refs = nRef.decrementAndGet();
        if(refs == 0)
        {
            close();
        }
    }

    public void cancel()
    {
        cancel = true;
        launcher.addMessage( new Message(MessageType.CANCEL, null) );
    }

    public void addRef()
    {
        nRef.incrementAndGet();
    }
}
