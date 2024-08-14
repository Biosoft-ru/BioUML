package biouml.plugins.ensembl.type;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import biouml.plugins.ensembl.access.EnsemblSequenceTransformer;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Slice;
import ru.biosoft.bsa.SlicedSequence;
import ru.biosoft.util.LazyValue;

/**
 * Ensemble sequence element
 */
public class EnsemblSequence extends SlicedSequence implements SqlConnectionHolder
{
    public static final String ASSEMBLY_TABLE = "assembly";
    public static final String DNA_TABLE = "dna";

    /**
     * Length of the sequence
     */
    protected int length;
    
    /**
     * Root region ID for sequence
     */
    protected int seqRegionId;

    private final DataCollection<?> owner;
    
    private List<Integer> coordSystemPath;
    
    public EnsemblSequence(int seqRegionId, int length, DataCollection<?> owner, List<Integer> coordSystemPath)
    {
        super(Nucleotide15LetterAlphabet.getInstance());
        this.seqRegionId = seqRegionId;
        this.length = length;
        this.owner = owner;
        this.coordSystemPath = coordSystemPath;
    }

    @Override
    protected Slice loadSlice(int pos) throws BiosoftSQLException
    {
        AssemblyException assemblyException = getAssemblyException( pos );
        if(assemblyException != null)
        {
            Slice slice = loadSliceForAssemblyException( assemblyException, pos );
            //System.out.println( "Req " + pos + ", load ex " + getName() + ":" + slice.from + "-" + slice.to );
            return slice;
        }
        
        Assembly assembly = getSequenceLevelAssembly( pos );
        
        cutAssemblyException( pos, assembly );
        
        Slice slice = new Slice();
        slice.from = assembly.sourceStart;
        slice.to = assembly.sourceEnd + 1;
        slice.data = getSequencePart(assembly);
        
        //System.out.println( "Req " + pos + ", load " + getName() + ":" + slice.from + "-" + slice.to );
        return slice;
    }

    private void cutAssemblyException(int pos, Assembly assembly)
    {
        Interval sourceInterval = new Interval(assembly.sourceStart, assembly.sourceEnd);
        for( AssemblyException ae : assemblyExceptions.get() )
        {
            Interval exRegion = ae.getDerivedRegion();
            if(sourceInterval.intersects( exRegion ))
            {
                for(Interval part : sourceInterval.remainOfIntersect( exRegion ))
                    if(part.inside( pos ))
                    {
                        sourceInterval = part;
                        break;
                    }
            }
        }
        if(assembly.sourceStart != sourceInterval.getFrom())
            assembly.moveStart( sourceInterval.getFrom() - assembly.sourceStart );
        if(assembly.sourceEnd != sourceInterval.getTo())
            assembly.moveEnd( sourceInterval.getTo() - assembly.sourceEnd );
    }
    
    private Assembly getSequenceLevelAssembly(int pos)
    {
        Iterator<Integer> it = coordSystemPath.iterator();
        it.next();//skip first coord system (chromosome coord system)
        
        if(!it.hasNext())
        {
            //chromosome coord system itself at sequence level
            //return mapping to itself
            return new Assembly( seqRegionId, 1, getLength(), seqRegionId, 1, getLength(), false );
        }

        int sourceSeqRegion = seqRegionId;
        int sourcePos = pos;
        
        Assembly merged = null;
        
        while(it.hasNext())
        {
            int coordSystem = it.next();
            Assembly assembly = getAssembly(sourceSeqRegion, sourcePos, coordSystem);
            
            if(merged == null)
                merged = assembly;
            else
                merged = merged.merge( assembly );
            
            if(assembly.targetSeqRegionId == -1)//unmapped region
                break;

            sourceSeqRegion = assembly.targetSeqRegionId;
            sourcePos = assembly.translateSourceToTargetPos( sourcePos );
        }

        return merged;
    }
    
    @Override
    public int getLength()
    {
        return length;
    }

    @Override
    public int getStart()
    {
        return 1;
    }
    
    protected Assembly getAssembly(int sourceSeqRegionId, int sourcePos, int targetCoordSystem) throws BiosoftSQLException
    {
        String query = "SELECT cmp_seq_region_id, asm_start, asm_end, cmp_start, cmp_end, ori "
                + "FROM assembly JOIN seq_region ON(seq_region_id=cmp_seq_region_id) "
                + "WHERE asm_seq_region_id=" + sourceSeqRegionId + " AND coord_system_id=" + targetCoordSystem
                + " AND asm_start<=" + sourcePos + " AND asm_end>=" + sourcePos;

        ResultSet rs = null;
        Statement st = null;
        Connection con = getConnection();
        try
        {
            st = con.createStatement();
            rs = st.executeQuery(query);
            
            if( rs.next() )
                return new Assembly( sourceSeqRegionId, rs.getInt(2), rs.getInt(3), rs.getInt(1), rs.getInt(4), rs.getInt(5), rs.getInt(6) == -1);
        }
        catch(SQLException ex)
        {
            throw new BiosoftSQLException(this, query, ex);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
        
        Interval unmapped = getEmptySubRegion( sourceSeqRegionId, sourcePos, targetCoordSystem );
        return new Assembly( sourceSeqRegionId, unmapped.getFrom(), unmapped.getTo(), -1, 1, unmapped.getLength(), false );
    }

    protected Interval getEmptySubRegion(int sourceSeqRegionId, int sourcePos, int targetCoordSystem)
    {
        Connection connection = getConnection();
        
        int emptyStart = SqlUtil.queryInt(connection, "SELECT MAX(asm_end) FROM " + ASSEMBLY_TABLE
                + " JOIN seq_region ON(seq_region_id=cmp_seq_region_id)"
                + " WHERE asm_seq_region_id=" + sourceSeqRegionId + " AND coord_system_id=" + targetCoordSystem + " AND asm_end<" + sourcePos);
        

        int emptyEnd = SqlUtil.queryInt(connection, "SELECT MIN(asm_start) FROM " + ASSEMBLY_TABLE
                + " JOIN seq_region ON(seq_region_id=cmp_seq_region_id)"
                + " WHERE asm_seq_region_id=" + sourceSeqRegionId + " AND coord_system_id=" + targetCoordSystem + " AND asm_start>" + sourcePos);
        if(emptyEnd == 0)
        {
            //MIN(asm_start) was NULL, asm_start always > 0
            int sourceLength = SqlUtil.queryInt( connection, "SELECT length FROM seq_region WHERE seq_region_id=" + sourceSeqRegionId );
            emptyEnd = sourceLength + 1;
        }
        
        return new Interval(emptyStart+1, emptyEnd-1);
    }

    protected byte[] getSequencePart(Assembly subRegion) throws BiosoftSQLException
    {
        if(subRegion.targetSeqRegionId == -1)
        {
            //unmapped region
            byte[] data = new byte[subRegion.targetEnd - subRegion.targetStart + 1];
            for( int i = 0; i < data.length; i++ )
                data[i] = (byte)'N';
            return data;
        }
        
        String str = SqlUtil.queryString(getConnection(), "SELECT sequence FROM " + DNA_TABLE + " WHERE seq_region_id=" + subRegion.targetSeqRegionId);
        if(str == null)
            throw new DataElementReadException(getOrigin().getCompletePath(), "contig#" + subRegion.targetSeqRegionId);
        
        byte[] bytes = IntStreamEx.ofChars( str.substring(subRegion.targetStart - 1, subRegion.targetEnd) ).toByteArray();
        
        if(subRegion.reverseComplement)
            bytes = SequenceRegion.getReversedSequence(new LinearSequence(bytes, Nucleotide15LetterAlphabet.getInstance())).getBytes();
        
        return bytes;
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getConnection(owner);
    }

    /** Assembly states, which parts of seq_regions (from distinct coord_systems) are exactly equal. */
    protected static class Assembly
    {
        public int targetSeqRegionId;
        public int sourceSeqRegionId;
        
        /** All coordinates 1-based, start and end inclusive */
        public int sourceStart;
        public int sourceEnd;
        public int targetStart;
        public int targetEnd;
        
        public boolean reverseComplement;

        public Assembly(int sourceSeqRegionId, int sourceStart, int sourceEnd, int targetSeqRegionId, int targetStart, int targetEnd, boolean reverseComplement)
        {
            this.targetSeqRegionId = targetSeqRegionId;
            this.sourceSeqRegionId = sourceSeqRegionId;
            this.sourceStart = sourceStart;
            this.sourceEnd = sourceEnd;
            this.targetStart = targetStart;
            this.targetEnd = targetEnd;
            this.reverseComplement = reverseComplement;
        }
        
        public int translateSourceToTargetPos(int pos)
        {
            if(!(pos >= sourceStart && pos <= sourceEnd))
                throw new IllegalArgumentException();
            int offset = pos - sourceStart;
            return reverseComplement ? targetEnd - offset : targetStart + offset;
        }
        
        public void moveStart(int offset)
        {
            this.sourceStart += offset;
            if(reverseComplement)
                targetEnd -= offset;
            else
                targetStart += offset;
        }
        
        public void moveEnd(int offset)
        {
            this.sourceEnd += offset;
            if(reverseComplement)
                targetStart -= offset;
            else
                targetEnd += offset;
        }

        
        public Assembly merge(Assembly next)
        {
            if(this.targetSeqRegionId != next.sourceSeqRegionId)
                throw new IllegalArgumentException();
            return new Assembly( this.sourceSeqRegionId, this.sourceStart + ( next.sourceStart - this.targetStart ),
                    this.sourceEnd + ( next.sourceEnd - this.targetEnd ), next.targetSeqRegionId, next.targetStart, next.targetEnd,
                    reverseComplement != next.reverseComplement );
        }
    }
    

    private final LazyValue<List<AssemblyException>> assemblyExceptions = new LazyValue<List<AssemblyException>>()
    {
        @Override
        protected List<AssemblyException> doGet() throws Exception
        {
            return fetchAssemblyExceptions();
        }
    };

    private List<AssemblyException> fetchAssemblyExceptions() throws BiosoftSQLException
    {
        List<AssemblyException> result = new ArrayList<>();
        String query = "SELECT seq_region_start,seq_region_end" + ",exc_seq_region_id,exc_seq_region_start,exc_seq_region_end"
                + " FROM assembly_exception WHERE seq_region_id=" + seqRegionId + " AND exc_type='PAR'";
        try (Statement st = getConnection().createStatement(); ResultSet rs = st.executeQuery( query ))
        {
            while( rs.next() )
            {
                int derivedRegionId = seqRegionId;
                Interval derivedRegion = new Interval( rs.getInt( 1 ), rs.getInt( 2 ) );
                int sourceRegionId = rs.getInt( 3 );
                Interval sourceRegion = new Interval( rs.getInt( 4 ), rs.getInt( 5 ) );
                AssemblyException ae = new AssemblyException( derivedRegionId, derivedRegion, sourceRegionId, sourceRegion );
                result.add( ae );
            }
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException( this, query, e );
        }
        return result;
    }

    private AssemblyException getAssemblyException(int pos)
    {
        for( AssemblyException ae : assemblyExceptions.get() )
            if( ae.getDerivedRegion().inside( pos ) )
                return ae;
        return null;
    }

    private Slice loadSliceForAssemblyException(AssemblyException assemblyException, int pos)
    {
        int sourcePos = assemblyException.translateFromDerivedToSource( pos );
        for( AnnotatedSequence as : (DataCollection<AnnotatedSequence>)owner )
        {
            String chrId = as.getProperties().getValue( EnsemblSequenceTransformer.CHROMOSOME_ID ).toString();
            if( chrId.equals( String.valueOf( assemblyException.getSourceRegionId() ) ) )
            {
                EnsemblSequence sourceSequence = (EnsemblSequence)as.getSequence();
                Slice sourceSlice = sourceSequence.getSlice( sourcePos );

                Slice slice = new Slice();
                slice.from = assemblyException.translateFromSourceToDerived( sourceSlice.from );
                slice.to = assemblyException.translateFromSourceToDerived( sourceSlice.to );
                slice.data = sourceSlice.data;
                
                int dx = 0;
                if(slice.from < assemblyException.getDerivedRegion().getFrom())
                    dx = assemblyException.getDerivedRegion().getFrom() - slice.from;
                
                int dy = 0;
                if(slice.to - 1 > assemblyException.getDerivedRegion().getTo())
                    dy = slice.to - assemblyException.getDerivedRegion().getTo() - 1;
                
                if(dx > 0 || dy > 0)
                {
                    slice.from += dx;
                    slice.to -= dy;
                    slice.data = Arrays.copyOfRange( slice.data, dx, slice.data.length - dy );
                }
                
                return slice;
            }
        }
        throw new RuntimeException( "Sequence region " + assemblyException.getSourceRegionId() + " is referenced by "
                + assemblyException.getDerivedRegionId() + " sequence region, but can not be found" );
    }
    
    
}
