package biouml.plugins.pharm.prognostic;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AnalysisMethodElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class PopulationTreatmentParametersBeanInfo extends BeanInfoEx2<PopulationTreatmentParameters>
{
    public PopulationTreatmentParametersBeanInfo()
    {
        super( PopulationTreatmentParameters.class );
        beanDescriptor.setDisplayName( MessageBundle.getMessage( "PARAMETERS" ) );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "input" ).title( "INPUT_FILES" ).inputElement( FolderCollection.class ).add();
        property( "output" ).title( "OUTPUT_FILES" ).outputElement( FolderCollection.class ).add();
        property( "population" ).title( "POPULATION" ).inputElement( TableDataCollection.class ).add();
        property( "treatmentTime" ).title( "TREATMENT_TIME" ).expert().add();
        property( "drugs" ).title( "DRUGS" ).editor( DrugsEditor.class ).add();
        property( "generationInfo" ).title( "GENERATION_INFO" ).inputElement( AnalysisMethodElement.class ).add();
    }

    @Override
    public String getResourceString(String key)
    {
        return MessageBundle.getMessage( key );
    }

    public static class DrugsEditor extends GenericMultiSelectEditor
    {
        @Override
        public String[] getAvailableValues()
        {
            return StreamEx.of( PopulationTreatmentParameters.ALL_DRUGS ).map( drug -> drug.getTitle() ).sorted().toArray( String[]::new );
        }
    }
}
