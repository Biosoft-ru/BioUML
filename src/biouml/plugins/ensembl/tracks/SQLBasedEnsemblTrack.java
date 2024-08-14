package biouml.plugins.ensembl.tracks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Module;
import biouml.plugins.ensembl.access.EnsemblSequenceTransformer;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SlicedTrack;
import ru.biosoft.bsa.TrackUtils;

/**
 * Base class for ensembl connection support
 */
public abstract class SQLBasedEnsemblTrack extends SlicedTrack
{
    protected DataElementPath defaultSequencesPath;
    protected String mainTable;
    protected int maxSiteLength = -1;

    public SQLBasedEnsemblTrack(String name, DataCollection<?> origin, int sliceLength, String mainTable)
    {
        super(name, origin, sliceLength);
        this.mainTable = mainTable;
        try
        {
            defaultSequencesPath = TrackUtils.getPrimarySequencesPath(Module.getModulePath(this));
        }
        catch( Exception e )
        {
        }
    }
    
    /**
     * @return template to create an SQL query to retrieve a slice of sites. May contain the following placeholders:
     * {table} - main table name
     * {range} - range restriction clause
     */
    abstract protected String getSliceQueryTemplate();
    
    /**
     * @return template to create an SQL query to retrieve a site by name. May contain the following placeholders:
     * {table} - main table name
     * {range} - range restriction clause
     * {site} - name of the site
     */
    abstract protected String getSiteQueryTemplate();

    /**
     * Creates site from ResultSet
     * @param rs - ResultSet returned by query created via getSliceQueryTemplate or getSiteQueryTemplate
     * @param sequence parent sequence for the site
     * @return created Site
     * @throws Exception if any problem appeared
     */
    abstract protected Site createSite(ResultSet rs, Sequence sequence) throws Exception;

    protected Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getConnection(getOrigin());
    }
    
    protected String getIntervalSqlClause(String sequence, Interval interval)
    {
        if(maxSiteLength == -1 && interval.getFrom() > 1)
        {
            synchronized(this)
            {
                if(maxSiteLength == -1)
                {
                    try
                    {
                        maxSiteLength = SqlUtil.queryInt(getConnection(), "SELECT MAX(seq_region_end-seq_region_start) FROM "+mainTable);
                    }
                    catch( BiosoftSQLException e )
                    {
                        new DataElementReadException(e, this, "maximal site length").log();
                    }
                }
            }
        }
        int leftBoundary = interval.getFrom() - maxSiteLength + 1;
        if( leftBoundary < 0 || maxSiteLength == -1 )
            leftBoundary = 0;
        return "seq_region_id=" + getSequenceId(sequence) + " AND (seq_region_start BETWEEN " + leftBoundary + " AND " + interval.getTo()
                + ") AND seq_region_end>=" + interval.getFrom();
    }

    /**
     * @param sequence
     * @return
     */
    protected String getSequenceId(String sequence)
    {
        DataElementPath sequencePath = DataElementPath.create(sequence);
        AnnotatedSequence map = sequencePath.getDataElement(AnnotatedSequence.class);
        DynamicProperty property = map.getProperties().getProperty(EnsemblSequenceTransformer.CHROMOSOME_ID);
        if(property == null && defaultSequencesPath != null)
        {
            sequencePath = defaultSequencesPath.getChildPath(sequencePath.getName());
            map = sequencePath.getDataElement(AnnotatedSequence.class);
            property = map.getProperties().getProperty(EnsemblSequenceTransformer.CHROMOSOME_ID);
        }
        if( property == null )
            throw new DataElementReadException(sequencePath, EnsemblSequenceTransformer.CHROMOSOME_ID);
        String id = property.getValue().toString();
        return id;
    }

    @Override
    protected int countSitesLimited(String sequence, Interval interval, int limit)
    {
        try
        {
            return SqlUtil.queryInt(getConnection(),
                    "SELECT count(1) FROM (SELECT 1 FROM " + mainTable + " WHERE " + getIntervalSqlClause(sequence, interval)
                            + ( limit > 0 ? " LIMIT " + limit : "" ) + ") s", -1);
        }
        catch( BiosoftSQLException e1 )
        {
            new DataElementReadException(e1, this, "site count").log();
            return -1;
        }
    }

    @Override
    protected Site doGetSite(String sequence, String siteName, Interval interval) throws Exception
    {
        ResultSet rs = null;
        Statement st = null;
        AnnotatedSequence map = (AnnotatedSequence)CollectionFactory.getDataElement(sequence);
        try
        {
            String query = getSiteQueryTemplate().replace("{table}", mainTable)
                    .replace("{range}", getIntervalSqlClause(sequence, interval)).replace("{site}", SqlUtil.quoteString(siteName));
            st = SqlUtil.createStatement(getConnection());
            rs = SqlUtil.executeAndAdvance(st, query);
            if(rs != null)
            {
                return createSite(rs, map.getSequence());
            }
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this, "site "+siteName);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
        return null;
    }

    @Override
    protected Collection<Site> loadSlice(String sequence, Interval interval) throws Exception
    {
        List<Site> result = new ArrayList<>();
        String query = getSliceQueryTemplate().replace( "{table}", mainTable ).replace( "{range}",
                getIntervalSqlClause( sequence, interval ) );
        AnnotatedSequence map = (AnnotatedSequence)CollectionFactory.getDataElement( sequence );
        try (Statement st = SqlUtil.createStatement( getConnection() ); ResultSet rs = st.executeQuery( query ))
        {
            while( rs.next() )
            {
                result.add(createSite(rs, map.getSequence()));
            }
            return result;
        }
        catch( Exception e )
        {
            throw new DataElementReadException(e, this, "slice ("+sequence+interval+")");
        }
    }
}
