package ru.biosoft.bsa;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;

public abstract class SlicedSequence extends SequenceSupport
{
    /**
     * List of slices sorted by 'from'
     */
    protected NavigableMap<Integer, Reference<Slice>> slices = new ConcurrentSkipListMap<>();
    
    protected ThreadLocal<Slice> lastSlice = new ThreadLocal<>();

    public SlicedSequence(Alphabet alphabet)
    {
        super(null, alphabet);
    }
    
    public SlicedSequence(String name, Alphabet alphabet)
    {
        super(name, alphabet);
    }

    protected abstract Slice loadSlice(int pos);

    @Override
    public byte getLetterAt(int position) throws RuntimeException
    {
        Slice slice = getSlice(position);
        byte[] region = slice.data;
        if(region == null)
        {
            throw new DataElementReadException(null, getOrigin() == null ? DataElementPath.create(getName()):getOrigin().getCompletePath(), slice.from+"-"+slice.to);
        }
        return region[position - slice.from];
    }

    public Slice getSlice(int position)
    {
        Slice slice = searchSlice(position);
        if( slice == null || slice.data == null )
        {
            if(slice != null) slices.remove(slice.from);
            try
            {
                slice = loadSlice(position);
            }
            catch(Exception e)
            {
                throw new DataElementReadException(e, getOrigin() == null ? DataElementPath.create(getName()):getOrigin().getCompletePath(), "#"+position);
            }
            if( slice == null )
            {
                throw new DataElementReadException(null, getOrigin() == null ? DataElementPath.create(getName()):getOrigin().getCompletePath(), "#"+position);
            }
            lastSlice.set(slice);
            slices.put( slice.from, new SoftReference<>( slice ) );
        }
        return slice;
    }
    
    /**
     * Returns slice by position if it was cached previously
     */
    protected Slice searchSlice(int pos)
    {
        // Check last loaded slice first as it hits very often
        Slice slice = lastSlice.get();
        if(slice != null && pos >= slice.from && pos < slice.to)
            return slice;
        Map.Entry<Integer, Reference<Slice>> ref = slices.floorEntry( pos );
        if(ref == null || (slice = ref.getValue().get()) == null)
            return null;
        lastSlice.set( slice );
        return pos >= slice.from && pos < slice.to ? slice : null;
    }

    @Override
    public void setLetterAt(int position, byte letter) throws RuntimeException
    {
        throw new UnsupportedOperationException("setLetterAt is unsupported for SlicedSequence");
    }

    @Override
    public byte getLetterCodeAt(int position, Alphabet alphabet)
    {
        Slice slice = getSlice(position);
        if(position+alphabet.codeLength()-1>=slice.to)
            return super.getLetterCodeAt(position, alphabet);
        byte[] region = slice.data;
        if(region == null)
        {
            System.out.println("Error fetching region: "+slice.from+"-"+slice.to);
        }
        return alphabet.lettersToCode(slice.data, position-slice.from);
    }
}
