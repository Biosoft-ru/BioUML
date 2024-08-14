package ru.biosoft.access.sql;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import org.apache.commons.io.output.CountingOutputStream;

import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.TempFiles;

/**
 * Bulk inserter that writes data to tab separated file and then use
 * 'LOAD DATA LOCAL INFILE' mysql statement.
 *
 * @author ivan
 *
 */
public class FastBulkInsert extends BulkInsert
{
    private File file;
    private PrintWriter writer;
    private CountingOutputStream countingOS;//used to track the number of bytes written, it is faster then calling file.length()
    private static final long MAX_MYSQLIMPORT_FILE_LENGTH = 50 * 1024 * 1024;

    public FastBulkInsert(Connection conn, String table, String[] fields)
    {
        super(conn, table, fields);
    }

    public FastBulkInsert(SqlConnectionHolder holder, String table, String[] fields)
    {
        super(holder, table, fields);
    }

    public FastBulkInsert(Connection conn, String table)
    {
        this(conn, table, null);
    }

    public FastBulkInsert(SqlConnectionHolder holder, String table)
    {
        this(holder, table, null);
    }

    @Override
    public void flush() throws BiosoftSQLException
    {
        if( file == null )
            return;
        writer.close();

        String sql = "LOAD DATA LOCAL INFILE " + SqlUtil.quoteString(file.getAbsolutePath()) + " INTO TABLE " + table;
        sql += " CHARACTER SET UTF8";
        if( fields != null )
            sql += " (" + String.join(",", getEscapedFields()) + ")";
        SqlUtil.execute(getConnection(), sql);

        file.delete();
        file = null;
        writer = null;
    }

    @Override
    protected void finalize() throws Throwable
    {
        if(writer != null) writer.close();
        if(file != null) file.delete();
        super.finalize();
    }

    private static String objectToSQLString(Object obj)
    {
        if( obj == null )
            return "\\N";
        if( obj instanceof Double )
        {
            Double d = (Double)obj;
            if( d.isNaN() )
                return "\\N";
            if( d.isInfinite() )
                return String.valueOf(d>0?Double.MAX_VALUE:-Double.MAX_VALUE);
        }
        if( obj instanceof Float )
        {
            Float d = (Float)obj;
            if( d.isNaN() )
                return "\\N";
            if( d.isInfinite() )
                return String.valueOf(d>0?Double.MAX_VALUE:-Double.MAX_VALUE);
        }
        return escapeSpecialChars( obj.toString() );
    }
    
    private static String escapeSpecialChars(String s)
    {
        int n = s.length();
        for(int i = 0; i < s.length(); i++)
        {
            char c = s.charAt( i );
            if(c == '\\' || c == '\t' || c =='\n')
                n++;
        }
        if(n == s.length())
            return s;
        char[] chars = new char[n];
        int j = 0;
        for(int i = 0; i < s.length(); i++)
        {
            char c = s.charAt( i );
            if(c == '\\' || c == '\t' || c =='\n')
                chars[j++] = '\\';
            chars[j++] = c;
        }
        return new String( chars );
    }

    @Override
    public void insert(Object... fields) throws BiosoftSQLException
    {
        if( file == null || countingOS.getByteCount() > MAX_MYSQLIMPORT_FILE_LENGTH )
        {
            flush();
            try
            {
                file = TempFiles.file(".tsv");
                countingOS = new CountingOutputStream( new FileOutputStream( file ) );
                Writer osWriter = new OutputStreamWriter( countingOS, "utf8" );
                writer = new PrintWriter(osWriter);
            }
            catch( IOException e )
            {
                throw ExceptionRegistry.translateException(e);
            }
        }

        if( fields.length != 0 )
        {
            writer.write( objectToSQLString( fields[0] ) );
            for(int i = 1; i < fields.length; i++)
                writer.append( '\t' ).append( objectToSQLString(fields[i]) );
        }
        writer.write('\n');
    }
}
