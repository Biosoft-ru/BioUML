package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElementSupport;

/**
 * Unit to specify kinetic constat, parameter or variable units.
 *
 * @pending refine the definition according CellML/SBML approach
 */
public class RelationType extends MutableDataElementSupport
{
    public RelationType(DataCollection parent, String name)
    {
        super(parent, name);
        title = name;
    }

    /**
     * Relation title, we preserve ID, but we can change the title.
     */
    protected String title;
    public String getTitle()
    {
        return title;
    }
    public void setTitle (String title)
    {
        String oldValue = this.title;
        this.title = title;
        firePropertyChange("title", oldValue, title);
    }

    /** The object textual description. Can be text/plain or text/html. */
    private String description;
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        String oldValue = this.description;
        this.description = description;
        firePropertyChange("description", oldValue, description);
    }

    /** Arbitrary comment. */
    private String comment;
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        String oldValue = this.comment;
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }

    /** JavaScript function to generate stroke for this relation. */
    private String stroke;
    public String getStroke()
    {
        return stroke;
    }
    public void setStroke(String stroke)
    {
        String oldValue = this.stroke;
        this.stroke = stroke;
        firePropertyChange("stroke", oldValue, stroke);
    }
}
