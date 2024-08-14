package ru.biosoft.access.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public class PersistentPreparedStatement extends PersistentStatement implements PreparedStatement
{
    protected String query;
    protected PreparedStatement preparedStatement;

    @Override
    void reconnect(SQLException ex) throws SQLException
    {
        conn.reconnect(ex);
        this.originalConnection = conn.getOriginalConnection();
        preparedStatement = originalConnection.prepareStatement(query, type, concurrency, holdability);
        parent = preparedStatement;
    }
    
    PersistentPreparedStatement(PersistentConnection conn, String query, int type, int concurrency, int holdability) throws SQLException
    {
        this.conn = conn;
        this.type = type;
        this.concurrency = concurrency;
        this.holdability = holdability;
        this.query = query;
        this.originalConnection = conn.getOriginalConnection();
        preparedStatement = originalConnection.prepareStatement(query, type, concurrency, holdability);
        parent = preparedStatement;
        this.conn.addStatement(this);
    }

    @Override
    public void addBatch() throws SQLException
    {
        preparedStatement.addBatch();
    }

    @Override
    public void clearParameters() throws SQLException
    {
        preparedStatement.clearParameters();
    }

    @Override
    public boolean execute() throws SQLException
    {
        return preparedStatement.execute();
    }

    @Override
    public ResultSet executeQuery() throws SQLException
    {
        return preparedStatement.executeQuery();
    }

    @Override
    public int executeUpdate() throws SQLException
    {
        return preparedStatement.executeUpdate();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException
    {
        return preparedStatement.getMetaData();
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException
    {
        return preparedStatement.getParameterMetaData();
    }

    @Override
    public void setArray(int arg0, Array arg1) throws SQLException
    {
        preparedStatement.setArray(arg0, arg1);
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1, int arg2) throws SQLException
    {
        preparedStatement.setAsciiStream(arg0, arg1, arg2);
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1, long arg2) throws SQLException
    {
        preparedStatement.setAsciiStream(arg0, arg1, arg2);
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1) throws SQLException
    {
        preparedStatement.setAsciiStream(arg0, arg1);
    }

    @Override
    public void setBigDecimal(int arg0, BigDecimal arg1) throws SQLException
    {
        preparedStatement.setBigDecimal(arg0, arg1);
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1, int arg2) throws SQLException
    {
        preparedStatement.setBinaryStream(arg0, arg1, arg2);
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1, long arg2) throws SQLException
    {
        preparedStatement.setBinaryStream(arg0, arg1, arg2);
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1) throws SQLException
    {
        preparedStatement.setBinaryStream(arg0, arg1);
    }

    @Override
    public void setBlob(int arg0, Blob arg1) throws SQLException
    {
        preparedStatement.setBlob(arg0, arg1);
    }

    @Override
    public void setBlob(int arg0, InputStream arg1, long arg2) throws SQLException
    {
        preparedStatement.setBlob(arg0, arg1, arg2);
    }

    @Override
    public void setBlob(int arg0, InputStream arg1) throws SQLException
    {
        preparedStatement.setBlob(arg0, arg1);
    }

    @Override
    public void setBoolean(int arg0, boolean arg1) throws SQLException
    {
        preparedStatement.setBoolean(arg0, arg1);
    }

    @Override
    public void setByte(int arg0, byte arg1) throws SQLException
    {
        preparedStatement.setByte(arg0, arg1);
    }

    @Override
    public void setBytes(int arg0, byte[] arg1) throws SQLException
    {
        preparedStatement.setBytes(arg0, arg1);
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1, int arg2) throws SQLException
    {
        preparedStatement.setCharacterStream(arg0, arg1, arg2);
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException
    {
        preparedStatement.setCharacterStream(arg0, arg1, arg2);
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1) throws SQLException
    {
        preparedStatement.setCharacterStream(arg0, arg1);
    }

    @Override
    public void setClob(int arg0, Clob arg1) throws SQLException
    {
        preparedStatement.setClob(arg0, arg1);
    }

    @Override
    public void setClob(int arg0, Reader arg1, long arg2) throws SQLException
    {
        preparedStatement.setClob(arg0, arg1, arg2);
    }

    @Override
    public void setClob(int arg0, Reader arg1) throws SQLException
    {
        preparedStatement.setClob(arg0, arg1);
    }

    @Override
    public void setDate(int arg0, Date arg1, Calendar arg2) throws SQLException
    {
        preparedStatement.setDate(arg0, arg1, arg2);
    }

    @Override
    public void setDate(int arg0, Date arg1) throws SQLException
    {
        preparedStatement.setDate(arg0, arg1);
    }

    @Override
    public void setDouble(int arg0, double arg1) throws SQLException
    {
        preparedStatement.setDouble(arg0, arg1);
    }

    @Override
    public void setFloat(int arg0, float arg1) throws SQLException
    {
        preparedStatement.setFloat(arg0, arg1);
    }

    @Override
    public void setInt(int arg0, int arg1) throws SQLException
    {
        preparedStatement.setInt(arg0, arg1);
    }

    @Override
    public void setLong(int arg0, long arg1) throws SQLException
    {
        preparedStatement.setLong(arg0, arg1);
    }

    @Override
    public void setNCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException
    {
        preparedStatement.setNCharacterStream(arg0, arg1, arg2);
    }

    @Override
    public void setNCharacterStream(int arg0, Reader arg1) throws SQLException
    {
        preparedStatement.setNCharacterStream(arg0, arg1);
    }

    @Override
    public void setNClob(int arg0, NClob arg1) throws SQLException
    {
        preparedStatement.setNClob(arg0, arg1);
    }

    @Override
    public void setNClob(int arg0, Reader arg1, long arg2) throws SQLException
    {
        preparedStatement.setNClob(arg0, arg1, arg2);
    }

    @Override
    public void setNClob(int arg0, Reader arg1) throws SQLException
    {
        preparedStatement.setNClob(arg0, arg1);
    }

    @Override
    public void setNString(int arg0, String arg1) throws SQLException
    {
        preparedStatement.setNString(arg0, arg1);
    }

    @Override
    public void setNull(int arg0, int arg1, String arg2) throws SQLException
    {
        preparedStatement.setNull(arg0, arg1, arg2);
    }

    @Override
    public void setNull(int arg0, int arg1) throws SQLException
    {
        preparedStatement.setNull(arg0, arg1);
    }

    @Override
    public void setObject(int arg0, Object arg1, int arg2, int arg3) throws SQLException
    {
        preparedStatement.setObject(arg0, arg1, arg2, arg3);
    }

    @Override
    public void setObject(int arg0, Object arg1, int arg2) throws SQLException
    {
        preparedStatement.setObject(arg0, arg1, arg2);
    }

    @Override
    public void setObject(int arg0, Object arg1) throws SQLException
    {
        preparedStatement.setObject(arg0, arg1);
    }

    @Override
    public void setRef(int arg0, Ref arg1) throws SQLException
    {
        preparedStatement.setRef(arg0, arg1);
    }

    @Override
    public void setRowId(int arg0, RowId arg1) throws SQLException
    {
        preparedStatement.setRowId(arg0, arg1);
    }

    @Override
    public void setShort(int arg0, short arg1) throws SQLException
    {
        preparedStatement.setShort(arg0, arg1);
    }

    @Override
    public void setSQLXML(int arg0, SQLXML arg1) throws SQLException
    {
        preparedStatement.setSQLXML(arg0, arg1);
    }

    @Override
    public void setString(int arg0, String arg1) throws SQLException
    {
        preparedStatement.setString(arg0, arg1);
    }

    @Override
    public void setTime(int arg0, Time arg1, Calendar arg2) throws SQLException
    {
        preparedStatement.setTime(arg0, arg1, arg2);
    }

    @Override
    public void setTime(int arg0, Time arg1) throws SQLException
    {
        preparedStatement.setTime(arg0, arg1);
    }

    @Override
    public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2) throws SQLException
    {
        preparedStatement.setTimestamp(arg0, arg1, arg2);
    }

    @Override
    public void setTimestamp(int arg0, Timestamp arg1) throws SQLException
    {
        preparedStatement.setTimestamp(arg0, arg1);
    }

    @Override
    @Deprecated
    public void setUnicodeStream(int arg0, InputStream arg1, int arg2) throws SQLException
    {
        preparedStatement.setUnicodeStream(arg0, arg1, arg2);
    }

    @Override
    public void setURL(int arg0, URL arg1) throws SQLException
    {
        preparedStatement.setURL(arg0, arg1);
    }
    
    //
    //Compatible with java 1.7
    @Override
    public boolean isCloseOnCompletion()
    {
        return false;
    }
    @Override
    public void closeOnCompletion()
    {
    }
}
