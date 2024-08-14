package biouml.plugins.gtrd.master.meta;

import java.util.ArrayList;
import java.util.List;

public class TF
{
    public String uniprotId;//"P35869"
    public String uniprotName;//"AHR_HUMAN"
    public List<String> uniprotProteinNames;//["Aryl hydrocarbon receptor", "Ah receptor", "AhR", "Class E basic helix-loop-helix protein 76", "bHLHe76"],
    public List<String> uniprotGeneNames;// ["AHR", "BHLHE76"],
    public String organism;//"Homo sapiens",
    public String uniprotStatus;// "reviewed",
    public String tfClassId;// "1.2.5.1.1"

    public TF()
    {
    }
    
    public TF(TF tf)
    {
        this.uniprotId = tf.uniprotId;
        this.uniprotName = tf.uniprotName;
        this.organism = tf.organism;
        this.uniprotStatus = tf.uniprotStatus;
        this.tfClassId = tf.tfClassId;
        this.uniprotProteinNames = new ArrayList<>( tf.uniprotProteinNames );
        this.uniprotGeneNames = new ArrayList<>( tf.uniprotGeneNames );
    }


}
