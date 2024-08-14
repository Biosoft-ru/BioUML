package biouml.plugins.kegg;

import java.awt.Color;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import biouml.standard.diagram.PathwayDiagramViewOptions;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("View options")
public class KeggPathwayDiagramViewOptions extends PathwayDiagramViewOptions
{
    /**
     * Creates <code>PathwayDiagramViewOptions</code> and initializes it.
     *
     * @param parent   parent property
     */
    public KeggPathwayDiagramViewOptions(Option parent)
    {
        super(parent);
        this.substanceBrush = new Brush(Color.white);
        this.reactionBrush = new Brush(Color.black);
        this.reactionPen = new Pen(1, Color.black);
        this.autoLayout = true;
//        this.setPathLayouter(new KeggPathwayLayouter());
        setRelationTypeColors(new RelationTypeColor[] {new RelationTypeColor(DEFAULT, Color.black)});
        initSemanticRelationOptions();
    }
}
