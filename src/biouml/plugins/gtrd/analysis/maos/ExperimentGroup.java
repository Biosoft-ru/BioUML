package biouml.plugins.gtrd.analysis.maos;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.gtrd.ChIPseqExperiment;

//Group of experiments with the same tfClass,cell and treatment
public class ExperimentGroup
{
    private String tfClass;//of level 3,4 or 5
    private List<ChIPseqExperiment> expList = new ArrayList<>();
    
    public ExperimentGroup(String tfClass)
    {
        this.tfClass = tfClass;
    }
    
    public String getTfClass()
    {
        return tfClass;
    }
    
    public String getCell()
    {
        return expList.get( 0 ).getCell().getTitle();
    }
    
    public String getTreatment()
    {
        return expList.get( 0 ).getTreatment();
    }
    
    public List<ChIPseqExperiment> getExperiments()
    {
        return expList;
    }
    
    public void addExperiment(ChIPseqExperiment e)
    {
        expList.add( e );
    }
}
