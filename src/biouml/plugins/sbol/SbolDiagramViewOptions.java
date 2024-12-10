package biouml.plugins.sbol;

import com.developmentontheedge.beans.Option;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.ColorUtils;

public class SbolDiagramViewOptions extends DiagramViewOptions
{
    private Pen backbonePen = new Pen(2, ColorUtils.parseColor("#515151"));

    public SbolDiagramViewOptions(Option parent)
    {
        super(parent);
    }

    public Pen getBackbonePen()
    {
        return backbonePen;
    }

    public void setBackbonePen(Pen backbonePen)
    {
        Object oldValue = this.backbonePen;
        this.backbonePen = backbonePen;
        firePropertyChange("backbonePen", oldValue, this.backbonePen);
    }

}
