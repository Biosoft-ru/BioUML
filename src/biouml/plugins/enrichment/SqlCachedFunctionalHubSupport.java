package biouml.plugins.enrichment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Module;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.standard.type.Species;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BioHubFetchException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.sql.BulkInsert;
import ru.biosoft.access.sql.FastBulkInsert;
import ru.biosoft.access.sql.RewriteBulkInsert;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.Util;

/**
 * Support class to implement hubs suitable for functional analysis which generate mapping table
 * (group -> ENSG) during the first call and then use it
 * @author lan
 */
public abstract class SqlCachedFunctionalHubSupport extends BioHubSupport
{
    public static final String HUB_PRIORITY_PROPERTY = "hubPriority";

    /**
     * Represents single group or category
     * @author lan
     */
    public static class Group
    {
        private final String title;
        private final String accession;
        private final Set<String> elements;
        
        /**
         * Create group
         * @param accession
         * @param title
         * @param elements
         */
        public Group(String accession, String title, Set<String> elements)
        {
            super();
            this.accession = accession;
            this.title = title;
            this.elements = elements;
        }
        
        public Group(String accession, String title)
        {
            this(accession, title, new HashSet<String>());
        }
    
        public Group(String accession)
        {
            this(accession, accession);
        }
    
        /**
         * @return the title
         */
        public String getTitle()
        {
            return title;
        }
    
        /**
         * @return the accession
         */
        public String getAccession()
        {
            return accession;
        }
    
        /**
         * @return the elements
         */
        public Set<String> getElements()
        {
            return elements;
        }
        
        /**
         * Adds an element to the Group
         * @param element element to add
         */
        public void addElement(String element)
        {
            this.elements.add(element);
        }
        
        @Override
        public String toString()
        {
            return this.accession+"/"+this.title+": "+this.elements;
        }
    }

    protected Logger log = Logger.getLogger(SqlCachedFunctionalHubSupport.class.getName());
    protected ThreadLocal<Connection> connection = new ThreadLocal<>();
    protected Boolean tableWorking = null;
    /**
     * @return list of all Group objects supported by this hub
     * @throws Exception
     * This method is executed in privileged context (you have admin rights inside it)
     */
    abstract protected Iterable<Group> getGroups() throws Exception;
    
    /**
     * ReferenceType of elements returned by getGroups() for further BioHub matching to Ensembl gene
     * @return ReferenceType object registered via {@link ReferenceTypeRegistry}
     */
    abstract protected ReferenceType getInputReferenceType();
    
    /**
     * @return name of cached SQL table
     */
    abstract protected String getTableName();

    /**
     * Subclasses may override this to add custom annotation to the element
     * @param element element to annotate
     */
    protected void annotateElement(Element element)
    {
    }

    /**
     * Generate path for new Element object by group accession
     * May be overridden
     * @param id - group accession to generate path on
     * @return generated path
     */
    protected String getElementPath(String id)
    {
        return "stub/%//"+id;
    }

    public SqlCachedFunctionalHubSupport(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return getPriority( dbOptions, DataElementPath.create( FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD ),
                () -> Integer.parseInt( properties.getProperty( HUB_PRIORITY_PROPERTY, "5" ) ) );
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        Map<Element, Element[]> references = getReferences(new Element[] {startElement}, dbOptions, relationTypes, maxLength, direction);
        return references.get(startElement);
    }

    @Override
    public @Nonnull Map<Element, Element[]> getReferences(Element[] startElements, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        boolean hitsMode = false;
        Species species = null;
        Species defSpecies = Species.getDefaultSpecies(null);
        for( DataElementPath cr : dbOptions.getUsedCollectionPaths() )
        {
            if(cr.toString().equals(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD))
            {
                hitsMode = true;
            }
            DataElement collection = CollectionFactory.getDataElement(cr.getName());
            if(collection == null) continue;
            Module module = Module.optModule(collection);
            if(module == null) continue;
            defSpecies = Species.getDefaultSpecies(module);
        }
        try
        {
            SecurityManager.runPrivileged(() -> {
                generateTable();
                return null;
            });
        }
        catch( Throwable t )
        {
            throw new BioHubFetchException(t, this);
        }
        try {
        	Connection conn = getConnection();
            Map<String, Element> elementsCache = new HashMap<>();
            Map<Element, Element[]> result = new HashMap<>();
            try( PreparedStatement ps = conn
                    .prepareStatement( "SELECT pathway_id,pathway_title,species FROM " + getTableName() + " WHERE ensembl_id=?" ) )
            {
                for( Element startElement : startElements )
                {
                    ps.setString( 1, startElement.getAccession() );
                    try( ResultSet resultSet = ps.executeQuery() )
                    {
                        Set<Element> elementSet = new HashSet<>();
                        while( resultSet.next() )
                        {
                            if( species == null && hitsMode )
                            {
                                try
                                {
                                    species = Species.getSpecies( resultSet.getString( 3 ) );
                                }
                                catch( Exception e )
                                {
                                }
                            }
                            String name = resultSet.getString( 1 );
                            if( !elementsCache.containsKey( name ) )
                            {
                                Element element = new Element( getElementPath( name ) );
                                element.setValue( "Title", resultSet.getString( 2 ) );
                                annotateElement( element );
                                elementsCache.put( name, element );
                            }
                            elementSet.add( elementsCache.get( name ) );
                        }
                        result.put( startElement, elementSet.toArray( new Element[elementSet.size()] ) );
                    }
                }
            }
    
            if(!hitsMode) return result;
            if(species == null) species = defSpecies;
            String tmpTableName = "tmp_genes_"+getShortName().toLowerCase().replaceAll("\\W", "")+"_"+Util.getUniqueId();
            SqlUtil.execute(conn, "CREATE TEMPORARY TABLE "+tmpTableName+" (ensembl_id varchar(32), primary key(ensembl_id))");
            BulkInsert inserter = new FastBulkInsert(conn, tmpTableName);
            for(Element startElement: startElements)
            {
                inserter.insert(new Object[] {startElement.getAccession()});
            }
            inserter.flush();
            int totalGenes = SqlUtil.queryInt(conn, "SELECT COUNT(DISTINCT ensembl_id) FROM " + getTableName() + " WHERE species = '"+species.getLatinName()+"'");
            int totalHits = SqlUtil.queryInt(conn, "SELECT COUNT(DISTINCT ensembl_id) FROM " + getTableName() + " JOIN "+tmpTableName+" USING(ensembl_id) WHERE species = '"+species.getLatinName()+"'");
            DynamicProperty[] defaultProperties = {
                    new DynamicProperty(FunctionalHubConstants.INPUT_GENES_DESCRIPTOR, Integer.class, totalHits),
                    new DynamicProperty(FunctionalHubConstants.TOTAL_GENES_DESCRIPTOR, Integer.class, totalGenes)
            };
            String tmpTableName2 = tmpTableName+"_pw";
            int nameLength = 0;
            for(String elementName: elementsCache.keySet())
            {
            	if (elementName == null) continue;
                nameLength = Math.max(elementName.length(), nameLength);
            }
            SqlUtil.execute(conn, "CREATE TEMPORARY TABLE "+tmpTableName2+" (pathway_id varchar("+(nameLength+2)+"), primary key(pathway_id))");
            inserter = new FastBulkInsert(conn, tmpTableName2);
            for(Element element: elementsCache.values())
            {
                for(DynamicProperty property: defaultProperties) element.setValue(property);
                inserter.insert(new Object[] {element.getAccession()});
            }
            inserter.flush();
            SqlUtil.iterate(
                    conn,
                    "SELECT p.pathway_id,COUNT(*) FROM " + getTableName() + " JOIN " + tmpTableName2
                            + " p USING(pathway_id) WHERE species = '" + species.getLatinName() + "' GROUP BY p.pathway_id",
                    rs -> elementsCache.get( rs.getString( 1 ) ).setValue(
                            new DynamicProperty( FunctionalHubConstants.GROUP_SIZE_DESCRIPTOR, Integer.class, rs.getInt( 2 ) ) ) );
            SqlUtil.dropTable(conn, tmpTableName);
            SqlUtil.dropTable(conn, tmpTableName2);
            return result;
        }
        catch( Throwable t )
        {
        	throw new BioHubFetchException(t, this);
        }
    }

    final protected synchronized Connection getConnection() throws BiosoftSQLException
    {
        if( connection.get() == null )
        {
            connection.set(doGetConnection());
        }
        return connection.get();
    }

    /**
     * @return SQL connection used by this object
     * @throws SQLException
     */
    protected Connection doGetConnection() throws BiosoftSQLException
    {
        DataElementPath path = getModulePath();
        if(path != null)
        {
            return SqlConnectionPool.getConnection(path.getDataCollection());
        }
        return SqlConnectionPool.getPersistentConnection(properties);
    }

    public synchronized void generateTable() throws Exception
    {
        if(tableWorking == null)
        {
            try
            {
                SqlUtil.execute(getConnection(), "SELECT pathway_id,pathway_title,ensembl_id,species FROM " + getTableName() + " LIMIT 1");
                tableWorking = true;
            }
            catch( BiosoftSQLException e )
            {
                tableWorking = false;
            }
        }
        if(tableWorking == true) return;
        log.info("Hub table '"+getTableName()+"' doesn't exist: generating process started.");
        
        Iterable<Group> groups = getGroups();
        
        Set<String> allMolecules = new HashSet<>();
        int groupNameLength = 0;
        int groupTitleLength = 0;
        for(Group group: groups)
        {
            allMolecules.addAll(group.getElements());
            if(groupNameLength < group.getAccession().length())
                groupNameLength = group.getAccession().length();
            if(groupTitleLength < group.getTitle().length())
                groupTitleLength = group.getTitle().length();
        }
        
        Connection conn = getConnection();
        SqlUtil.dropTable(conn, getTableName() + "_tmp");
        SqlUtil.execute(conn,
                "CREATE TABLE " + getTableName() + "_tmp(" +
                    "pathway_id varchar(" + ( groupNameLength + 2 ) + "), " +
                    "pathway_title varchar(" + ( groupTitleLength + 2 ) + "), " +
                    "ensembl_id varchar(32), " +
                    "species varchar(48), " +
                    "key(pathway_id), " +
                    "key(ensembl_id)" +
                ") ENGINE=MyISAM");
        BulkInsert inserter = new RewriteBulkInsert(conn, getTableName() + "_tmp");
    
        for(Species species: Species.allSpecies())
        {
            Properties inputProperties = new Properties();
            inputProperties.put(BioHub.TYPE_PROPERTY, getInputReferenceType().toString());
            inputProperties.put(BioHub.SPECIES_PROPERTY, species.getName());
            Properties outputProperties = new Properties();
            outputProperties.put(BioHub.TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).toString());
            outputProperties.put(BioHub.SPECIES_PROPERTY, species.getName());
            Map<String, String[]> references = BioHubRegistry.getReferences(allMolecules.toArray(new String[allMolecules.size()]),
                    inputProperties, outputProperties, null);
            if( references == null )
                continue;
            for(Group group: groups)
            {
                for(String molName: group.getElements())
                {
                    String[] molRefs = references.get(molName);
                    if(molRefs == null) continue;
                    for(String molRef: molRefs)
                    {
                        inserter.insert(new Object[] {group.getAccession(), group.getTitle(), molRef, species.getName()});
                    }
                }
            }
        }
        inserter.flush();
        
        SqlUtil.dropTable(conn, getTableName());
        SqlUtil.execute(conn,
            "CREATE TABLE " + getTableName() + "(" +
                "pathway_id varchar(" + ( groupNameLength + 2 ) + "), " +
                "pathway_title varchar(" + ( groupTitleLength + 2 ) + "), " +
                "ensembl_id varchar(32), " +
                "species varchar(48), " +
                "key(pathway_id, species), " +
                "key(ensembl_id)" +
            ") ENGINE=MyISAM");
        SqlUtil.execute(conn, "INSERT INTO " + getTableName() + " SELECT DISTINCT pathway_id,pathway_title,ensembl_id,species FROM " + getTableName() + "_tmp");
        SqlUtil.dropTable(conn, getTableName() + "_tmp");
        log.info("Hub table '"+getTableName()+"': generating process finished.");
        tableWorking = true;
    }
}
