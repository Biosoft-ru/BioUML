package biouml.plugins.keynodes;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;

import java.util.Properties;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.UserCollectionBioHub;
import biouml.standard.type.Species;

@SuppressWarnings ( "serial" )
@PropertyName ( "Parameters" )
public class BasicKeyNodeAnalysisParameters extends AbstractAnalysisParameters
{
    protected DataElementPath sourcePath;
    private Species species;
    protected BioHubInfo bioHub;
    protected DataElementPath customHubCollection;

    public BasicKeyNodeAnalysisParameters()
    {
        setSpecies( Species.getDefaultSpecies( null ) );
        BioHubInfo[] hubs = BioHubSelector.getAvailableValues( KeyNodesHub.class, false );
        if( hubs.length != 0 )
            setBioHub( hubs[0] );
    }

    public TableDataCollection getSource()
    {
        return ( sourcePath == null || ! ( sourcePath.optDataElement() instanceof TableDataCollection ) ) ? null
                : (TableDataCollection)sourcePath.optDataElement();
    }

    public void setSource(TableDataCollection source)
    {
        setSourcePath( DataElementPath.create( source ) );
    }

    @PropertyName ( "Molecules collection" )
    @PropertyDescription ( "Input the collection of molecules/genes" )
    public DataElementPath getSourcePath()
    {
        return sourcePath;
    }

    public void setSourcePath(DataElementPath sourcePath)
    {
        DataElementPath oldValue = this.sourcePath;
        this.sourcePath = sourcePath;
        firePropertyChange( "sourcePath", oldValue, this.sourcePath );
        if( sourcePath != null )
            setSpecies( Species.getDefaultSpecies( sourcePath.optDataCollection() ) );
    }

    @PropertyName ( "Species" )
    @PropertyDescription ( "Species to which analysis should be confined" )
    public Species getSpecies()
    {
        return species;
    }
    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange( "species", oldValue, species );
    }

    private UserCollectionBioHub userCollectionBioHub;
    public KeyNodesHub<?> getKeyNodesHub()
    {
        BioHubInfo bhi = getBioHub();
        if( bhi == null )
            return null;
        if( UserCollectionBioHub.CUSTOM_REPOSITORY_HUB_NAME.equals( bhi.getName() ) )
        {
            DataElementPath customCollection = getCustomHubCollection();
            if( userCollectionBioHub == null || customCollection == null
                    || !customCollection.equals( userCollectionBioHub.getCustomCollectionPath() ) )
            {
                userCollectionBioHub = new UserCollectionBioHub( new Properties(), customCollection );
            }
            return userCollectionBioHub;
        }
        else
            return (KeyNodesHub<?>)bhi.getBioHub();
    }

    @PropertyName ( "Search collection" )
    @PropertyDescription ( "Collection containing reactions" )
    public BioHubInfo getBioHub()
    {
        return bioHub;
    }
    public void setBioHub(BioHubInfo bioHub)
    {
        Object oldValue = this.bioHub;
        this.bioHub = bioHub;
        firePropertyChange( "bioHub", oldValue, this.bioHub );
        firePropertyChange( "*", null, null );
    }

    public boolean isCustomHubCollectionHidden()
    {
        if( getBioHub() == null )
            return true;
        return !UserCollectionBioHub.CUSTOM_REPOSITORY_HUB_NAME.equals( getBioHub().getName() );
    }

    @PropertyName ( "Custom search collection" )
    @PropertyDescription ( "Path to the custom search collection" )
    public DataElementPath getCustomHubCollection()
    {
        return customHubCollection;
    }
    public void setCustomHubCollection(DataElementPath customHubCollection)
    {
        Object oldValue = this.customHubCollection;
        this.customHubCollection = customHubCollection;
        firePropertyChange( "customHubCollection", oldValue, this.customHubCollection );
    }
}
