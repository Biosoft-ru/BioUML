package biouml.workbench.diagram.viewpart;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.SubDiagram;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class SubDiagramSimple implements DataElement
{
    private SubDiagram subDiagram;

    public SubDiagramSimple()
    {

    }

    public SubDiagramSimple(SubDiagram s)
    {
        this.subDiagram = s;
    }

    @Override
    @PropertyName ( "Name" )
    public String getName()
    {
        return subDiagram == null ? null : subDiagram.getName();
    }
    public void setName(String name)
    {

    }

    @PropertyName ( "Title" )
    public String getTitle()
    {
        return subDiagram == null ? null : subDiagram.getTitle();
    }
    public void setTitle(String title)
    {
        if( subDiagram != null )
            subDiagram.setTitle( title );
    }

    @PropertyName ( "Diagram path" )
    public String getDiagramPath()
    {
        return subDiagram == null ? null : subDiagram.getDiagramPath();
    }
    public void setDiagramPath(String type)
    {

    }

    @PropertyName ( "State" )
    public String getState()
    {
        return subDiagram == null || subDiagram.getState() == null ? null : subDiagram.getState().getName();
    }
    public void setState(String type)
    {

    }


    @PropertyName ( "Comment" )
    public String getComment()
    {
        return subDiagram == null ? null : subDiagram.getComment();
    }
    public void setComment(String comment)
    {
        if( subDiagram != null )
            subDiagram.setComment( comment );
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public SubDiagram getSubDiagram()
    {
        return subDiagram;
    }
}