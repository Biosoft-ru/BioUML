package biouml.plugins.keynodes;

import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import biouml.plugins.keynodes.biohub.KeyNodesHub;

public class BioHubSelector extends GenericComboBoxEditor
{
    Class<? extends KeyNodesHub> hubClass = KeyNodesHub.class;

    @Override
    public BioHubInfo[] getAvailableValues()
    {
        boolean expertMode = true;
        try
        {
            expertMode = ( (AbstractAnalysisParameters)getBean() ).isExpertMode();
        }
        catch( Exception e )
        {
        }
        return getAvailableValues(hubClass, expertMode);
    }

    public static BioHubInfo[] getAvailableValues(Class<? extends KeyNodesHub> hubClass, boolean addExpertHubs)
    {
        return BioHubRegistry.bioHubs( new TargetOptions( new CollectionRecord( "KeyNodesHub", true ) ), addExpertHubs )
                .filter( hub -> hubClass.isInstance( hub.getBioHub() ) ).toArray( BioHubInfo[]::new );
    }
}