package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

/**Defines common properties for diagram description. */
public class DiagramInfo extends Referrer
{
    private String created;
    private String[] modified = new String[0];
    private AuthorInfo[] authors = new AuthorInfo[0];
            
    public DiagramInfo(String name)
    {
        super(null, name, DIAGRAM_INFO);
    }

    public DiagramInfo(DataCollection parent, String name)
    {
        super(parent, name, DIAGRAM_INFO);
    }

    /**
     * @pending clone all properties.
     */
    public DiagramInfo clone(String newName)
    {
       return clone(getOrigin(), newName);
    }    
    
    @Override
    public DiagramInfo clone(DataCollection<?> newOrigin, String newName)
    {
        DiagramInfo newInfo = new DiagramInfo(newOrigin, newName);

        newInfo.setTitle       ( getTitle() );
        newInfo.setDate        ( getDate() );
        newInfo.setComment     ( getComment() );
        newInfo.setDescription ( getDescription() );
        newInfo.setAuthors( getAuthors().clone() );
        newInfo.setCreated( getCreated() );
        newInfo.setModified( getModified().clone() );

        if( getLiteratureReferences() != null )
        {
            String[] refs = getLiteratureReferences().clone();
            newInfo.setLiteratureReferences(refs);
        }

        if( getDatabaseReferences() != null )
        {
            DatabaseReference[] refs = StreamEx.of( getDatabaseReferences() ).map( DatabaseReference::new )
                    .peek( ref -> ref.setParent( newInfo ) ).toArray( DatabaseReference[]::new );
            newInfo.setDatabaseReferences(refs);
        }
        return newInfo;
    }
    
    @PropertyName("Authors")
    public AuthorInfo[] getAuthors()
    {
        return authors;
    }
    public void setAuthors(AuthorInfo[] authors)
    {
        this.authors = authors;
    }

    @PropertyName("Modified")
    public String[] getModified()
    {
        return modified;
    }
    public void setModified(String[] modified)
    {
        this.modified = modified;
    }

    @PropertyName("Created")
    public String getCreated()
    {
        return created;
    }
    public void setCreated(String created)
    {
        this.created = created;
    }

    public static class AuthorInfo
    {
        private String familyName;
        private String givenName;
        private String email;
        private String orgName;

        @PropertyName ( "Family name" )
        public String getFamilyName()
        {
            return familyName;
        }
        public void setFamilyName(String familyName)
        {
            this.familyName = familyName;
        }

        @PropertyName ( "Given name" )
        public String getGivenName()
        {
            return givenName;
        }
        public void setGivenName(String givenName)
        {
            this.givenName = givenName;
        }

        @PropertyName ( "Email" )
        public String getEmail()
        {
            return email;
        }
        public void setEmail(String email)
        {
            this.email = email;
        }

        @PropertyName ( "Organisation" )
        public String getOrgName()
        {
            return orgName;
        }
        public void setOrgName(String orgName)
        {
            this.orgName = orgName;
        }
    }
    public static class AuthorInfoBeanInfo extends BeanInfoEx2<AuthorInfo>
    {
        public AuthorInfoBeanInfo()
        {
            super( AuthorInfo.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "familyName" );
            add( "givenName" );
            add( "email" );
            add( "orgName" );
        }
    }
}