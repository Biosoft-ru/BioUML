package biouml.plugins.pharm.prognostic;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PopulationGenerationParametersBeanInfo extends BeanInfoEx2<PopulationGenerationParameters>
{
    public PopulationGenerationParametersBeanInfo()
    {
        super( PopulationGenerationParameters.class );
        beanDescriptor.setDisplayName( MessageBundle.getMessage( "PARAMETERS" ) );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "input" ).title( "INPUT_FILES" ).inputElement( FolderCollection.class ).add();
        property( "output" ).title( "OUTPUT_FILES" ).outputElement( FolderCollection.class ).add();
        property( "patientsNumber" ).title( "PATIENTS_NUMBER" ).add();
        property( "patientPhysiology" ).title( "PATIENT_PHYSIOLOGY" ).add();
        property( "optimizationSettings" ).title( "OPTIMIZATION_SETTINGS" ).expert().add();
    }

    @Override
    public String getResourceString(String key)
    {
        return MessageBundle.getMessage( key );
    }
}
