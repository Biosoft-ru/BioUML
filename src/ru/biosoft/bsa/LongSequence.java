package ru.biosoft.bsa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.util.ConcurrentFileBuffer;
import ru.biosoft.util.FileBuffer;
import ru.biosoft.util.TempFiles;

/**
 * LongSequence is designed to process very long sequence.
 *
 * <p>The sequence is stored as text file (row text).
 * For fast access to sequence characters {@link FileBuffer} is used.
 *
 * <p>Note. It is suggested that sequence file is temporary and should be removed
 * by <code>LongSequence</>.
 * 
 * <p>Note. With ConcurrentFileBuffer it doesn't support writing</p>
 *
 * @pending error processing and logging
*/
public class LongSequence extends SequenceSupport
{
    /** The sequence length. */
    protected int length = -1;

    /** File were sequence is stored (as row text). */
    protected File file;

    /** FileBuffer for fast access to the sequence. */
    protected volatile FileBuffer buffer;

    /** File reader to make the sequence file. */
    protected Reader reader;
    protected int seqLineOffset = 0;

    protected static final Logger cat = Logger.getLogger(LongSequence.class.getName());

    ////////////////////////////////////////
    // Constructor
    //
    public LongSequence(File file, Alphabet alphabet) throws RuntimeException
    {
        super(file.getName(), alphabet);
        localInit(file, new ConcurrentFileBuffer(file));
    }

    /**
     * Special constructor for lazy initialisation.
     *
     * <p>The sequence file will be created from reader data only when
     * real data (sequence characters) will be requested.
     */
    public LongSequence(Reader reader, int lineOffset, Alphabet alphabet, int length)
    {
        super(null, alphabet);
        this.reader = reader;
        this.seqLineOffset = lineOffset;
        this.length = length;
    }

    ///////////////////////////////////////////////////////////////////
    // init functions
    //

    /**
     * Creates temporary file with sequence data.
     */
    protected void initBuffer() throws Exception
    {
        if( buffer == null )
        {
            synchronized( this )
            {
                if( buffer == null )
                {
                    BufferedReader seqReader = new BufferedReader(reader);
                    if( seqLineOffset > 0 )
                    {
                        for( int i = 0; i < seqLineOffset; i++ )
                            seqReader.readLine();
                    }

                    File file = TempFiles.file("longseq");

                    try (BufferedWriter writer = new BufferedWriter( new FileWriter( file ), 32000 ))
                    {
                        // first character should not be null under windows, otherwise DPF under windows
                        // cannot interpret the temporary file correctly
                        //writer.write(0);
                        writer.write( 42 );

                        SequenceFactory.parseSequence( writer, seqReader, alphabet, ErrorLetterPolicy.REPLACE_BY_ANY );
                    }
                    reader.close();
                    localInit(file, new ConcurrentFileBuffer(file));
                }
            }
        }
    }

    protected void localInit(File file, FileBuffer buffer) throws RuntimeException
    {
        try
        {
            this.file = file;
            length = (int)file.length() - 1;

            this.buffer = buffer;
        }
        catch( Exception ex )
        {
            throw new RuntimeException("Long sequence creation error", ex);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        if( cat.isLoggable( Level.FINE ) )
            cat.log(Level.FINE, "LongSequence.finalize: " + this);
        try
        {
            if( buffer != null )
            {
                buffer.close();
            }
            if( file != null )
            {
                file.delete();
                if( cat.isLoggable( Level.FINE ) )
                    cat.log(Level.FINE, "file " + file.getName() + " is deleted: " + !file.exists());
            }
        }
        finally
        {
            super.finalize();
        }
    }

    ////////////////////////////////////////
    // public
    //

    @Override
    final public int getLength()
    {
        if( length < 0 && buffer == null )
        {
            try
            {
                initBuffer();
            }
            catch( Throwable t )
            {
                cat.log(Level.SEVERE, "Can not get sequence length.", t);
            }
        }

        return length;
    }

    @Override
    public int getStart()
    {
        return 1;
    }

    @Override
    public byte getLetterAt(int position) throws RuntimeException
    {
        try
        {
            initBuffer();

            byte letter = buffer.read(position);
            return letter;
        }
        catch( Exception ex )
        {
            throw new RuntimeException("LongSequence read error", ex);
        }
    }

    @Override
    public void setLetterAt(int position, byte letter) throws RuntimeException
    {
        try
        {
            initBuffer();

            buffer.write(position, letter);
        }
        catch( Exception ex )
        {
            throw new RuntimeException("Temporary sequence file write error", ex);
        }
    }

    @Override
    public boolean equals(Object toCompare)
    {
        if( this == toCompare )
            return true;
        if( toCompare == null || this.getClass() != toCompare.getClass() )
            return false;
        LongSequence toSeq = (LongSequence)toCompare;
        if( file == null && toSeq.file == null )
            return true;
        if( file != null && toSeq.file != null )
        {
            if( file.equals(toSeq.file) )
                return true;
            return SequenceFactory.compareSequences((Sequence)this, (Sequence)toCompare);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }
}
