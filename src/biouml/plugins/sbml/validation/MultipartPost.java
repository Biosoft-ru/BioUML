package biouml.plugins.sbml.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Performs a Multipart HTTP post to the given URL.  A post operation
 * is started with the creation of a MultipartPost object.  Post
 * parameters are sent with writeParameter() and may be either
 * strings or the contents of an XML file.  A post is finished by
 * calling done() which returns an InputStream for reading the servers
 * response.
 *
 * NOTE: This class is meant to communicate with the SBML.org online
 * validator.  As such, it assumes uploaded files are XML and always
 * sends a Content-Type: text/xml.
 */
class MultipartPost
{
    public MultipartPost(String url) throws IOException
    {
        Random random = new Random();

        connection = ( new URL(url) ).openConnection();
        connection.setReadTimeout( 10000 );
        connection.setConnectTimeout( 10000 );
        boundary = "<<" + Long.toString(random.nextLong(), 30);
        String type = "multipart/form-data; boundary=" + boundary;

        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", type);

        stream = connection.getOutputStream();
    }

    public InputStream done() throws IOException
    {
        writeln("--" + boundary + "--");
        writeln();

        stream.close();

        return connection.getInputStream();
    }

    public void writeParameter(String name, String value) throws IOException
    {
        writeln("--" + boundary);
        writeln("Content-Disposition: form-data; name=\"" + name + "\"");
        writeln();
        writeln(value);
    }

    public void writeParameter(String name, File file) throws IOException
    {
        String prefix = "Content-Disposition: form-data; name=\"file\"; filename=";

        writeln("--" + boundary);
        writeln(prefix + '"' + file.getName() + '"');
        writeln("Content-Type: text/xml");
        writeln();

        try(InputStream source = new FileInputStream(file))
        {
            copy(source, stream);
            writeln();
            stream.flush();
        }
    }

    void copy(InputStream source, OutputStream destination) throws IOException
    {
        byte[] buffer = new byte[8192];
        int nbytes = 0;

        while( ( nbytes = source.read(buffer, 0, buffer.length) ) >= 0 )
        {
            destination.write(buffer, 0, nbytes);
        }
    }

    void writeln(String s) throws IOException
    {
        write(s);
        writeln();
    }

    void writeln() throws IOException
    {
        write('\r');
        write('\n');
    }

    void write(char c) throws IOException
    {
        stream.write(c);
    }

    void write(String s) throws IOException
    {
        stream.write(s.getBytes(StandardCharsets.UTF_8));
    }


    URLConnection connection;
    OutputStream stream;
    String boundary;
}
