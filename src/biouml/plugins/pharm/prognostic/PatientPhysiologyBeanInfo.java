package biouml.plugins.pharm.prognostic;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PatientPhysiologyBeanInfo extends BeanInfoEx2<PatientPhysiology>
{
    public PatientPhysiologyBeanInfo()
    {
        super( PatientPhysiology.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "generalData" ).title( "GENERAL_DATA" ).add();
        property( "pressure" ).title( "PRESSURE" ).add();
        property( "ecg" ).title( "ECG" ).add();
        property( "heartUltrasound" ).title( "HEART_ULTRASOUND" ).add();
        property( "bloodTest" ).title( "BLOOD_TEST" ).add();
        property( "biochemistry" ).title( "BIOCHEMISTRY" ).add();
        property( "diseases" ).title( "DISEASES" ).add();
        property( "genetics" ).title( "GENETICS" ).add();
        property( "calculatedParameters" ).title( "CALCULATED_PARAMETERS" ).add();
    }

    @Override
    public String getResourceString(String key)
    {
        return MessageBundle.getMessage( key );
    }
}
