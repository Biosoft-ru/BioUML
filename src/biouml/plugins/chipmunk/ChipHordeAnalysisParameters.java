package biouml.plugins.chipmunk;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;


/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
@PropertyName("ChIPHorde analysis")
@PropertyDescription("Multiple motif search")
public class ChipHordeAnalysisParameters extends ChipMunkAnalysisParameters
{
    private int nMotifs = 3;
    private String mode = AbstractChipMunkParameters.HORDE_MODES[0];

    @PropertyName("Motifs count limit")
    @PropertyDescription("Maximum number of motifs to discover")
    public int getNMotifs()
    {
        return nMotifs;
    }

    public void setNMotifs(int nMotifs)
    {
        Object oldValue = this.nMotifs;
        this.nMotifs = nMotifs;
        firePropertyChange("nMotifs", oldValue, nMotifs);
    }

    @PropertyName("Filtering mode")
    @PropertyDescription("Whether to mask polyN (\"Mask\") or to drop entire sequence (\"Filter\")")
    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        Object oldValue = this.mode;
        this.mode = mode;
        firePropertyChange("mode", oldValue, mode);
    }
    
    @Override
    public @Nonnull String[] getOutputNames()
    {
        return new String[] {"outputLibrary"};
    }

    @Override
    @PropertyName ( "Matrix name prefix" )
    @PropertyDescription ( "Prefix for the matrix name. It will be followed by number." )
    public String getMatrixName()
    {
        return super.getMatrixName();
    }
}
