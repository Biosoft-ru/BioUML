package biouml.plugins.ensembl.type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ensembl.datamodel.ExternalRef;
import org.ensembl.driver.CoreDriver;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.WithSite;

/**
 * Specification of relationship between BuiUML Gene and
 * Ensembl Gene types:
 * <pre>
 * biouml.standard.type.Gene properties <-> org.ensembl.datamodel.Gene properties
 *
 * chromosome                           <-> location (converted to the "chromosome" coordinate system)
 *
 * regulation                           <-> ?
 *
 * species                              <-> ?
 *
 * structureReferences                  <-> ?
 *
 * completeName                         <-> ?
 *
 * synonyms                             <-> ?
 *
 * databaseReferences                   <-> ? (with some changes) externalRefs
 *
 * description                          <-> description
 *
 * literatureReferences                 <-> ?
 *
 * comment                              <-> ?
 *
 * date                                 <-> modifiedDate
 *
 * title                                <-> displayName
 *
 * name                                 <-> accesionID
 *
 * ?                                    <-> status
 *
 * ?                                    <-> interproIDs
 *
 * ?                                    <-> version
 *
 * ?                                    <-> createdDate
 *
 * ?                                    <-> analysis
 *
 * ?                                    <-> analysisID
 *
 * ?                                    <-> driver (internal Ensembl Java API features)
 *
 * ?                                    <-> internalID (internal Ensembl Java API features)
 * </pre>
 */
@SuppressWarnings ( "serial" )
public class Gene extends biouml.standard.type.Gene implements WithSite
{
    private static Logger log = Logger.getLogger(Gene.class.getName());
    protected String status = "";
    protected int version = 0;
    protected String createdDate = null;
    protected CoreDriver driver;
    private Site site;

    public Gene(DataCollection<?> origin, String name, Site site, CoreDriver driver)
    {
        super(origin, name);
        this.driver = driver;
        this.site = site;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        String oldValue = this.status;
        this.status = status;
        firePropertyChange("status", oldValue, status);
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        int oldValue = this.version;
        this.version = version;
        firePropertyChange("version", oldValue, version);
    }

    public String getCreatedDate()
    {
        return createdDate;
    }

    public void setCreatedDate(String createdDate)
    {
        String oldValue = this.createdDate;
        this.createdDate = createdDate;
        firePropertyChange("createdDate", oldValue, createdDate);
    }

    @SuppressWarnings ( "unchecked" )
    @Override
    public biouml.standard.type.DatabaseReference[] getDatabaseReferences()
    {
        try
        {
            List<ExternalRef> reff = driver.getExternalRefAdaptor().fetch( getName() );
            Set<DatabaseReference> dbRefSet = new HashSet<>();
            if ( reff != null && reff.size ( ) > 0 )
            {
                for ( ExternalRef ref : reff )
                {
                    DatabaseReference databaseReference = new DatabaseReference ( );
                    databaseReference.setDatabaseName ( ref.getExternalDatabase ( ).getName ( ) );
                    databaseReference.setId ( ref.getDisplayID ( ) );
                    databaseReference.setInfo ( ref.getInfoText ( ) );
                    databaseReference.setComment ( ref.getDescription ( ) );
                    databaseReference.setAc ( ref.getPrimaryID ( ) );
                    databaseReference.setVersion ( ref.getVersion ( ) );
                    databaseReference.setSynonyms ( ref.getSynonyms ( ) );
                    dbRefSet.add( databaseReference );
                }
                return dbRefSet.toArray( new DatabaseReference[0] );
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "While fetching references for gene "+getName(), e);
        }
        return new biouml.standard.type.DatabaseReference[]{};
    }

    @Override
    public Site getSite()
    {
        return site;
    }
}
