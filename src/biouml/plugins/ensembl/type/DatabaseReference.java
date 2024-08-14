package biouml.plugins.ensembl.type;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification of relationship between BuiUML DatabaseReference and
 * Ensembl ExternalRefs types:
 * <pre>
 * biouml.standard.type.DatabaseReference properties    <-> org.ensembl.datamodel.ExternalRefs properties
 * 
 * ac                                                   <-> primaryID
 * 
 * asText                                               <-> ?
 * 
 * ?                                                    <-> infoText
 * 
 * comment                                              <-> description
 * 
 * databaseName                                         <-> getExternalDatabase().getName()
 * 
 * ?                                                    <-> getExternalDatabase().getPriority()
 * 
 * ?                                                    <-> getExternalDatabase().getStatus()
 * 
 * ?                                                    <-> getExternalDatabase().getVersion()
 * 
 * id                                                   <-> displayID
 * 
 * ?                                                    <-> externalDbId
 * 
 * ?                                                    <-> goLincageType
 * 
 * ?                                                    <-> infoType
 * 
 * ?                                                    <-> objectXrefID
 * 
 * ?                                                    <-> queryIdentity
 * 
 * ?                                                    <-> queryInternalID
 * 
 * ?                                                    <-> synonyms
 * 
 * ?                                                    <-> targetIdentity
 * 
 * ?                                                    <-> version
 * </pre>
 */
@SuppressWarnings ( "serial" )
public class DatabaseReference extends biouml.standard.type.DatabaseReference
{

    protected String version = "";
    protected String info = "";
    protected List<String> synonyms = new ArrayList<>();

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        String oldValue = this.version;
        this.version = version;
        firePropertyChange("version", oldValue, version);
    }

    public String getInfo()
    {
        return info;
    }

    public void setInfo(String info)
    {
        String oldValue = this.info;
        this.info = info;
        firePropertyChange("info", oldValue, info);
    }

    public List<String> getSynonyms()
    {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms)
    {
        List<String> oldValue = this.synonyms;
        this.synonyms = synonyms;
        firePropertyChange("synonyms", oldValue, synonyms);
    }

    @Override
    public String toString()
    {
        return getDatabaseName() + " (" + String.join( "; ", getSynonyms() ) + "; " + getVersion() + "): " + getId() + " (" + getAc()
                + "); " + getComment() + " " + getInfo();
    }

}
