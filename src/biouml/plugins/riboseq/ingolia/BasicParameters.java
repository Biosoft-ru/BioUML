package biouml.plugins.riboseq.ingolia;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class BasicParameters extends CoreParametersWithASiteTable
{
    private int minWindowFootprints = 50;
    @PropertyName("Min footprints")
    @PropertyDescription("Minimal number of footprints in scoring window")
    public int getMinWindowFootprints()
    {
        return minWindowFootprints;
    }
    public void setMinWindowFootprints(int minWindowFootprints)
    {
        final int oldValue = this.minWindowFootprints;
        this.minWindowFootprints = minWindowFootprints;
        firePropertyChange( "minWindowFootprints", oldValue, this.minWindowFootprints );
    }
    
    private int minASiteFootprints = 10;
    @PropertyName("Min A-site footprints")
    @PropertyDescription("Minimal number of footprints in ribosome A-site")
    public int getMinASiteFootprints()
    {
        return minASiteFootprints;
    }
    public void setMinASiteFootprints(int minASiteFootprints)
    {
        int oldValue = this.minASiteFootprints;
        this.minASiteFootprints = minASiteFootprints;
        firePropertyChange( "minASiteFootprints", oldValue, minASiteFootprints );
    }

    private int windowOverhangs = -1;
    @PropertyName("Window overhangs")
    @PropertyDescription("Minimal distance between scoring window bounds and transcript bounds, -1 for disabling")
    public int getWindowOverhangs()
    {
        return windowOverhangs;
    }
    public void setWindowOverhangs(int windowOverhangs)
    {
        final int oldValue = this.windowOverhangs;
        this.windowOverhangs = windowOverhangs;
        firePropertyChange( "windowOverhangs", oldValue, this.windowOverhangs );
    }

    public ObservationListBuilder createObservationListBuilder()
    {
        ObservationBuilder observationBuilder = new ObservationBuilder();
        observationBuilder.setMinWindowFootrpints( getMinWindowFootprints() );
        observationBuilder.setMinASiteFootprints( minASiteFootprints );
        observationBuilder.setWindowOverhangs( getWindowOverhangs() );
        return new ObservationListBuilder( observationBuilder, createProfileBuilder(), createAlignmentConverter() );
    }

}
