package ru.biosoft.access;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import ru.biosoft.access.core.Key;

/**
 * Index for search keys by custom algorithm.
 * @todo Use BTreeIndex for int keys
 * @todo Optimize key/node iterator
 * @todo Implement removing
 */
public class BTreeRangeIndex extends BTreeIndex
{
    public BTreeRangeIndex( File dataFile, String indexName, String indexPath, int blockSize )
    throws IOException
    {
        super( dataFile,indexName,indexPath,blockSize );
    }

    @Override
    protected Node createNode( byte[] buffer,int pos,String key,IndexEntry value )
    {
        return new StringNode(buffer,pos,key,value);
    }

    @Override
    protected Node createNode( byte[] buffer,int pos )
    {
        return new StringNode(buffer,pos);
    }

    // @todo optimize it
    @Override
    public Iterator keyIterator(Key key)
    {
        Iterator<String> iter = keySet().iterator();
        int count = 0;
        boolean found = false;
        while( iter.hasNext() )
        {
            if( key.accept(iter.next()) )
            {
                found = true;
                break;
            }
            count++;
        }
        if( found )
        {
            iter = keySet().iterator();
            while( count>0 )
            {
                count --;
                iter.next();
            }
        }
        return iter;
    }

    // @todo optimize it
    @Override
    public Iterator nodeIterator( Key key )
    {
        try
        {
            if ( root==null )
                root = getBlock( ROOT_BLOCK_OFFSET );
            return root.iterator( key );
        }
        catch ( IOException exc )
        {
            throw new RuntimeException("nodeIterator("+key.serializeToString()+") failed.",exc );
        }
    }

    @Override
    public IndexEntry get( Object key )
    {
        try
        {
            if ( root==null )
                root = getBlock( ROOT_BLOCK_OFFSET );
            return root.get( (String)key );
        } catch ( IOException exc )
        {
            throw new RuntimeException("get("+key+") failed.",exc );
        }
    }

    @Override
    public IndexEntry put( String key, IndexEntry entry )
    {
        try
        {
            if ( root==null )
                root = getBlock( ROOT_BLOCK_OFFSET );
            IndexEntry ret = root.put( key,entry,null,null,true );
            if ( ret==null )
            {
                if ( currSize != Integer.MAX_VALUE )
                    currSize++;

                writeCurrSize();
            }
            return ret;
        } catch ( IOException exc )
        {
            throw new RuntimeException( "put(Object,Object) failed.",exc );
        }
    }

    private class StringNode extends Node
    {
        public StringNode( byte[] buffer,int pos,String key,IndexEntry value )
        {
            super( buffer,pos,key,value );
            if( value==null )
                end = offset + key.length() + 2;
            else
                end = offset + key.length() + ((StringIndexEntry)value).value.length() + 2;
        }

        public StringNode( byte[] buffer,int pos )
        {
            super( buffer,pos );
        }

        @Override
        protected void init( byte[] buffer, int pos )
        {
            this.buffer = buffer;
            offset = pos;
            end = pos;

            if ( end>=buffer.length || buffer[end]==0)
                return;

            byte ch;
            int i=0;
            char[] tmp = new char[1024];
            while ( (ch=buffer[end++])!='\r' )
            {
                tmp[i++] = (char)ch;
            }
            value = new StringIndexEntry(new String(tmp,0,i));

            if ( end>=buffer.length || buffer[end]==0)
                return;

            i = 0;
            while ( (ch=buffer[end++])!='\r' )
            {
                tmp[i++] = (char)ch;
            }
            key = new String(tmp,0,i);
        }
    }
}