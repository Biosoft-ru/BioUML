package biouml.plugins.enrichment;

import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.journal.ProjectUtils;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

@SuppressWarnings ( "serial" )
@PropertyName("Functional classification")
@PropertyDescription("Functional classification")
public class FunctionalClassificationParameters extends AbstractAnalysisParameters
{
    protected DataElementPath sourcePath;
    protected int minHits;
    protected double pvalueThreshold;
    protected boolean onlyOverrepresented;
    protected DataElementPath outputTable;
    protected DataElementPath repositoryHubRoot, referenceCollection;
    protected BioHubInfo bioHub;
    protected Species species;

    public FunctionalClassificationParameters()
    {
        setSpecies(Species.getDefaultSpecies(null));
        minHits = 2;
        onlyOverrepresented = true;
        pvalueThreshold = 0.05;
        Object[] bioHubs = new BioHubSelector().getAvailableValues();
        if( bioHubs == null || bioHubs.length == 0 )
        {
            throw new UnsupportedOperationException("No functional classification hubs installed: this analysis is unavailable");
        }
        bioHub = (BioHubInfo)bioHubs[0];
    }

    public TableDataCollection getSource()
    {
        DataElement de = sourcePath == null ? null : sourcePath.optDataElement();
        return de instanceof TableDataCollection ? (TableDataCollection)de : null;
    }

    @PropertyName("Source data set")
    @PropertyDescription("Input table having Ensembl genes as rows.")
    public DataElementPath getSourcePath()
    {
        return sourcePath;
    }

    public void setSourcePath(DataElementPath sourcePath)
    {
        DataElementPath oldValue = this.sourcePath;
        this.sourcePath = sourcePath;
        firePropertyChange("sourcePath", oldValue, this.sourcePath);
        TableDataCollection table = getSource();
        setSpecies(Species.getDefaultSpecies(table));
    }

    @PropertyName("Minimal hits to group")
    @PropertyDescription("Groups with lower number of hits will be filtered out (n<sub>min</sub>)")
    public int getMinHits()
    {
        return minHits;
    }

    public void setMinHits(int minHits)
    {
        Object oldValue = this.minHits;
        this.minHits = minHits;
        firePropertyChange("minHits", oldValue, this.minHits);
    }

    @PropertyName("P-value threshold")
    @PropertyDescription("P-value threshold (P<sub>max</sub>)")
    public double getPvalueThreshold()
    {
        return pvalueThreshold;
    }

    public void setPvalueThreshold(double pvalueThreshold)
    {
        Object oldValue = this.pvalueThreshold;
        this.pvalueThreshold = pvalueThreshold;
        firePropertyChange("pvalueThreshold", oldValue, this.pvalueThreshold);
    }
    
    @PropertyName("Only over-represented")
    @PropertyDescription("If checked, under-represented groups will be excluded from the result")
    public boolean getOnlyOverrepresented()
    {
        return onlyOverrepresented;
    }

    public void setOnlyOverrepresented(boolean onlyOverrepresented)
    {
        Object oldValue = this.onlyOverrepresented;
        this.onlyOverrepresented = onlyOverrepresented;
        firePropertyChange("onlyOverrepresented", oldValue, this.onlyOverrepresented);
    }
    

    @PropertyName("Result name")
    @PropertyDescription("Name and path for the resulting table")
    public DataElementPath getOutputTable()
    {
        return outputTable;
    }

    public void setOutputTable(DataElementPath outputTable)
    {
        Object oldValue = this.outputTable;
        this.outputTable = outputTable;
        firePropertyChange("outputTable", oldValue, this.outputTable);
    }
    
    public BioHub getFunctionalHub()
    {
        BioHubInfo bioHub = getBioHub();
        if( bioHub.getName().equals( RepositoryHub.REPOSITORY_HUB_NAME ) )
            return new RepositoryHub( new Properties(), getRepositoryHubRoot(), getReferenceCollection() );
        else
            return bioHub.getBioHub();
    }

    @PropertyName("Classification")
    @PropertyDescription("Classification you want to use. List of classifications may differ depending on software version and your subscription. Use '"+RepositoryHub.REPOSITORY_HUB_NAME+"' for custom classification.")
    public BioHubInfo getBioHub()
    {
        return bioHub;
    }

    public String getHubShortName()
    {
        if( bioHub == null )
            return null;
        if( bioHub.isSpecial() )
            return getRepositoryHubRoot() == null ? null : getRepositoryHubRoot().getName();
        return bioHub.getBioHub().getShortName();
    }

    public void setBioHub(BioHubInfo bioHub)
    {
        Object oldValue = this.bioHub;
        this.bioHub = bioHub;
        firePropertyChange("bioHub", oldValue, this.bioHub);
    }

    @PropertyName("Species")
    @PropertyDescription("Species corresponding to the input table.")
    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, this.species);
    }

    @PropertyName("Path to classification root")
    @PropertyDescription ( "Specify path to the folder containing classification tables, when '" + RepositoryHub.REPOSITORY_HUB_NAME
            + "' is selected as classification. Only tables with 'Ensembl gene' type are used for the classification." )
    public DataElementPath getRepositoryHubRoot()
    {
        return repositoryHubRoot;
    }

    public void setRepositoryHubRoot(DataElementPath repositoryHubRoot)
    {
        Object oldValue = this.repositoryHubRoot;
        String oldHubName = getHubShortName();
        this.repositoryHubRoot = repositoryHubRoot;
        firePropertyChange("repositoryHubRoot", oldValue, repositoryHubRoot);
        firePropertyChange("hubShortName", oldHubName, getHubShortName());
    }

    @PropertyName("Reference collection")
    @PropertyDescription("If specified, this collection will be used as list of all Ensembl genes for custom classification. If not specified, list of all Ensembl genes will be created by combining all categories.")
    public DataElementPath getReferenceCollection()
    {
        return referenceCollection;
    }

    public void setReferenceCollection(DataElementPath referenceCollection)
    {
        Object oldValue = this.referenceCollection;
        this.referenceCollection = referenceCollection;
        firePropertyChange("referenceCollection", oldValue, referenceCollection);
    }

    public boolean isRepositoryHubRootHidden()
    {
        return !getBioHub().isSpecial();
    }

    @Override
    public void write(Properties properties, String prefix)
    {
        super.write(properties, prefix);
        properties.put(prefix + "plugin", "biouml.plugins.enrichment");
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"sourcePath"};
    }

    public static class BioHubSelector extends GenericComboBoxEditor
    {
        private static final TargetOptions dbOptions = new TargetOptions(new CollectionRecord(
                FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD, true));
    
        @Override
        public Object[] getAvailableValues()
        {
            boolean expertMode = true;
            DataElementPath projectPath = null;
            try
            {
                expertMode = ((FunctionalClassificationParameters)getBean()).isExpertMode();
                projectPath = ProjectUtils.getProjectPath( ( (FunctionalClassificationParameters)getBean() ).getOutputTable() );
            }
            catch( Exception e )
            {
            }
            return BioHubRegistry.bioHubs( dbOptions, expertMode, projectPath ).append( BioHubRegistry.specialHubs() )
                    .toArray( BioHubInfo[]::new );
        }
    }
}
