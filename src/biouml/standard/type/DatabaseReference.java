package biouml.standard.type;

import java.util.Objects;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.support.SerializableAsText;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.beans.Option;

/**
 * General definition of reference to external  database.
 *
 * Database reference can be stored as text in following format:
 * DatabaseName; ID[; AC][; comment]
 */
public class DatabaseReference extends Option implements SerializableAsText
{
    private static final long serialVersionUID = -4331440516388287611L;
    
    public static final @Nonnull DataElementPath STUB_PATH = DataElementPath.create("stub");

    public DatabaseReference()
    {
    }

    /** Copy constructor. */
    public DatabaseReference(DatabaseReference original)
    {
        databaseName = original.databaseName;
        id = original.id;
        ac = original.ac;
        comment = original.comment;
        relationshipType = original.relationshipType;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( ac, comment, databaseName, databaseVersion, id, idVersion, relationshipType );
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null || getClass() != obj.getClass() )
            return false;
        DatabaseReference other = (DatabaseReference)obj;
        return Objects.equals( ac, other.ac ) &&
                Objects.equals( comment, other.comment ) &&
                Objects.equals( databaseName, other.databaseName ) &&
                Objects.equals( databaseVersion, other.databaseVersion ) &&
                Objects.equals( id, other.id ) &&
                Objects.equals( idVersion, other.idVersion ) &&
                Objects.equals( relationshipType, other.relationshipType );
    }

    //
    public DatabaseReference(String text)
    {
        StringTokenizer tokens = new StringTokenizer(text, ";", true);

        databaseName = nextToken(tokens);
        id = nextToken(tokens);
        ac = nextToken(tokens);
        databaseVersion = nextToken(tokens);
        idVersion = nextToken(tokens);
        relationshipType = nextToken(tokens);
        if(tokens.hasMoreElements())
            comment = tokens.nextToken().replaceAll("^ ", "");
        while(tokens.hasMoreElements())
            comment += tokens.nextToken();
    }

    public DatabaseReference(Element element)
    {
        //DBref path example: stub/db_name/id/ac
        if( !element.getElementPath().isDescendantOf( STUB_PATH ) )
            throw new IllegalArgumentException("Supplied element not supported: " + element);
        String[] properties = element.getElementPath().getPathComponents();
        if( properties.length <= 3 )
            throw new IllegalArgumentException("Supplied element not supported: " + element);
        setDatabaseName(properties[1]);
        setId(properties[2]);
        setAc(properties[3]);
    }
    
    public DatabaseReference(String dbName, String id, String acc)
    {
        setDatabaseName( dbName );
        setId( id );
        setAc( acc );
    }
    
    public DatabaseReference(String dbName, String id)
    {
        this(dbName, id, id);
    }

    protected String nextToken(StringTokenizer tokens)
    {
        String value = null;
        if( tokens.hasMoreElements() )
        {
            value = tokens.nextToken().trim();
            if( value.length() == 0 || value.equals(";") || value.equals("null") )
                value = null;

            // read next delimiter
            if( tokens.hasMoreElements() )
                tokens.nextToken();
        }

        return value;
    }

    @Override
    public String getAsText()
    {
        StringBuffer result = new StringBuffer();//databaseName);

        if (databaseName != null)
        {
            result.append( databaseName );
        }
        result.append("; ");
        if( id != null )
        {
            result.append( id );
        }

        result.append("; ");
        if( ac != null )
        {
            result.append(ac);
        }

        result.append("; ");
        if( databaseVersion != null )
        {
            result.append(databaseVersion);
        }

        result.append("; ");
        if( idVersion != null )
        {
            result.append(idVersion);
        }

        result.append("; ");
        if( relationshipType != null )
        {
            result.append(relationshipType);
        }

        result.append("; ");
        if( comment != null )
        {
            result.append(comment);
        }

        return result.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /** Database name. */
    protected String databaseName;
    public String getDatabaseName()
    {
        return databaseName;
    }
    public void setDatabaseName(String databaseName)
    {
        String oldValue = this.databaseName;
        this.databaseName = databaseName;
        firePropertyChange("databaseName", oldValue, databaseName);
    }

    /** Database version. */
    protected String databaseVersion;
    public String getDatabaseVersion()
    {
        return databaseVersion;
    }
    public void setDatabaseVersion(String databaseVersion)
    {
        this.databaseVersion = databaseVersion;
    }

    /** Record ID (primary key) in referenced database. */
    protected String id;
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        String oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id);
    }

    /** ID version. */
    protected String idVersion;
    public String getIdVersion()
    {
        return idVersion;
    }
    public void setIdVersion(String idVersion)
    {
        this.idVersion = idVersion;
    }

    /** Relationship type */
    protected String relationshipType;
    public String getRelationshipType()
    {
        return relationshipType;
    }
    public void setRelationshipType(String relationshipType)
    {
        String oldValue = this.relationshipType;
        this.relationshipType = relationshipType;
        firePropertyChange("relationshipType", oldValue, relationshipType);
    }

    /** Record AC (secondary key) in referenced database. */
    protected String ac;
    public String getAc()
    {
        return ac;
    }
    public void setAc(String ac)
    {
        String oldValue = this.ac;
        this.ac = ac;
        firePropertyChange("ac", oldValue, ac);
    }

    /** Arbitrary comment. */
    protected String comment;
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        String oldValue = this.comment;
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }

    @Override
    public String toString()
    {
        return "" + getDatabaseName() + ": " + getId() + ", " + getRelationshipType() + ( getAc() == null ? "" : " (" + getAc() + ")" )
                + ( getComment() == null ? "" : "; " + getComment() );
    }

    public Element convertToElement()
    {
        return new Element( STUB_PATH.getChildPath( getDatabaseName(), TextUtil2.nullToEmpty( getId() ), TextUtil2.nullToEmpty( getAc() ) ) );
    }
}