package biouml.model.dynamics.plot;

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.EModel;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.Pen;
import ru.biosoft.table.TableDataCollection;

public class Experiment extends Option implements DataElement
{
    private String nameY;
    private String title;
    private String nameX;
    private DataElementPath path;
    private List<String> columnNames = new ArrayList<>();
    private Pen pen = null;
    
    public Experiment()
    {
        
    }
    public Experiment(DataElementPath path, String nameX, String nameY, String title, Pen pen)
    {
        setPath( path );
        setNameX( nameX );
        setNameY( nameY );
        setTitle( title );
        setPen( pen );
    }
    
    public DataElementPath getPath()
    {
        return path;
    }
    
    public void setPath(DataElementPath path)
    {
        this.path = path;
        TableDataCollection table = path.getDataElement( TableDataCollection.class );
        this.columnNames = table.columns().map( c->c.getName() ).toList();
        if( !columnNames.isEmpty() )
        {
            //try to keep columns
            if( !table.getColumnModel().hasColumn( getNameX() ) )
                setNameX( columnNames.get( 0 ) );

            if( !table.getColumnModel().hasColumn( getNameY() ) )
                setNameY( columnNames.get( ( columnNames.size() > 1 ) ? 1 : 0 ) );
        }
    }

    @PropertyName ( "Name Y" )
    public String getNameY()
    {
        return nameY;
    }
    public void setNameY(String nameY)
    {
        String oldValue = this.nameY;
        this.nameY = nameY;
        this.title = nameY;
        firePropertyChange("nameY", oldValue, nameY);
    }
    
    @PropertyName ( "Name X" )
    public String getNameX()
    {
        return nameX;
    }
    public void setNameX(String nameX)
    {
        String oldValue = this.nameX;
        this.nameX = nameX;
        firePropertyChange("nameX", oldValue, nameX);
    }

    public StreamEx<String> columns()
    {
        return StreamEx.of( columnNames.stream() );
    }

    @PropertyName ( "Title" )
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        String oldValue = this.title;
        this.title = title;
        firePropertyChange("title", oldValue, title);
    }
    
    @PropertyName ( "Line spec" )
    public Pen getPen()
    {
        return pen;
    }
    public void setPen(Pen pen)
    {
        this.pen = pen;
    }

    public Experiment clone(EModel emodel)
    {
        return new Experiment(path, nameX, nameY, title, pen);
    }
    @Override
    public String getName()
    {
        //TODO: getName() is required to display Experiment in table for web
        //create more correct name or use wrapper for web only
        return path + "_" + nameX + "_" + nameY;
    }
    @Override
    public DataCollection<?> getOrigin()
    {
        return null;
    }
}