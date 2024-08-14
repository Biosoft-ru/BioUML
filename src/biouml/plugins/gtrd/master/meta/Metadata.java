package biouml.plugins.gtrd.master.meta;

import java.util.HashMap;
import java.util.Map;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPathSet;

public class Metadata implements DataElement
{
    public TF tf = new TF();
    public Map<String, ChIPseqExperiment> chipSeqExperiments = new HashMap<>();
    public Map<String, ChIPexoExperiment> chipExoExperiments = new HashMap<>();
    public Map<String, HistonesExperiment> histoneExperiments = new HashMap<>();
    public Map<String, DNaseExperiment> dnaseExperiments = new HashMap<>();
    public Map<String, MNaseExperiment> mnaseExperiments = new HashMap<>();
    public Map<String, FAIREExperiment> faireExperiments = new HashMap<>();
    public Map<String, ATACExperiment> atacExperiments = new HashMap<>();
    
    public Map<String, CellLine> cells = new HashMap<>();
    
    public DataElementPathSet siteModels = new DataElementPathSet();
    public BuildInfo buildInfo = new BuildInfo();
    
    public Metadata() {  }
    public Metadata(Metadata m) {
        this.tf = new TF(m.tf);
        chipSeqExperiments.putAll( m.chipSeqExperiments );
        chipExoExperiments.putAll( m.chipExoExperiments );
        histoneExperiments.putAll( m.histoneExperiments );
        dnaseExperiments.putAll( m.dnaseExperiments );
        mnaseExperiments.putAll( m.mnaseExperiments );
        faireExperiments.putAll( m.faireExperiments );
        atacExperiments.putAll( m.atacExperiments );
        cells.putAll( m.cells );
        siteModels = m.siteModels;
        buildInfo = new BuildInfo(m.buildInfo);
    }
    
    public int version = 1;//version of master track
    public int getVersion()
    {
        return version;
    }
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    //ru.biosoft.access.core.DataElement implementation
    private DataCollection<?> origin;
    private String name;
    
    @Override
    public DataCollection<?> getOrigin()
    {
        return origin;
    }
    public void setOrigin(DataCollection<?> origin)
    {
        this.origin = origin;
    }
    @Override
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

}
