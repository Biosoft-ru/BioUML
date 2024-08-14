package biouml.standard.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import biouml.model.DiagramViewOptions;
import biouml.standard.type.SemanticRelation;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
@PropertyName("View options")
public class PathwayDiagramViewOptions extends DiagramViewOptions
{
    protected static final String PEN     = "pen";
    protected static final String BRUSH   = "brush";
    protected static final String DEFAULT = "default";

    public PathwayDiagramViewOptions(Option parent)
    {
        super(parent);
        setDiagramTitleVisible(false);
        this.setCompartmentTitleAlignment(CompositeView.X_CC | CompositeView.Y_BB);
        initSemanticRelationOptions();
    }

    ///////////////////////////////////////////////////////////////////

    public Brush cellCytoplasmBrush = new Brush(Color.lightGray);
    public Brush cellNucleusBrush = new Brush(Color.black);
    public Brush geneBrush = new Brush(Color.orange.darker());

    public Brush proteinBrush = new Brush(Color.pink);
    public Brush activeProteinBrush = new Brush(Color.pink);
    public Brush inactiveProteinBrush = new Brush(Color.green);
    public Brush modifiedProteinBrush = new Brush(Color.red);
    
    public Brush physicalEntityBrush = new Brush(Color.white);
    
    public Pen   catalystActionPen  = new Pen(1, Color.magenta);
    public Pen   inhibitorActionPen = new Pen(1, Color.blue);
    public Pen   switchOnActionPen  = new Pen(1, Color.red);
    public Pen   switchOffActionPen = new Pen(1, Color.black);
    public Pen   reactionPen        = new Pen(1, Color.green.darker());

    public Brush increaseActionBrush  = new Brush(Color.magenta);
    public Brush decreaseActionBrush  = new Brush(Color.blue);
    public Brush switchOnActionBrush  = new Brush(Color.red);
    public Brush switchOffActionBrush = new Brush(Color.black);
    public Brush reactionBrush        = new Brush(Color.green.darker());

    public Brush substanceBrush = new Brush(Color.blue);

    public boolean automaticallyLocateReactions = false;
    public boolean showReactionName = false;

    ////////////////////////////////////////////////////////////////////////////
    // Semantic network specific settings
    //

    protected ColorFont conceptTitleFont = new ColorFont("Arial", Font.PLAIN, 12,  Color.black);
    public Pen conceptPen = new Pen(1, Color.black);
    
    protected ColorFont functionTitleFont = new ColorFont("Arial", Font.ITALIC, 12, Color.blue);

    protected ColorFont processTitleFont = new ColorFont("Arial", Font.PLAIN, 12, Color.black);
    
    protected ColorFont stateTitleFont = new ColorFont("Arial", Font.PLAIN, 12, Color.black);
    public Pen statePen = new Pen(1, Color.black);

    protected ColorFont relationTitleFont = new ColorFont("Arial", Font.PLAIN, 12, Color.black);

    /**
     * Margin between title and surrounding it raound rectangle.
     */
    protected Point titleMargin = new Point(3, 2);
    public Point getTitleMargin()
    {
        return titleMargin;
    }
    public void setTitleMargin(Point titleMargin)
    {
        Point oldValue = this.titleMargin;
        this.titleMargin = titleMargin;
        firePropertyChange("titleMargin", oldValue, titleMargin);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Semantic relation view issues
    //

    protected RelationTypeColor[] relationTypeColors =
    {
        new RelationTypeColor(DEFAULT, Color.blue.darker()),
    };
    public RelationTypeColor[] getRelationTypeColors()
    {
        return relationTypeColors;
    }
    public void setRelationTypeColors(RelationTypeColor[] relationTypeColors)
    {
        RelationTypeColor[] oldValue = relationTypeColors;
        this.relationTypeColors = relationTypeColors;
        firePropertyChange("relationTypeColors", oldValue, relationTypeColors);
    }

    protected RelationTypeStroke[] relationTypeStrokes =
    {
        new RelationTypeStroke(DEFAULT,                                 new BasicStroke(1)),
        new RelationTypeStroke(SemanticRelation.PARTICIPATION_DIRECT,   new BasicStroke(1)),
        new RelationTypeStroke(SemanticRelation.PARTICIPATION_UNKNOWN,  new BasicStroke(1)),
        new RelationTypeStroke(SemanticRelation.PARTICIPATION_INDIRECT,
                               new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                               new float[] {7, 3}, 0))
    };
    public RelationTypeStroke[] getRelationTypeStrokes()
    {
        return relationTypeStrokes;
    }
    public void setRelationTypeStrokes(RelationTypeStroke[] relationTypeStrokes)
    {
        RelationTypeStroke[] oldValue = relationTypeStrokes;
        this.relationTypeStrokes = relationTypeStrokes;
        firePropertyChange("relationTypeStrokes", oldValue, relationTypeStrokes);
    }


    protected Map<String, Object> semanticRelationOptions;
    public void initSemanticRelationOptions()
    {
        semanticRelationOptions = new HashMap<>();

        for( RelationTypeColor typeColor : relationTypeColors )
        {
            semanticRelationOptions.put( BRUSH + typeColor.getType(), new Brush(typeColor.getColor()) );

            for( RelationTypeStroke typeStroke : relationTypeStrokes )
            {
                semanticRelationOptions.put( PEN + typeColor.getType() + typeStroke.getType(),
                                             new Pen(typeStroke.getStroke(), typeColor.getColor()) );
            }
        }
    }

    public Pen getSemanticRelationPen(String relationType, String participationType)
    {
        Object pen = semanticRelationOptions.get(PEN + relationType + participationType);

        if(pen == null)
            pen = semanticRelationOptions.get(PEN + DEFAULT + participationType);

        if(pen == null)
            pen = semanticRelationOptions.get(PEN + relationType + DEFAULT);

        if(pen == null)
            pen = semanticRelationOptions.get(PEN + DEFAULT + DEFAULT);

        return (Pen)pen;
    }

    public Brush getSemanticRelationBrush(String relationType)
    {
        Object brush = semanticRelationOptions.get(BRUSH + relationType);

        if(brush == null)
            brush = semanticRelationOptions.get(BRUSH + DEFAULT);

        return (Brush)brush;
    }

    ////////////////////////////////////////////////////////////////////////////

    public ColorFont getStateTitleFont()
    {
        return stateTitleFont;
    }
    public void setStateTitleFont(ColorFont stateTitleFont)
    {
        this.stateTitleFont = stateTitleFont;
    }

    public static class RelationTypeColor
    {
        public RelationTypeColor(String type, Color color)
        {
            this.type  = type;
            this.color = color;
        }

        protected String type;
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            this.type = type;
        }

        protected Color color;
        public Color getColor()
        {
            return color;
        }
        public void setColor(Color color)
        {
            this.color = color;
        }
    }

    public static class RelationTypeStroke
    {
        public RelationTypeStroke(String type, BasicStroke stroke)
        {
            this.type  = type;
            this.stroke = stroke;
        }

        protected String type;
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            this.type = type;
        }

        protected BasicStroke stroke;
        public BasicStroke getStroke()
        {
            return stroke;
        }
        public void setStroke(BasicStroke stroke)
        {
            this.stroke = stroke;
        }
    }

}
