package ru.biosoft.plugins.jri.rdirect;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.exception.InternalException;

public class RWriter
{
    public static void writeInteger(Writer w, int x) throws IOException
    {
        w.write( String.valueOf(x) );
        w.write( 'L' );
    }
    
    public static void writeLong(Writer w, long x) throws IOException
    {
        w.write( String.valueOf(x) );
        w.write( 'L' );
    }
    
    public static void writeBoolean(Writer w, boolean b) throws IOException
    {
        w.write( b ? "TRUE" : "FALSE" );
    }
    
    public static void writeNull(Writer w) throws IOException
    {
        w.write("NULL");
    }
    
    public static void writeDouble(Writer w, double x) throws IOException
    {
        if(Double.isNaN( x ))
            w.write( "NaN" );
        else if(x == Double.POSITIVE_INFINITY)
            w.write( "Inf" );
        else if(x == Double.NEGATIVE_INFINITY)
            w.write( "-Inf" );
        else w.write( String.valueOf(x) );
    }
    
    public static void writeString(Writer w, String s) throws IOException
    {
        w.write( '"' );
        for(char c : s.toCharArray())
        {
            writeEscapedChar( w, c );
        }
        w.write( '"' );
    }

    private static void writeEscapedChar(Writer w, char c) throws IOException
    {
        switch(c)
        {
            case '\\':
            case '\'':
            case '"':
            case '`':
                w.write( '\\' );
                w.write( c );
                break;
            case '\n':
                w.write( "\\n" );
                break;
            case '\r':
                w.write( "\\r" );
                break;
            case '\t':
                w.write( "\\t" );
                break;
            case '\b':
                w.write( "\\b" );
                break;
            case 7:
                w.write( "\\a" );
                break;
            case '\f':
                w.write( "\\f" );
                break;
            case 11:
                w.write( "\\v" );
                break;
            default:
                if(c >= 32 && c<128) {
                    w.write( c );
                } else {
                    w.write("\\u");
                    String h = Integer.toHexString( c );
                    for(int i=h.length(); i<4; i++)
                        w.write( '0' );
                    w.write(h);
                }
        }
    }
    
    public static void writeObject(Writer w, Object obj) throws IOException
    {
        if(obj == null)
        {
            writeNull(w);
        } else if(obj instanceof Integer)
        {
            writeInteger(w, (Integer)obj);
        } else if(obj instanceof Long)
        {
            writeLong(w, (Long)obj);
        } else if(obj instanceof Boolean)
        {
            writeBoolean(w, (Boolean)obj);
        } else if(obj instanceof Number)
        {
            writeDouble(w, ((Number)obj).doubleValue());
        } else if(obj instanceof int[])
        {
            writeArray(w, (int[])obj);
        } else if(obj instanceof double[])
        {
            writeArray(w, (double[])obj);
        } else if(obj instanceof String[])
        {
            writeArray(w, (String[])obj);
        } else if(obj instanceof Object[])
        {
            writeArray(w, (Object[])obj);
        } else
        {
            writeString(w, obj.toString());
        }
    }
    
    public static void writeArray(Writer w, Object[] arr) throws IOException
    {
        w.write( "list(" );
        boolean start = true;
        for(Object element : arr)
        {
            if(start)
            {
                start = false;
            } else
            {
                w.write( ", " );
            }
            writeObject(w, element);
        }
        w.write( ")" );
    }

    public static void writeArray(Writer w, String[] arr) throws IOException
    {
        w.write( "c(" );
        boolean start = true;
        for(String element : arr)
        {
            if(start)
            {
                start = false;
            } else
            {
                w.write( ", " );
            }
            writeString(w, element);
        }
        w.write( ")" );
    }

    public static void writeArray(Writer w, int[] arr) throws IOException
    {
        w.write( "c(" );
        boolean start = true;
        for(int element : arr)
        {
            if(start)
            {
                start = false;
            } else
            {
                w.write( ", " );
            }
            writeInteger(w, element);
        }
        w.write( ")" );
    }
    
    public static void writeArray(Writer w, double[] arr) throws IOException
    {
        w.write( "c(" );
        boolean start = true;
        for(double element : arr)
        {
            if(start)
            {
                start = false;
            } else
            {
                w.write( ", " );
            }
            writeDouble(w, element);
        }
        w.write( ")" );
    }
    
    public static String getRString(Object obj)
    {
        StringWriter str = new StringWriter();
        try
        {
            writeObject(str, obj);
        }
        catch( IOException e )
        {
            throw new InternalException( e );
        }
        return str.toString();
    }
    
    public static List<String> getRStringChunks(String input, int limit)
    {
        List<String> strings = new ArrayList<>();
        StringWriter sw = new StringWriter();
        sw.append('"');
        for(int i=0; i<input.length(); i++)
        {
            try
            {
                writeEscapedChar(sw, input.charAt( i ));
                if(sw.getBuffer().length() > limit-6)
                {
                    sw.append( '"' );
                    strings.add( sw.toString() );
                    sw = new StringWriter();
                    sw.append('"');
                }
            }
            catch( IOException e )
            {
                throw new InternalException( e );
            }
        }
        if(sw.getBuffer().length() > 1)
        {
            sw.append( '"' );
            strings.add( sw.toString() );
        }
        return strings;
    }
}
