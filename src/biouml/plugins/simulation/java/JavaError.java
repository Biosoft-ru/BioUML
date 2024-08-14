package biouml.plugins.simulation.java;

/**
 * Created by IntelliJ IDEA.
 * User: bober
 * Date: 04.09.2004
 * Time: 16:14:25
 * To change this template use Options | File Templates.
 */
public class JavaError
{
    static final String FILE_START = "File: ";
    static final String FILE_END = " Line: ";
    static final String LINE_START = FILE_END;
    static final String LINE_END = " Column: ";
    static final String COLUMN_START = LINE_END;
    static final String COLUMN_END = "\n";
    static final String MESSAGE_END = COLUMN_END;

    String message = null;
    String fileName = null;
    int line = 0;
    int column = 0;

    JavaError(String result)
    {
        //??? Error: File: d:\matlabr12\work\pharmo_simple_script.m Line: 3 Column: 8
        //"identifier" expected, ";" found.

        result = result.substring(result.indexOf("???") + "???".length());

        fileName = result.substring(result.indexOf(FILE_START) + FILE_START.length(), result.indexOf(FILE_END));

        String lineStr = result.substring(result.indexOf(LINE_START) + LINE_START.length(), result.indexOf(LINE_END));
        line = Integer.parseInt(lineStr);

        String columnStr = result.substring(result.indexOf(COLUMN_START) + COLUMN_START.length(), result.indexOf(COLUMN_END));
        column = Integer.parseInt(columnStr);

        message = result.substring(result.indexOf(MESSAGE_END) + MESSAGE_END.length());
    }
}
