package ru.biosoft.bsa.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Slice;
import ru.biosoft.bsa.SlicedSequence;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.ConnectionPool;
import ru.biosoft.server.Request;

public class ClientSequence extends SlicedSequence
{
    protected static final Logger log = Logger.getLogger(ClientSequence.class.getName());

    protected String pathOnServer;
    protected BSAClient connection;

    public ClientSequence(DataCollection<?> origin) throws LoggedException
    {
        super(Nucleotide15LetterAlphabet.getInstance());

        ClientConnection conn = ConnectionPool.getConnection(origin);
        connection = new BSAClient(new Request(conn, log), log);
    }

    public void setPathOnServer(String pathOnServer)
    {
        this.pathOnServer = pathOnServer;
    }

    @Override
    protected Slice loadSlice(int pos)
    {
        try
        {
            return connection.getSlice(pathOnServer, pos);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not load slice from server", e);
        }
        return null;
    }

    protected int length = -1;

    @Override
    public int getLength()
    {
        if( length == -1 )
        {
            try
            {
                length = connection.getSequenceLength(pathOnServer);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get sequence length", e);
                length = 0;
            }
        }
        return length;
    }

    protected int start = -1;

    @Override
    public int getStart()
    {
        if( start == -1 )
        {
            try
            {
                start = connection.getSequenceStart(pathOnServer);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get sequence start", e);
                start = 0;
            }
        }
        return start;
    }
}
