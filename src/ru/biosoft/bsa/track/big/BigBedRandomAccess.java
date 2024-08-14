package ru.biosoft.bsa.track.big;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.jetbrains.bio.RomBuffer;
import org.jetbrains.bio.RomBufferFactory;
import org.jetbrains.bio.big.BedEntry;
import org.jetbrains.bio.big.BigBedFile;
import org.jetbrains.bio.big.Interval;
import org.jetbrains.bio.big.MultiInterval;
import org.jetbrains.bio.big.Offset;
import org.jetbrains.bio.big.RTreeIndex;
import org.jetbrains.bio.big.RTreeIndex.Header;
import org.jetbrains.bio.big.RTreeIndexLeaf;

import kotlin.sequences.Sequence;

public class BigBedRandomAccess
{
    private BigBedFile bbFile;
    private TreeMap<Long, Block> idxToOffset;
    private long siteCount;
    public BigBedRandomAccess(BigBedFile bbFile) throws IOException, DataFormatException
    {
        this.bbFile = bbFile;
        initIdxToOffset();
    }
    
    static class Block
    {
        Block(long offset, long size)
        {
            this.offset = offset;
            this.size = size;
        }
        
        long offset, size;

        @Override
        public String toString()
        {
            return "Block [offset=" + offset + ", size=" + size + "]";
        }
        
    }
    
    public long getSiteCount()
    {
        return siteCount;
    }
    
    public List<BedEntry> fetch(long from, int size) throws IOException, DataFormatException
    {
        if(from < 0 || from >= siteCount || size < 0 || from+size > siteCount)
            throw new IllegalArgumentException();
        if(size == 0)
            return Collections.emptyList();
        
        
        
        List<BedEntry> result = new ArrayList<>();
        
        Long keyFrom = idxToOffset.floorKey( from );
        Long keyTo = idxToOffset.floorKey( from + size -1 );
        SortedMap<Long, Block> blocks = idxToOffset.subMap( keyFrom, true, keyTo, true );
        
        RomBufferFactory factory = bbFile.getBuffFactory$big();
        int uncompressedBufSize = bbFile.header.getUncompressBufSize();

        try (RomBuffer input = factory.create())
        {
            long skip = -1;        
            for(Map.Entry<Long, Block> entry : blocks.entrySet())
            {
                if(skip == -1)
                    skip = from - entry.getKey();
                Block block = entry.getValue();
                
                input.setPosition( block.offset );
                byte[] compressed = input.readBytes( (int)block.size );
                Inflater inflater = new Inflater();
                inflater.setInput( compressed );
                byte[] uncompressed = new byte[uncompressedBufSize];
                int uncompressedLen = inflater.inflate( uncompressed );
                if(inflater.getRemaining() > 0)
                    throw new DataFormatException();

                
                ByteBuffer buf = ByteBuffer.wrap(uncompressed, 0, uncompressedLen);
                buf.order(input.getOrder());
                while (buf.hasRemaining()) {
                    int chrId = buf.getInt();
                    int start = buf.getInt();
                    int end = buf.getInt();
                   
                    if(skip == 0)
                    {
                        StringBuilder sb = new StringBuilder();
                        byte c;
                        while ((c = buf.get()) > 0)
                            sb.append( (char)c );
                        
                        String rest = sb.toString();
                        String chrom = bbFile.getChromosomes().get( chrId );
                        result.add( new BedEntry( chrom, start, end, rest ) );
                        if(result.size() >= size)
                            return result;
                    }
                    else
                    {
                        skip--;
                        while (buf.get() > 0);
                    }
                }
            }
         
        }

        return result;
    }
    
    
    private void initIdxToOffset() throws IOException, DataFormatException
    {
        idxToOffset = new TreeMap<>();
        RTreeIndex rTree = bbFile.rTree;
        Offset left = new Offset( 0, 0 );
        Offset right = new Offset(Integer.MAX_VALUE, Integer.MAX_VALUE);

        Interval query = new MultiInterval( left, right );

        int itemsPerSlot = -1;//do not rely on rTreeHeader.getItemsPerSlot()
        
        RomBufferFactory factory = bbFile.getBuffFactory$big();
        int uncompressedBufSize = bbFile.header.getUncompressBufSize();

        try (RomBuffer input = factory.create())
        {
            Sequence<RTreeIndexLeaf> leafs = rTree.findOverlappingBlocks( input, query, uncompressedBufSize, null );
            Iterator<RTreeIndexLeaf> it = leafs.iterator();

            //int sitesInPrevBlock = 0;
            RTreeIndexLeaf prev = null;
            siteCount = 0;
            while( it.hasNext() )
            {
                RTreeIndexLeaf leaf = it.next();
                long offset = leaf.getDataOffset();
                long size = leaf.getDataSize();
                int chrIdx = leaf.getInterval().getLeft().getChromIx();

                if( prev != null )
                {
                    if( prev.getInterval().getLeft().getChromIx() != chrIdx )
                    {
                        //leaf and prev on distinct chromosomes
                        //last block on chromosome maybe not full (<=itemsPerSlot)
                        siteCount += countInCompressedBlock( input, prev.getDataOffset(), prev.getDataSize(), uncompressedBufSize );
                    }
                    else
                    {
                        //leaf and prev on same chromosomes
                        //assume that prev block is full in this case
                        if( itemsPerSlot == -1 )
                        {
                            itemsPerSlot = countInCompressedBlock( input, prev.getDataOffset(), prev.getDataSize(), uncompressedBufSize );
                        }
                        siteCount += itemsPerSlot;
                    }
                }
                
                idxToOffset.put( siteCount, new Block( offset, size ) );
                prev = leaf;
            }
            
            if(prev != null)//last block
            {
                siteCount += countInCompressedBlock( input, prev.getDataOffset(), prev.getDataSize(), uncompressedBufSize );
            }
        }
        
    }

    private static int countInCompressedBlock(RomBuffer input, long offset, long size, int uncompressedBufSize) throws DataFormatException
    {
        input.setPosition( offset );
        return countInCompressedBlock( input.readBytes( (int)size ), uncompressedBufSize, input.getOrder());
    }
    private static int countInCompressedBlock(byte[] compressed, int uncompressedBufSize, ByteOrder order) throws DataFormatException
    {
        //TODO: use RomBuffer.decompress
        Inflater inflater = new Inflater();
        inflater.setInput( compressed );
        byte[] uncompressed = new byte[uncompressedBufSize];
        int uncompressedLen = inflater.inflate( uncompressed );
        if(inflater.getRemaining() > 0)
            throw new DataFormatException();
        return countInUncompressedBlock(uncompressed, 0, uncompressedLen, order);
    }
    
    private static int countInUncompressedBlock(byte[] uncompressed, int uncompressedOffset, int uncompressedLen, ByteOrder order)
    {
        int res=  0;
        ByteBuffer buf2 = ByteBuffer.wrap(uncompressed, uncompressedOffset, uncompressedLen);
        buf2.order(order);
        while (buf2.hasRemaining()) {
            int chrId = buf2.getInt();
            int start = buf2.getInt();
            int end = buf2.getInt();
            while (buf2.get() > 0);
            res++;
        }
        return res;
        
    }
      
}

