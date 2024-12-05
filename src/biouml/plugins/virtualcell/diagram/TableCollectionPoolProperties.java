package biouml.plugins.virtualcell.diagram;

import java.awt.Dimension;
import java.awt.Point;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.Role;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TableCollectionPoolProperties extends Option implements InitialElementProperties, DataOwner
{
    private String name;
    private DataElementPath path;
    private TableDataCollection tdc;
    private boolean shouldBeSaved = false;
    private double saveStep = 0;
  

    private Node node;

    public TableCollectionPoolProperties(String name)
    {
        this.name = name;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( compartment, new Stub( compartment, name, "Pool" ) );
        this.node = result;
        result.setRole( this );
        result.setLocation( location );
        result.setShapeSize( new Dimension( 100, 50 ) );
        compartment.put( result );
        if( viewPane != null )
            viewPane.completeTransaction();
        return new DiagramElementGroup( result );
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        Object oldValue = this.name;
        this.name = name;
        this.firePropertyChange( "Name", oldValue, path );
    }

    @PropertyName ( "Initial values" )
    public DataElementPath getPath()
    {
        return path;
    }

    public void setPath(DataElementPath path)
    {
        Object oldValue = this.path;
        this.path = path;
        if( path != null )
            this.tdc = path.getDataElement( TableDataCollection.class );
        this.firePropertyChange( "Path", oldValue, path );
        this.firePropertyChange( "*", null, null );
    }
    
    @PropertyName("Should be saved")
    public boolean isShouldBeSaved()
    {
        return shouldBeSaved;
    }

    public void setShouldBeSaved(boolean shouldBeSaved)
    {
        this.shouldBeSaved = shouldBeSaved;
    }

    @PropertyName("Save step")
    public double getSaveStep()
    {
        return saveStep;
    }

    public void setSaveStep(double saveStep)
    {
        this.saveStep = saveStep;
    }
    
    @Override
    public DiagramElement getDiagramElement()
    {
        return node;
    }

    public void setDiagramElement(Node node)
    {
        this.node = node;
    }

    @Override
    public Role clone(DiagramElement de)
    {
        // TODO Auto-generated method stub
        TableCollectionPoolProperties result = new TableCollectionPoolProperties( name );
        result.setDiagramElement( (Node)de );
        result.setSaveStep( saveStep );
        result.setShouldBeSaved( shouldBeSaved );
        result.setPath( path );
        return result;
    }

    @Override
    public String[] getNames()
    {
        return TableDataCollectionUtils.getColumnNames( tdc );
    }

}