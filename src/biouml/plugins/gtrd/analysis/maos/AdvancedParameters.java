package biouml.plugins.gtrd.analysis.maos;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.analysis.maos.AdvancedParametersBeanInfo.PsdHubSelector;
import biouml.plugins.gtrd.analysis.maos.AdvancedParametersBeanInfo.TranspathHubSelector;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.bsa.analysis.maos.Parameters;

public class AdvancedParameters extends Parameters
{
    public AdvancedParameters()
    {
        setScoreType( PVALUE_SCORE_TYPE );
        initTranspath();
        initHumanPSD();
    }
    
    public void initTranspath()
    {
        TranspathHubSelector transpathSelector = new TranspathHubSelector();
        BioHubInfo[] hubList = transpathSelector.getAvailableValues();
        if(hubList.length > 0)
        {
            addTranspathAnnotation = true;
            transpathHub = hubList[0];
        }else
        {
            addTranspathAnnotation = false;
        }
    }
    
    private void initHumanPSD()
    {
        PsdHubSelector psdSelector = new PsdHubSelector();
        BioHubInfo[] hubList = psdSelector.getAvailableValues();
        if(hubList.length > 0)
        {
            addPsdAnnotation = true;
            psdHub = hubList[0];
        }else
        {
            addPsdAnnotation = false;
        }        
    }

    private String tfClassDepth = "5";
    @PropertyName("TF class depth")
    public String getTfClassDepth()
    {
        return tfClassDepth;
    }
    public void setTfClassDepth(String tfClassDepth)
    {
        String oldValue = this.tfClassDepth;
        this.tfClassDepth = tfClassDepth;
        firePropertyChange( "tfClassDepth", oldValue, tfClassDepth );
    }
    public int getTfClassDepthNumber()
    {
        return Integer.parseInt( tfClassDepth );
    }
    
    //Transpath params
    
    private boolean addTranspathAnnotation = true;
    @PropertyName("Add transpath annotation")
    @PropertyDescription("Transpath pathways annotation will be added to the output table")
    public boolean isAddTranspathAnnotation()
    {
        return addTranspathAnnotation;
    }
    public void setAddTranspathAnnotation(boolean addTranspathAnnotation)
    {
        boolean oldValue = this.addTranspathAnnotation;
        this.addTranspathAnnotation = addTranspathAnnotation;
        firePropertyChange( "addTranspathAnnotation", oldValue, addTranspathAnnotation );
    }
    
    public boolean isTranspathHubHidden()
    {
        return !isAddTranspathAnnotation();
    }

    private BioHubInfo transpathHub;
    @PropertyName("Transpath")
    public BioHubInfo getTranspathHub()
    {
        return transpathHub;
    }
    public void setTranspathHub(BioHubInfo transpathHub)
    {
        Object oldValue = this.transpathHub;
        this.transpathHub = transpathHub;
        firePropertyChange( "transpathHub", oldValue, transpathHub );
    }
    
    //HumanPSD params
    
    private BioHubInfo psdHub;
    @PropertyName("HumanPSD")
    public BioHubInfo getPsdHub()
    {
        return psdHub;
    }
    public void setPsdHub(BioHubInfo psdHub)
    {
        Object oldValue = this.psdHub;
        this.psdHub = psdHub;
        firePropertyChange( "psdHub", oldValue, psdHub );
    }

    private boolean addPsdAnnotation = true;
    @PropertyName("Add HumanPSD annotation")
    @PropertyDescription("HumanPSD disease annotation will be added to the output table")
    public boolean isAddPsdAnnotation()
    {
        return addPsdAnnotation;
    }
    public void setAddPsdAnnotation(boolean addPsdAnnotation)
    {
        boolean oldValue = this.addPsdAnnotation;
        this.addPsdAnnotation = addPsdAnnotation;
        firePropertyChange( "addPsdAnnotation", oldValue, addPsdAnnotation );
    }
    public boolean isPsdHubHidden()
    {
        return !isAddPsdAnnotation();
    }
    
    //GTRD

    private DataElementPath selectedGTRDPeaks;
    @PropertyName("Selected GTRD peaks/metaclusters")
    public DataElementPath getSelectedGTRDPeaks()
    {
        return selectedGTRDPeaks;
    }
    public void setSelectedGTRDPeaks(DataElementPath selectedGTRDPeaks)
    {
        Object oldValue = this.selectedGTRDPeaks;
        this.selectedGTRDPeaks = selectedGTRDPeaks;
        firePropertyChange( "selectedGTRDPeaks", oldValue, selectedGTRDPeaks );
    }
}
