package ru.biosoft.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import ru.biosoft.util.DPSUtils;

import com.developmentontheedge.beans.DynamicPropertySet;

public class Response
{
    protected OutputStream os;

    /** Total size of transmitted data. */
    public long lengthOfTransmittedData = 0;

    protected String url;

    public Response(OutputStream os, String url)
    {
        this.os = os;
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public OutputStream getOutputStream()
    {
        return os;
    }

    /**
     * Sends error message
     * 
     * @param string
     * @throws IOException
     */
    public synchronized void error(String message) throws IOException
    {
        StringBuffer s = new StringBuffer();
        s.append(message);

        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(Connection.ERROR);
        dos.writeUTF(s.toString());
        dos.flush();

        lengthOfTransmittedData += s.length();
    }

    public synchronized void error(Throwable t) throws IOException
    {
        error(t.getMessage());
    }

    public void sendDPSArray(DynamicPropertySet[] dpsArray) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DPSUtils.saveDPSArray(dpsArray, out);
        send(out.toByteArray(), Connection.FORMAT_GZIP);
    }

    /**
     * Sends the message in the specified format, with leading OK, meaning all
     * is correct.
     */
    public synchronized void send(byte[] message, int format) throws IOException
    {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(Connection.OK);

        dos.writeInt(format);

        if( message != null )
        {
            if( format == Connection.FORMAT_SIMPLE )
            {
                byte[] b = message;
                dos.writeInt(b.length);
                if( b.length > 0 )
                    dos.write(b);
            }
            else if( format == Connection.FORMAT_GZIP )
            {
                byte[] b = message;
                ByteArrayOutputStream baos = new ByteArrayOutputStream(b.length / 3);

                try (GZIPOutputStream gos = new GZIPOutputStream( baos ))
                {
                    gos.write( b );
                    gos.finish();
                }

                b = baos.toByteArray();

                dos.writeInt(b.length);
                dos.write(b);
                dos.writeInt(message.length);
            }
            lengthOfTransmittedData += message.length;
        }
        dos.flush();
    }

    /**
     * Disconnect function (by default do nothing).
     */
    public void disconnect()
    {
    }

}
