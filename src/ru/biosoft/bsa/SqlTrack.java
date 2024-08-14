package ru.biosoft.bsa;

import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.SqlDataInfo;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.SortableDataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.BulkInsert;
import ru.biosoft.access.sql.FastBulkInsert;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlDataElement;
import ru.biosoft.access.sql.SqlList;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.GenomeSelector.GenomeSelectorTrackUpdater;
import ru.biosoft.bsa.access.SitesToTableTransformer;
import ru.biosoft.bsa.transformer.UnknownSequence;
import ru.biosoft.bsa.view.SqlTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;

@PropertyName ( "track" )
public class SqlTrack extends AbstractDataCollection<AnnotatedSequence> implements WritableTrack, SqlDataElement, CloneableDataElement
{
    protected static final Logger log = Logger.getLogger(SqlTrack.class.getName());

    public static final String DE_PROPERTY_COLLECTION_PREFIX = "DataCollection_";
    public static final String TABLE_PREFIX = "track";
    public static final String PROFILE_TABLE_PREFIX = "profile";
    public static final String PROPERTY_COLUMN_PREFIX = "prop_";
    public static final String LABEL_PROPERTY = "label";
    public static final PropertyDescriptor PROFILE_PD = StaticDescriptor.create("profile");
    public static final String MAX_SITE_LENGTH_PROPERTY = "maxSiteLength";

    protected String id;
    protected String tableId;
    protected String labelField;
    protected ChrCache chrCache = new ChrCache();
    protected boolean isValid = true;
    protected Properties initProperties;
    protected int size = -1;
    protected int maxSiteLength = -1;
    protected int queryLength;
    protected Position defaultPosition;
    protected GenomeSelector genomeSelector;

    private static class SiteProperty
    {
        public Class<?> type;
        public int index;
        public int maxLength = SqlDataInfo.INITIAL_COLUMN_LENGTH;
        public PropertyDescriptor pd;
        public String sqlName;

        public SiteProperty(Class<?> type, PropertyDescriptor pd)
        {
            this.type = type;
            this.index = 0;
            this.pd = pd;
        }
        public void setMaxLength(int maxLength)
        {
            this.maxLength = maxLength;
        }

        public int getMaxLength()
        {
            return maxLength;
        }
    }

    protected java.util.Map<String, SiteProperty> siteProperties = null;
    protected HashMap<String, DataCollection<?>> propertyDataCollections = new HashMap<>();

    protected BulkInsert inserter = null;

    protected String profileTableId;
    protected BulkInsert profileInserter;
    protected boolean hasProfiles = false;

    protected Query query(String template)
    {
        return new Query(template).name("table", tableId).name("profileTable", profileTableId);
    }

    protected void createSQLTable() throws BiosoftSQLException
    {
        SqlUtil.execute(getConnection(), query("CREATE TABLE $table$ (" + "`id`              INTEGER UNSIGNED NOT NULL,"
                + "`chrom`           VARCHAR(255) NOT NULL," + "`start`           INTEGER UNSIGNED NOT NULL,"
                + "`end`             INTEGER UNSIGNED NOT NULL," + "`strand`          INTEGER NOT NULL DEFAULT 0,"
                + "`type`            VARCHAR(255) DEFAULT NULL," + "PRIMARY KEY(`id`)" + ") ENGINE=MyISAM"));
        size = 0;
    }

    protected void createProfileSQLTable() throws BiosoftSQLException
    {
        SqlUtil.execute(getConnection(), query("CREATE TABLE $profileTable$ ("
                + "`id`              INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,"
                + "`site_id`         INTEGER UNSIGNED NOT NULL,"
                + "`position`        INTEGER UNSIGNED NOT NULL,"
                + "`value`           DOUBLE,"
                + "PRIMARY KEY(`id`),"
                + "KEY(`site_id`)" + ") ENGINE=MyISAM"));
    }

    protected void initDataCollections()
    {
        Properties properties = getInfo().getProperties();
        for( Object propertyKey : properties.keySet() )
        {
            if( propertyKey.toString().startsWith(DE_PROPERTY_COLLECTION_PREFIX) )
            {
                DataCollection<?> collection = CollectionFactory.getDataCollection(properties.getProperty(propertyKey.toString()));
                if( collection != null )
                    propertyDataCollections.put(propertyKey.toString().substring(DE_PROPERTY_COLLECTION_PREFIX.length()), collection);
            }
        }
    }

    synchronized int initTableStatus()
    {
        if( queryLength > 0 )
            return queryLength;
        ResultSet rs = null;
        Statement st = null;
        queryLength = Math.max(200000 / ( siteProperties.size() + 5 ), 100);
        try
        {
            long avgRowLength = SqlUtil.getAvgRowLength( getConnection(), tableId );
            if( avgRowLength != 0 )
                queryLength = Math.max((int) ( 10000000 / avgRowLength ), 50);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While initializing table status of " + getCompletePath(), e);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
        return queryLength;
    }

    public SqlTrack(DataCollection<?> origin, Properties properties)
    {
        super(origin, properties);
        initDataCollections();
        boolean shouldCreate = properties.getProperty( SqlDataInfo.ID_PROPERTY ) == null;
        if( shouldCreate ) // Create new SqlTrack
        {
            this.id = SqlDataInfo.initDataElement( getConnection(), origin, properties, TABLE_PREFIX );
            getInfo().getProperties().setProperty( SqlDataInfo.ID_PROPERTY, this.id );
        }
        else
        {
            this.id = properties.getProperty( SqlDataInfo.ID_PROPERTY );
            mutable = false;
        }
        if( getInfo().getNodeImage() == null )
            getInfo().setNodeImageLocation( SqlTrack.class, getNodeImageIcon() );
        if( getInfo().getChildrenNodeImage() == null )
            getInfo().setChildrenNodeImageLocation( SqlTrack.class, "resources/sequence.gif" );
        ExProperties.addPlugin( getInfo().getProperties(), getClass() );
        initProperties = (Properties)properties.clone();
        tableId = TABLE_PREFIX + "_" + this.id;
        profileTableId = PROFILE_TABLE_PREFIX + "_" + this.id;

        if( shouldCreate )
            createSQLTable();

        hasProfiles = SqlUtil.hasTable( getConnection(), profileTableId );
        if( shouldCreate && hasProfiles )
        {
            SqlUtil.dropTable( getConnection(), profileTableId );
            hasProfiles = false;
        }
        String defaultPositionStr = properties.getProperty(Track.DEFAULT_POSITION_PROPERTY);
        if( defaultPositionStr != null )
        {
            defaultPosition = new Position(defaultPositionStr);
        }
        if( properties.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY) != null )
        {
            DataElementPath seqBase = DataElementPath.create(properties.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
            chrCache.setSeqBase( seqBase );
        }
        getInfo().setChildrenLeaf(true);
        try
        {
            initSiteProperties();
        }
        catch( BiosoftSQLException e )
        {
            e.log();
            log.log(Level.SEVERE, this + ": Exception " + e.getId() + " occured during track initialization; track will be flagged as invalid");
            isValid = false;
        }
        labelField = properties.getProperty(LABEL_PROPERTY);
        if( labelField == null || siteProperties.get(labelField) == null )
        {
            labelField = "id";
        }
        else
        {
            labelField = PROPERTY_COLUMN_PREFIX + labelField;
        }
        setGenomeSelector( new GenomeSelector(this) );
        String maxLengthPropStr = properties.getProperty( MAX_SITE_LENGTH_PROPERTY );
        if(maxLengthPropStr != null)
            maxSiteLength = Integer.parseInt( maxLengthPropStr );
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        // Remove "mutable" check, for registerInputChild(AnnotatedSequence.class) to work
        return isValid() && clazz.isAssignableFrom(getDataElementType());
    }

    public int getId()
    {
        try
        {
            return Integer.parseInt(id);
        }
        catch( Exception e )
        {
            return -1;
        }
    }

    @Override
    public String getTableId()
    {
        return tableId;
    }

    @Override
    public String[] getUsedTables()
    {
        if( hasProfiles )
        {
            return new String[] {tableId, profileTableId};
        }
        return new String[] {tableId};
    }

    int countAllSites()
    {
        if( size >= 0 )
            return size;
        if( !isValid )
            size = 0;
        try
        {
            size = SqlUtil.getRowsCount(getConnection(), tableId);
        }
        catch( BiosoftSQLException e )
        {
            size = 0;
        }
        return size;
    }

    public void resetSize() {
        size = -1;
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        if( !isValid )
            return 0;
        SubSequence seq = new SubSequence(sequence, from, to);
        return SqlUtil.queryInt(getConnection(), query("SELECT COUNT(*) FROM $table$ WHERE chrom=$seq$ AND end>=$from$ AND start<=$to$")
                .str("seq", seq.getSequenceName()).num("from", seq.getFrom()).num("to", seq.getTo()));
    }

    public Collection<String> getAllProperties()
    {
        return Collections.unmodifiableSet(siteProperties.keySet());
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getConnection( this );
    }

    protected void initSiteProperties() throws BiosoftSQLException
    {
        siteProperties = new TreeMap<>();
        Statement st = null;
        ResultSet rs = null;
        try
        {
            st = SqlUtil.createStatement(getConnection());
            rs = SqlUtil.executeQuery(st, Query.describe(tableId));
            while( rs.next() )
            {
                String columnName = rs.getString("Field");
                if( !columnName.startsWith(PROPERTY_COLUMN_PREFIX) )
                    continue;
                String propertyName = columnName.substring(PROPERTY_COLUMN_PREFIX.length());
                String columnType = rs.getString("Type");
                Class<?> javaColumnType = SqlDataInfo.getJavaColumnType(columnType);
                if( javaColumnType == null )
                {
                    log.log(Level.SEVERE, "Unable to find matching Java type for SQL column " + tableId + "." + columnName + " " + columnType
                            + "; skipping");
                    continue;
                }
                PropertyDescriptor pd = BeanUtil.createDescriptor(propertyName);
                SiteProperty siteProperty = new SiteProperty(javaColumnType, pd);
                siteProperty.sqlName = columnName;
                siteProperty.setMaxLength(SqlDataInfo.getColumnContentLength(columnType));
                siteProperties.put(propertyName, siteProperty);
            }
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(this, e);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
    }

    protected DynamicPropertySet getPropertiesFromResultSet(ResultSet rs)
    {
        if( siteProperties == null )
            return null;
        DynamicPropertySet prop = new DynamicPropertySetAsMap();
        for( Entry<String, SiteProperty> entry : siteProperties.entrySet() )
        {
            try
            {
                String sitePropertyName = entry.getKey();
                SiteProperty siteProperty = entry.getValue();
                Class<?> type = siteProperty.type;
                String colName = siteProperty.sqlName;
                Object result = null;
                if( type == Integer.class )
                    result = rs.getInt(colName);
                else if( type == Float.class )
                    result = rs.getFloat(colName);
                else if( type == String.class )
                    result = rs.getString(colName);
                if( result != null && !rs.wasNull() )
                {
                    if( result instanceof String )
                    {
                        DataCollection<?> dc = propertyDataCollections.get(sitePropertyName);
                        if( dc != null )
                        {
                            DataElement de = dc.get((String)result);
                            if( de != null )
                                result = de;
                        }
                    }
                    prop.add(new DynamicProperty(siteProperty.pd, result.getClass(), result));
                }
                else
                {
                    prop.add(new DynamicProperty(siteProperty.pd, type, null));
                }
            }
            catch( Exception e )
            {
            }
        }
        return prop.size() > 0 ? prop : null;
    }

    private double[] getProfile(String chrom, Site site) throws BiosoftSQLException
    {
        Query sql;
        if(oldStyleProfileTable())
        {
            sql = query("SELECT position, value FROM $profileTable$ WHERE chrom=$seq$ AND position >= $from$ AND position <= $to$")
                    .str("seq", chrom).num("from", site.getFrom()).num("to", site.getTo());
        }
        else
        {
            sql = query("SELECT position, value FROM $profileTable$ WHERE site_id=$site_id")
                .str("site_id", site.getName());
        }
        Statement st = null;
        ResultSet rs = null;
        try
        {
            st = SqlUtil.createStatement(getConnection());
            rs = SqlUtil.executeQuery(st, sql);
            double[] result = new double[site.getLength()];
            while( rs.next() )
                result[rs.getInt(1) - site.getFrom()] = rs.getDouble(2);
            return result;
        }
        catch( SQLException ex )
        {
            throw new BiosoftSQLException(this, sql, ex);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
    }

    private boolean oldStyleProfileTable()
    {
        return SqlUtil.hasResult( getConnection(), query("SHOW COLUMNS FROM $profileTable$ LIKE 'chrom'") );
    }

    protected @Nonnull Site createSiteFromResultSet(Sequence seq, ResultSet rs) throws BiosoftSQLException
    {
        try
        {
            String chr = rs.getString(2);
            if( seq == null )
                seq = chrCache.getSequence(chr);
            DynamicPropertySet prop = getPropertiesFromResultSet(rs);
            String id = rs.getString(1);
            if( prop == null )
            {
                prop = new DynamicPropertySetAsMap();
            }
            String type = rs.getString(6);
            if( type == null )
                type = SiteType.TYPE_MISC_FEATURE;

            int strand = rs.getInt(5);
            //check that strand is correct
            if( strand < Site.STRAND_NOT_KNOWN || strand > Site.STRAND_BOTH )
            {
                log.warning( "Site '" + id + "' contains incorrect strand value + '" + strand
                        + "' it will be replaced by unknown strand type." );
                strand = StrandType.STRAND_NOT_KNOWN;
            }
            int start = rs.getInt(3);
            int end = rs.getInt(4);
            SiteImpl site = new SiteImpl(null, id, type, Basis.BASIS_USER, strand == StrandType.STRAND_MINUS ? end : start,
                    end - start + 1, Precision.PRECISION_EXACTLY, strand, seq, prop);
            if( hasProfiles )
            {
                double[] profile = getProfile(chr, site);
                site.getProperties().add(new DynamicProperty(PROFILE_PD, double[].class, profile));
            }
            return site;
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(this, e);
        }
    }

    protected @Nonnull
    AnnotatedSequence createChildElementFromResultSet(ResultSet rs) throws BiosoftSQLException
    {
        try
        {
            Site s = createSiteFromResultSet(null, rs);
            Sequence seq = s.getSequence();
            String name = rs.getString(labelField);
            if( seq == null )
            {
                seq = new UnknownSequence(name, 1, s.getLength());
            }
            return new MapAsVector(name, this, seq, null);
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(this, e);
        }
    }

    @Override
    public boolean contains(String name) throws BiosoftSQLException
    {
        if( !isValid )
            return false;
        if( v_cache.containsKey(name) )
            return true;
        return SqlUtil.hasResult(getConnection(), Query.byCondition(tableId, labelField, name));
    }

    @Override
    protected AnnotatedSequence doGet(String name) throws BiosoftSQLException
    {
        if( !isValid )
            return null;
        boolean labelChanged = updateLabel();
        if( nameList != null && labelChanged )
            nameList = null;
        Statement st = null;
        ResultSet rs = null;
        try
        {
            st = SqlUtil.createStatement(getConnection());
            rs = SqlUtil.executeAndAdvance(st, Query.byCondition(tableId, labelField, name));
            if( rs != null )
                return createChildElementFromResultSet(rs);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
        return null;
    }

    /**
     * Updates label field (which property will be used as name for collection elements
     * @return false if field is not updated, false otherwise
     */
    private boolean updateLabel()
    {
        String newLabelField = getInfo().getProperty(LABEL_PROPERTY);
        if( newLabelField == null || siteProperties.get(newLabelField) == null )
        {
            newLabelField = "id";
        }
        else
        {
            newLabelField = PROPERTY_COLUMN_PREFIX + newLabelField;
        }
        if( newLabelField.equals(labelField) )
            return false;
        labelField = newLabelField;
        return true;
    }

    private List<String> nameList = null;
    @Override
    public @Nonnull
    List<String> getNameList()
    {
        if( !isValid )
            return ListUtil.emptyList();
        if( updateLabel() )
            nameList = null;
        if( nameList == null )
        {
            if( getSize() < ApplicationUtils.getMaxSortingSize() )
                nameList = new SqlList(this, Query.sortedField(tableId, labelField), getSize(), true);
            else
                nameList = new SqlList(this, Query.field(tableId, labelField), getSize());
        }
        return nameList;
    }

    @Override
    public int getSize()
    {
        if( !isValid )
            return 0;
        return countAllSites();
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        if( !isValid )
        {
            return new VectorDataCollection<>("Sites", Site.class, null);
        }
        SubSequence seq = new SubSequence(sequence, from, to);
        return new SitesCollection(seq);
    }

    private int findMaxSiteLength() throws BiosoftSQLException
    {
        return SqlUtil.queryInt(getConnection(), query("SELECT MAX(end+1-start) FROM $table$"));
    }

    int getMaxSiteLength()
    {
        if( maxSiteLength == -1 )
        {
            synchronized(this)
            {
                if( maxSiteLength == -1 )
                {
                    maxSiteLength = findMaxSiteLength();
                    getInfo().getProperties().setProperty( MAX_SITE_LENGTH_PROPERTY, String.valueOf( maxSiteLength ) );
                    try {
                        getCompletePath().save( this );
                    } catch(Exception e) {
                        //user don't have permissions to store properties
                    }
                }
            }
        }
        return maxSiteLength;
    }

    /**
     * Generates WHERE clause by SubSequence
     * @param seq
     * @return
     */
    private String getWhereClause(SubSequence seq)
    {
        if( seq == null )
            return null;
        int leftBoundary = seq.getFrom() - getMaxSiteLength() + 1;
        if( leftBoundary < 0 )
            leftBoundary = 0;
        int rightBoundary = seq.getTo();
        return new Query("chrom=$seq$ AND (start BETWEEN $left$ AND $right$) AND end>=$from$").str(seq.getSequenceName())
                .num("left", leftBoundary).num("right", rightBoundary).num("from", seq.getFrom()).toString();
    }

    /**
     * Translate site by given SubSequence
     */
    Site createSite(SubSequence seq, ResultSet rs) throws BiosoftSQLException
    {
        return seq == null ? createSiteFromResultSet(null, rs) : seq.translateSite(createSiteFromResultSet(seq.getSequence(), rs));
    }

    protected TrackViewBuilder viewBuilder = new SqlTrackViewBuilder();
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws BiosoftSQLException
    {
        if( !isValid )
            return null;
        SubSequence seq = new SubSequence(sequence, from, to);
        Statement st = null;
        ResultSet rs = null;
        try
        {
            st = SqlUtil.createStatement(getConnection());
            rs = SqlUtil.executeAndAdvance(st, Query.byCondition(tableId, "id", siteName) + " AND " + getWhereClause(seq));
            if( rs != null )
                return createSite(seq, rs);
        }
        finally
        {
            SqlUtil.close(st, rs);
        }
        return null;
    }

    protected void checkProperties(Site site) throws BiosoftSQLException
    {
        DynamicPropertySet prop = site.getProperties();
        Iterator<String> iName = prop.nameIterator();
        boolean modified = false;
        while( iName.hasNext() )
        {
            String name = iName.next();
            if( "profile".equals(name) )
                continue;
            String sqlName = PROPERTY_COLUMN_PREFIX + name;
            SiteProperty siteProperty = siteProperties.get(name);
            if( siteProperty == null ) // new property appeared: add it
            {
                String typeSQL = null;
                DynamicProperty property = prop.getProperty(name);
                if( property.getType() == Integer.class )
                {
                    typeSQL = "int";
                    siteProperty = new SiteProperty(Integer.class, StaticDescriptor.create(name));
                }
                else if( property.getType() == Float.class || property.getType() == Double.class )
                {
                    typeSQL = "float";
                    siteProperty = new SiteProperty(Float.class, StaticDescriptor.create(name));
                }
                else
                {
                    siteProperty = new SiteProperty(String.class, StaticDescriptor.create(name));
                    Object value = prop.getValue(name);
                    int maxLength = SqlDataInfo.INITIAL_COLUMN_LENGTH;
                    if( value != null && value.toString().length() >= maxLength )
                    {
                        while( maxLength < value.toString().length() )
                            maxLength *= 2;
                        if( maxLength > SqlDataInfo.MAX_VARCHAR_LENGTH )
                            maxLength = Integer.MAX_VALUE;
                        siteProperty.setMaxLength(maxLength);
                    }
                    typeSQL = maxLength == Integer.MAX_VALUE ? "TEXT" : "VARCHAR(" + maxLength + ")";
                }
                siteProperty.sqlName = sqlName;
                SqlUtil.execute(getConnection(), "ALTER TABLE " + tableId + " ADD COLUMN " + SqlUtil.quoteIdentifier(sqlName) + " "
                        + typeSQL + " DEFAULT NULL");
                siteProperties.put(name, siteProperty);
                modified = true;
            }
            else
            {
                if( siteProperty.type == String.class )
                {
                    Object value = prop.getValue(name);
                    int maxLength = siteProperty.getMaxLength();
                    if( value != null && value.toString().length() > maxLength )
                    {
                        while( maxLength < value.toString().length() )
                            maxLength *= 2;
                        if( maxLength > SqlDataInfo.MAX_VARCHAR_LENGTH )
                            maxLength = Integer.MAX_VALUE;
                        siteProperty.setMaxLength(maxLength);
                        finalizeAddition(false);
                        SqlUtil.execute(getConnection(), "ALTER TABLE " + tableId + " MODIFY COLUMN " + SqlUtil.quoteIdentifier(sqlName)
                                + " " + ( maxLength == Integer.MAX_VALUE ? "TEXT" : "VARCHAR(" + maxLength + ")" ) + " DEFAULT NULL");
                        modified = true;
                    }
                }
            }
        }
        if( modified )
        {
            queryLength = 0;
            prepareAddStatement();
        }
    }

    protected void prepareAddStatement() throws BiosoftSQLException
    {
        finalizeAddition(false);
        List<String> fields = new ArrayList<>();
        fields.add("id");
        fields.add("chrom");
        fields.add("start");
        fields.add("end");
        fields.add("strand");
        fields.add("type");
        for( Entry<String, SiteProperty> entry : siteProperties.entrySet() )
        {
            fields.add(PROPERTY_COLUMN_PREFIX + entry.getKey());
            entry.getValue().index = fields.size();
        }
        inserter = new FastBulkInsert(this, tableId, fields.toArray(new String[fields.size()]));
    }

    private Site ensureBounds(Site site)
    {
        String chr = site.getOriginalSequence() != null ? site.getOriginalSequence().getName() : site.getName();
        Sequence seq = chrCache.getSequence(chr);
        if( seq.getLength() == 0 )
            return site;
        Interval interval = site.getInterval().intersect(seq.getInterval());
        if( interval == null )
            return null;

        int siteStart = ( site.getStrand() != StrandType.STRAND_MINUS ) ? interval.getFrom() : interval.getTo();
        int siteLength = Math.abs(interval.getLength());

        return new SiteImpl(site.getOrigin(), site.getName(), site.getType(), site.getBasis(), siteStart, siteLength, site.getPrecision(),
                site.getStrand(), site.getOriginalSequence(), site.getComment(), site.getProperties());

    }

    /**
     * Adds site to the track
     * @param site - site to add
     * Note that site name should be actually sequence name (chromosome number)
     * Site properties can contain integers, doubles, strings and DataElements
     * DataElements will be properly serialized only if all sites have DataElements from the same collection for given property name
     */
    @Override
    public void addSite(Site site) throws LoggedException
    {
        site = ensureBounds(site);
        if( site == null )
            return;
        Position sitePosition = new Position(site);
        if( defaultPosition == null || defaultPosition.compareTo(sitePosition) > 0 )
            defaultPosition = sitePosition;
        if( !isValid )
            return;
        if( inserter == null )
        {
            prepareAddStatement();
        }
        checkProperties(site);
        DynamicPropertySet prop = site.getProperties();
        Object[] values = new Object[siteProperties.size() + 6];
        String chrom = site.getOriginalSequence() != null ? site.getOriginalSequence().getName() : site.getName();
        int siteId = nextAvailableId();
        values[0] = siteId;
        values[1] = chrom;
        values[2] = site.getFrom();
        values[3] = site.getTo();
        values[4] = site.getStrand();
        values[5] = site.getType();

        for(DynamicProperty dp : prop)
        {
            String name = dp.getName();
            Object value = dp.getValue();
            if( value == null )
                continue;
            if( "profile".equals(name) && value instanceof double[] )
            {
                if( !hasProfiles )
                {
                    createProfileSQLTable();
                    hasProfiles = true;
                }
                if( profileInserter == null )
                    profileInserter = new FastBulkInsert(this, profileTableId, new String[] {"site_id", "position", "value"});
                double[] profileValues = (double[])value;
                for( int i = site.getFrom(); i <= site.getTo(); i++ )
                    profileInserter.insert(new Object[] {siteId, i, profileValues[i - site.getFrom()]});
            }
            else if( value instanceof DataElement )
            {
                if( ( (DataElement)value ).getOrigin()!= null
                        && !getInfo().getProperties().containsKey(DE_PROPERTY_COLLECTION_PREFIX + name) )
                {
                    getInfo().getProperties().setProperty(DE_PROPERTY_COLLECTION_PREFIX + name,
                            ( (DataElement)value ).getOrigin().getCompletePath().toString());
                    propertyDataCollections.put(name, ( (DataElement)value ).getOrigin());
                }
                values[siteProperties.get(name).index - 1] = ( (DataElement)value ).getName();
            }
            else
            {
                values[siteProperties.get(name).index - 1] = value.toString();
            }
        }
        countAllSites();
        size++;
        nameList = null;
        inserter.insert(values);
    }

    private int maxSiteId = -2;

    private int nextAvailableId()
    {
        if(maxSiteId == -2)
            maxSiteId = SqlUtil.queryInt( getConnection(), query( "SELECT MAX(id) FROM $table$" ));
        return ++maxSiteId;
    }

    @Override
    public void finalizeAddition() throws BiosoftSQLException
    {
        finalizeAddition( true );
    }
    
    private void finalizeAddition(boolean buildIndex)
    {
        if( inserter != null )
            inserter.flush();
        if( profileInserter != null )
            profileInserter.flush();
        if(buildIndex)
            buildIndex();
        maxSiteLength = -1;
        getInfo().getProperties().remove( MAX_SITE_LENGTH_PROPERTY );
        mutable = false;
        if( defaultPosition != null )
        {
            setDefaultPosition(defaultPosition);
        }
        getCompletePath().save( this );
        
    }
    
    private void buildIndex()
    {
        Query query = new Query( "SHOW INDEX FROM $table$ WHERE Key_name ='chrom'" ).name( getTableId() );
        if(!SqlUtil.hasResult( getConnection(), query ))
        {
            query = new Query( "create unique index chrom on $table$ (chrom,start,id)").name(getTableId());
            SqlUtil.execute( getConnection(), query);
        }
    }

    public void setDefaultPosition(Position position)
    {
        Sequence sequence = chrCache.getSequence(position.getSequence());
        Interval interval = position.getInterval().zoom(1.5);
        int minLength = (int)Math.sqrt(sequence.getLength() / 1000.0);
        if( interval.getLength() < minLength )
            interval = interval.zoomToLength(minLength);
        interval = interval.fit(sequence.getInterval());
        getInfo().getProperties().setProperty(DEFAULT_POSITION_PROPERTY, new Position(position.getSequence(), interval).toString());
    }

    @Override
    public @Nonnull
    Class<AnnotatedSequence> getDataElementType()
    {
        return AnnotatedSequence.class;
    }

    public void setDescription(String description)
    {
        getInfo().setDescription( TextUtil.nullToEmpty( description ) );
        try
        {
            getCompletePath().save(this);
        }
        catch( Exception e )
        {
            ExceptionRegistry.log(e);
        }
    }

    @Override
    public @Nonnull Iterator<AnnotatedSequence> iterator()
    {
        if( !isValid )
            return ListUtil.emptyIterator();
        updateLabel();
        return new SQLIterator<>();
    }

    @Override
    public StreamEx<AnnotatedSequence> stream()
    {
        return StreamEx.of( spliterator() );
    }

    private Reference<DataCollection<Site>> allSitesCollection = null;
    @Override
    public synchronized @Nonnull DataCollection<Site> getAllSites()
    {
        if( !isValid )
            return new VectorDataCollection<>("", Site.class, null);
        DataCollection<Site> dc = allSitesCollection == null ? null : allSitesCollection.get();
        if( dc == null )
        {
            dc = new SitesCollection(null);
            allSitesCollection = new WeakReference<>(dc);
        }
        return dc;
    }

    public static SqlTrack createTrack(DataElementPath path, Track parent)
    {
        return createTrack( path, parent, null, SqlTrack.class );
    }

    public static SqlTrack createTrack(DataElementPath path, Track parent, DataElementPath seqCollectionPath)
    {
        return createTrack( path, parent, seqCollectionPath, SqlTrack.class );
    }

    public static SqlTrack createTrack(DataElementPath path, Track parent, Class<? extends Track> clazz)
    {
        return createTrack( path, parent, null, clazz );
    }

    /**
     * Creates SqlTrack for given path and returns it.
     * @param path path to create the track in (must accept SqlTrack as child element)
     * @param parent if not null, track meta-information will be copied from the parent
     * @param seqCollectionPath path to sequences collection, can be null in which case SEQUENCES_COLLECTION_PROPERTY from parent will be used
     * @param clazz - class of desired result track will be used if assignable from SqlTrack.class 
     * @return created track
     */
    public static SqlTrack createTrack(DataElementPath path, Track parent, DataElementPath seqCollectionPath, Class<? extends Track> clazz)
    {
        path.remove();
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, path.getName());
        if( parent instanceof DataCollection )
        {
            DataCollectionInfo info = ( (DataCollection<?>)parent ).getInfo();
            Object labelProperty = info.getProperty(LABEL_PROPERTY);
            if( labelProperty != null )
                properties.put(LABEL_PROPERTY, labelProperty);
            if( seqCollectionPath == null )
                seqCollectionPath = DataElementPath.create(info.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
        }

        if( seqCollectionPath != null )
            properties.setProperty(Track.SEQUENCES_COLLECTION_PROPERTY, seqCollectionPath.toString());

        SqlTrack result = createTrackWithClass( path.optParentCollection(), properties, clazz );
        if( parent instanceof DataCollection )
        {
            DataCollectionUtils.copyPersistentInfo(result, (DataCollection<?>)parent);
            //restore icon property if it was replaced with parent's
            result.getInfo().setNodeImageLocation( SqlTrack.class, result.getNodeImageIcon() );
        }
        return result;
    }

    private static SqlTrack createTrackWithClass(DataCollection<?> origin, Properties properties, Class<? extends Track> clazz)
    {
        if( clazz != null && SqlTrack.class.isAssignableFrom( clazz ) )
        {
            try
            {
                Constructor<?> constructor = clazz.getConstructor( ru.biosoft.access.core.DataCollection.class, Properties.class );
                return (SqlTrack)constructor.newInstance( origin, properties );
            }
            catch( Exception e )
            {
            }
        }
        return new SqlTrack( origin, properties );
    }

    @Override
    public final SqlTrack clone(DataCollection parent, String name)
    {
        SqlTrack result;
        try
        {
            result = createTrack(DataElementPath.create(parent, name), this, this.getClass());
            for( Site site : getAllSites() )
            {
                result.addSite(site);
            }
            result.finalizeAddition();
            DataCollectionUtils.copyAnalysisParametersInfo( this, result );
        }
        catch( Exception e )
        {
            throw new RuntimeException(e);
        }
        return result;
    }

    public DataElementPath getChromosomesPath()
    {
        return DataElementPath.create(getInfo().getProperty(SEQUENCES_COLLECTION_PROPERTY));
    }

    @PropertyDescription ( "Genome (sequences collection)" )
    public GenomeSelector getGenomeSelector()
    {
        return genomeSelector;
    }

    public void setGenomeSelector(GenomeSelector genomeSelector)
    {
        if( genomeSelector != this.genomeSelector )
        {
            GenomeSelectorTrackUpdater listener = new GenomeSelectorTrackUpdater( SqlTrack.this, genomeSelector, () -> {
                chrCache.clear();
                String seqBaseStr = getInfo().getProperties().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY);
                if( seqBaseStr != null )
                {
                    DataElementPath seqBase = DataElementPath.create(seqBaseStr);
                    chrCache.setSeqBase( seqBase );
                }
                return;
            } );
            genomeSelector.addPropertyChangeListener( listener );
        }
        this.genomeSelector = genomeSelector;
    }

    protected String getNodeImageIcon()
    {
        return "resources/track.gif";
    }

    public class SitesCollection extends AbstractDataCollection<Site> implements SortableDataCollection<Site>
    {
        private final SubSequence seq;
        private int size = -1;
        private final String whereClause;
        protected List<String> nameList = null;

        private SitesCollection(SubSequence seq)
        {
            super(SqlTrack.this.getName(), SqlTrack.this.getOrigin(), null);
            this.seq = seq;
            this.whereClause = getWhereClause(seq);
        }

        @Override
        protected void cachePut(Site de)
        {
            super.cachePut(de);
        }

        @Override
        public int getSize()
        {
            try
            {
                if( size >= 0 )
                    return size;
                if( whereClause == null )
                    size = countAllSites();
                else
                    size = SqlUtil.queryInt(getConnection(), Query.count(getTableId()) + " WHERE " + whereClause);
                return size;
            }
            catch( Exception e )
            {
                throw new DataElementReadException(e, getCompletePath(), "size");
            }
        }

        @Override
        public @Nonnull
        List<String> getNameList()
        {
            if( nameList == null )
            {
                nameList = new SqlList(SqlTrack.this, Query.field(tableId, "id") + ( whereClause == null ? "" : " WHERE " + whereClause ),
                        getSize());
            }
            return nameList;
        }

        @Override
        public Site doGet(String name) throws BiosoftSQLException
        {
            String query = Query.byCondition(tableId, "id", name) + ( whereClause == null ? "" : " AND " + whereClause );
            Statement st = null;
            ResultSet rs = null;
            try
            {
                st = SqlUtil.createStatement(getConnection());
                rs = SqlUtil.executeAndAdvance(st, query);
                if( rs != null )
                    return createSite(seq, rs);
            }
            finally
            {
                SqlUtil.close(st, rs);
            }
            return null;
        }

        @Override
        public @Nonnull
        Iterator<Site> iterator()
        {
            if( seq == null )
                return new SitesIterator(this);
            //return new SqlPagingIterator<>( SqlTrack.this, tableId, "id", whereClause, initTableStatus(), rs->createSite(seq,rs) );
            return new OverlappingSitesIterator( SqlTrack.this, seq );
        }

        @Override
        public StreamEx<Site> stream()
        {
            return StreamEx.of(iterator());
        }

        private String lastSortClause = "";
        private List<String> lastSortedNameList;

        @Override
        public synchronized List<String> getSortedNameList(String fieldName, boolean direction)
        {
            String sortClause = getOrderByClause(fieldName, direction);
            if( !sortClause.equals(lastSortClause) )
            {
                try
                {
                    lastSortClause = sortClause;
                    lastSortedNameList = SqlUtil.queryStrings(getConnection(), Query.field(tableId, "id")
                            + ( whereClause == null ? "" : " WHERE " + whereClause ) + " ORDER BY " + sortClause);
                }
                catch( BiosoftSQLException e )
                {
                    e.log();
                }
            }
            return lastSortedNameList;
        }

        private String getOrderByClause(String fieldName, boolean direction)
        {
            if( fieldName.equals(SitesToTableTransformer.PROPERTY_SEQUENCE) )
                return direction ? "chrom,start" : "chrom DESC,start DESC";
            if( fieldName.equals(SitesToTableTransformer.PROPERTY_FROM) )
                return direction ? "start" : "start DESC";
            if( fieldName.equals(SitesToTableTransformer.PROPERTY_TO) )
                return direction ? "end" : "end DESC";
            if( fieldName.equals(SitesToTableTransformer.PROPERTY_LENGTH) )
                return direction ? "end-start,chrom,start" : "end-start DESC,chrom,start";
            if( fieldName.equals(SitesToTableTransformer.PROPERTY_TYPE) )
                return direction ? "type,chrom,start" : "type DESC,chrom,start";
            if( fieldName.equals(SitesToTableTransformer.PROPERTY_STRAND) )
                return direction ? "strand,chrom,start" : "strand DESC,chrom,start";
            if( fieldName.startsWith(SitesToTableTransformer.PROPERTY_PREFIX) )
            {
                String colName = PROPERTY_COLUMN_PREFIX + fieldName.substring(SitesToTableTransformer.PROPERTY_PREFIX.length());
                colName = SqlUtil.quoteIdentifier( colName );
                return direction ? colName + ",chrom,start"
                                  : colName + " DESC,chrom,start";
            }
            return direction ? "id" : "id DESC";
        }

        @Override
        public boolean isSortingSupported()
        {
            return getSize() < ApplicationUtils.getMaxSortingSize();
        }

        @Override
        public String[] getSortableFields()
        {
            Set<String> props = siteProperties.keySet();
            String[] result = new String[props.size() + 6];
            int i = 0;
            for( String prop : props )
                result[i++] = SitesToTableTransformer.PROPERTY_PREFIX + prop;
            result[i++] = SitesToTableTransformer.PROPERTY_SEQUENCE;
            result[i++] = SitesToTableTransformer.PROPERTY_FROM;
            result[i++] = SitesToTableTransformer.PROPERTY_TO;
            result[i++] = SitesToTableTransformer.PROPERTY_LENGTH;
            result[i++] = SitesToTableTransformer.PROPERTY_TYPE;
            result[i++] = SitesToTableTransformer.PROPERTY_STRAND;
            return result;
        }

        @Override
        public Iterator<Site> getSortedIterator(String field, boolean direction, int from, int to)
        {
            if( seq == null && from == 0 && to >= getSize() && !isSortingSupported() )
                return new SitesIterator(this);
            return new SQLIterator<>(seq, true, "ORDER BY " + getOrderByClause(field, direction), from, to);
        }
    }

    public static class SitesCollectionBeanInfo extends BeanInfoEx
    {
        public SitesCollectionBeanInfo()
        {
            super(SitesCollection.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_TRACK_INFO"));
            beanDescriptor.setShortDescription(getResourceString("CD_TRACK_INFO"));
        }

        @Override
        public void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("size", beanClass, "getSize", null));
        }
    }

    /**
     * Implement Iterator for iterate SqlDataCollection elements.
     * @param T either {@link Site} or {@link AnnotatedSequence}
     * @see SQLDataCollection
     */
    protected class SQLIterator<T extends DataElement> implements Iterator<T>
    {
        /** Current result set for iterate through. */
        private ResultSet resultSet;
        private String whereClause;
        private String orderClause;
        private SubSequence seq;
        private Statement statement;
        private boolean sitesMode = false;
        private int rowNumber = -1;
        private int maxRowNumber;
        private int lastQueryRowNumber;
        private int queryLength;

        /**
         * Create iterator.
         * Ask DBMS for all DataElements and fill resultSet.
         */
        public SQLIterator(SubSequence seq, boolean sitesMode)
        {
            this(seq, sitesMode, "ORDER BY " + (sitesMode ? "id" : labelField), 0, getSize());
        }

        /**
         * Create iterator.
         * Ask DBMS for all DataElements and fill resultSet.
         */
        public SQLIterator(SubSequence seq, boolean sitesMode, String orderClause, int from, int to)
        {
            this.seq = seq;
            this.whereClause = getWhereClause(seq);
            this.orderClause = orderClause;
            this.sitesMode = sitesMode;
            this.queryLength = Math.min(to - from, initTableStatus());
            this.maxRowNumber = to-1;
            init(from);
        }

        public SQLIterator()
        {
            this(null, false);
        }

        private void init(int skip)
        {
            try
            {
                if( statement != null )
                    statement.close();
                statement = SqlUtil.createStatement(getConnection());
                resultSet = SqlUtil.executeQuery(statement, Query.all(tableId) + ( whereClause == null ? "" : " WHERE " + whereClause )
                        + " " + orderClause + " LIMIT " + skip + "," + queryLength);
                lastQueryRowNumber = 0;
                if( !resultSet.next() )
                {
                    resultSet.close();
                    resultSet = null;
                }
            }
            catch( Exception exc )
            {
                throw new DataElementReadException(exc, getCompletePath(), "sites");
            }
            rowNumber = skip;
        }

        @Override
        protected void finalize() throws Throwable
        {
            SqlUtil.close(statement, resultSet);
        }

        /**
         * Implement Iterator.hasNext().
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext()
        {
            return resultSet != null;
        }

        /**
         * Implement Iterator.next().
         * @see java.util.Iterator#next()
         */
        @Override
        public T next()
        {
            if( !hasNext() )
            {
                throw new NoSuchElementException("SqlTrack.iterator() has no more elements.");
            }
            try
            {
                T element = null;
                if( sitesMode )
                    element = (T)createSite(seq, resultSet);
                else
                {
                    String id = resultSet.getString(1);
                    element = (T)v_cache.get(id);
                    if( element == null )
                        element = (T)createChildElementFromResultSet(resultSet);
                    v_cache.put(id, (AnnotatedSequence)element);
                }
                
                rowNumber++;
                lastQueryRowNumber++;
                
                if(rowNumber > maxRowNumber)
                {
                    resultSet.close();
                    resultSet = null;
                }
                else if( !resultSet.next() )
                {
                    resultSet.close();
                    if( lastQueryRowNumber < queryLength )
                        resultSet = null;
                    else
                        init(rowNumber);
                }

                return element;
            }
            catch( Exception exc )
            {
                throw new DataElementReadException(exc, getCompletePath(), "sites");
            }
        }
    }// end of SQLIterator

    protected class SitesIterator implements Iterator<Site>
    {
        private Statement statement;
        private ResultSet resultSet;
        private int startId = 0;
        private int maxId;
        private final int maxQueryLength;
        private final SitesCollection sitesCollection;

        public SitesIterator(SitesCollection sitesCollection)
        {
            maxQueryLength = initTableStatus();
            this.sitesCollection = sitesCollection;
            try
            {
                maxId = SqlUtil.queryInt(getConnection(), query("SELECT max(id) FROM $table$"));
                nextResultSet();
                advance();
            }
            catch( Exception e )
            {
                throw new DataElementReadException(e, getCompletePath(), "sites");
            }
        }

        @Override
        protected void finalize() throws Throwable
        {
            SqlUtil.close(statement, resultSet);
        }

        @Override
        public boolean hasNext()
        {
            return resultSet != null;
        }

        private void advance() throws BiosoftSQLException
        {
            try
            {
                while( !resultSet.next() )
                {
                    if( startId > maxId )
                    {
                        resultSet = null;
                        return;
                    }
                    SqlUtil.close(statement, resultSet);
                    nextResultSet();
                }
            }
            catch( SQLException e )
            {
                throw new BiosoftSQLException(SqlTrack.this, e);
            }
        }

        private void nextResultSet() throws BiosoftSQLException
        {
            statement = SqlUtil.createStatement(getConnection());
            resultSet = SqlUtil.executeQuery(statement, query("SELECT * FROM $table$ WHERE id BETWEEN $from$ AND $to$")
                    .num("from", startId).num("to", startId + maxQueryLength));
            startId = startId + maxQueryLength + 1;
        }

        @Override
        public Site next()
        {
            if( !hasNext() )
                throw new NoSuchElementException();
            Site result = null;
            try
            {
                result = createSiteFromResultSet(null, resultSet);
                if( sitesCollection != null )
                    sitesCollection.cachePut(result);
                advance();
            }
            catch( BiosoftSQLException e )
            {
                throw new DataElementReadException(e, getCompletePath(), "site");
            }
            return result;
        }
    }
}
