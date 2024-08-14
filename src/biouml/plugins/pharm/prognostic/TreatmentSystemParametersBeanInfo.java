package biouml.plugins.pharm.prognostic;

import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class TreatmentSystemParametersBeanInfo extends BeanInfoEx2<TreatmentSystemParameters>
{
    
    public TreatmentSystemParametersBeanInfo()
    {
        super( TreatmentSystemParameters.class );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        addWithTags("regime", TreatmentSystemParameters.availableRegimes);
        add("generalData");
        add("pressure");
        add("ecg");
        add("heartUltrasound");
        add("bloodTest");  
        add("biochemistry");
        addWithoutChildren("drugs", DrugsEditor.class);
        property( "resultPath" ).outputElement( HtmlDataElement.class ).add();   
        addExpert("populationSize");
        addExpert("time");
        addExpert("showPlots");
        addExpert("fastRegimeDiagramPath");
        addExpert("slowRegimeDiagramPath");
        addExpert("fastPopulationPath");
        addExpert("slowPopulationPath");
    }
    
    
    public static class DrugsEditor extends GenericMultiSelectEditor
    {
        @Override
        public String[] getAvailableValues()
        {
            return TreatmentSystemParameters.availableDrugs;
        }
    }
}
