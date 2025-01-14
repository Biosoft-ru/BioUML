package biouml.plugins.sbol;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.ColorUtils;

public class SbolDiagramViewOptions extends DiagramViewOptions
{
    private Pen backbonePen = new Pen(2, ColorUtils.parseColor("#515151"));

    public SbolDiagramViewOptions(Option parent)
    {
        super(parent);
        this.setDiagramTitleVisible( false );
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


    @Override
    @PropertyName("Auto-layout edges")
    @PropertyDescription("Layout diagram edges automatically")
    public boolean isAutoLayout()
    {
        return autoLayout;
    }
}
