package biouml.plugins.gtrd.analysis.maos;

import biouml.plugins.enrichment.FunctionalHubConstants;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.analysis.maos.ParametersBeanInfo;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class AdvancedParametersBeanInfo extends ParametersBeanInfo
{
    public AdvancedParametersBeanInfo()
    {
        super(AdvancedParameters.class);
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        
        int beforeOutputs = findPropertyIndex( "outputTable" );
        property( "psdHub" ).simple().editor( PsdHubSelector.class ).hidden( "isPsdHubHidden" ).add( beforeOutputs );
        property( "addPsdAnnotation" ).add( beforeOutputs );
        property( "transpathHub" ).simple().editor( TranspathHubSelector.class ).hidden( "isTranspathHubHidden" ).add( beforeOutputs );
        property( "addTranspathAnnotation" ).add( beforeOutputs );
        property( "tfClassDepth" ).tags( "5","4","3" ).add( beforeOutputs );
        
        property("selectedGTRDPeaks").outputElement( SqlTrack.class ).auto( "$vcfTrack$_gtrd_peaks" ).add();
    }
    
    public static class TranspathHubSelector extends GenericComboBoxEditor
    {
        private static final TargetOptions dbOptions = new TargetOptions(new CollectionRecord(
                FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD, true));
    
        @Override
        public BioHubInfo[] getAvailableValues()
        {
            return BioHubRegistry.bioHubs( dbOptions, true ).filter( this::isTranspathPathways ).toArray( BioHubInfo[]::new );
        }
        
        private boolean isTranspathPathways(BioHubInfo info)
        {
            return info.getBioHub().getClass().getName().endsWith( ".PathwayFunctionalHub" );
        }
    }
    
    public static class PsdHubSelector extends GenericComboBoxEditor
    {
        private static final TargetOptions dbOptions = new TargetOptions(new CollectionRecord(
                FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD, true));
    
        @Override
        public BioHubInfo[] getAvailableValues()
        {
            return BioHubRegistry.bioHubs( dbOptions, true ).filter( this::isPsdHub ).toArray( BioHubInfo[]::new );
        }
        
        private boolean isPsdHub(BioHubInfo info)
        {
            return info.getBioHub().getClass().getName().endsWith( ".BKLDiseaseHub" );
        }
    }
}
