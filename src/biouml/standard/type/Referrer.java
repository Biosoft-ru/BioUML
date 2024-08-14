package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;

/**
 * Extension of GenericEntity that contains references to other databases and literature sources.
 */
abstract public class Referrer extends GenericEntity
{
    private static final long serialVersionUID = -3116188284353933758L;

    /** The object textual description. Can be text/plain or text/html. */
    private String description;

    /** @pending make a special type for database references. */
    private DatabaseReference[] databaseReferences = null;
    /**
     * Array of literature references. Each string corresponds to
     * {@link ru.biosoft.access.core.DataElement} name from "literature" ru.biosoft.access.core.DataCollection.
     */
    private String[] literatureReferences;

    protected Referrer(DataCollection origin, String name)
    {
        super(origin, name);
    }

    protected Referrer(DataCollection origin, String name, String type)
    {
        super(origin, name, type);
    }

    @PropertyName ( "Description" )
    @PropertyDescription ( "The object textual description (plain text or HTML)." )
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        String oldValue = this.description;
        this.description = description;
        firePropertyChange("description", oldValue, description);
    }

    @PropertyName ( "Database references" )
    @PropertyDescription ( "Database cross-reference.<br>"
            + "Cross-references to other databases that contain information related to the entry. "
            + "For instance, line pointing to the relevant SWISS-PROT entry will be in the DR " + "field for the protein. "
            + "<p>General format is:" + "<pre>database_identifier; primary_identifier; secondary_identifier.</pre>" + "Example:"
            + "<pre>SWISS-PROT; P03593; V90K_AMV.</pre>" )
    public DatabaseReference[] getDatabaseReferences()
    {
        return databaseReferences;
    }
    public void setDatabaseReferences(DatabaseReference[] databaseReferences)
    {
        DatabaseReference[] oldValue = this.databaseReferences;
        this.databaseReferences = databaseReferences;
        if( databaseReferences != null )
            for( DatabaseReference dr : databaseReferences )
                dr.setParent(this);
        firePropertyChange("databaseReferences", oldValue, databaseReferences);
    }

    public void addDatabaseReferences(DatabaseReference ... newReferences)
    {
        if( newReferences == null )
            return;
        DatabaseReference[] curRefs = getDatabaseReferences();
        setDatabaseReferences(
                curRefs == null ? newReferences : StreamEx.of(curRefs).append(newReferences).distinct().toArray(DatabaseReference[]::new));
    }

    @PropertyName ( "Bibliography" )
    @PropertyDescription ( "The field contains references to the original papers. "
            + "The reference provides access to the paper within the database from which " + "the data has been extracted. "
            + "<p> The format is:" + "<pre>Reference identifier</pre>" + "Example:" + "<pre>Gilmour K.C. and Reich N.C., 1995</pre>" )
    public String[] getLiteratureReferences()
    {
        return literatureReferences;
    }
    public void setLiteratureReferences(String[] literatureReferences)
    {
        String[] oldValue = this.literatureReferences;
        this.literatureReferences = literatureReferences;
        firePropertyChange("literatureReferences", oldValue, literatureReferences);
    }

    public DatabaseReference[] getDatabaseReferences(String relationshipType)
    {
        if( databaseReferences == null )
            return new DatabaseReference[0];
        return StreamEx.of(databaseReferences).filter(ref -> ref.getRelationshipType().equals(relationshipType))
                .toArray(DatabaseReference[]::new);
    }

    @Override
    public Referrer clone(DataCollection<?> newOrigin, String newName)
    {
        Referrer clone = (Referrer)super.clone(newOrigin, newName);
        if( literatureReferences != null )
            clone.literatureReferences = literatureReferences.clone();
        if( databaseReferences != null )
            clone.databaseReferences = StreamEx.of(databaseReferences).map(DatabaseReference::new).toArray(DatabaseReference[]::new);
        return clone;
    }
}
