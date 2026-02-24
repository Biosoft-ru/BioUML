package biouml.model.dynamics.plot;

import java.util.Objects;
import java.util.stream.Stream;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class PlotVariable extends Option implements DataElement
{
    static final String TIME_VARIABLE = "time";
    private String path;
    private String name;
    private String title;
    private EModel emodel;
    private VariableDescriptor descriptor = new VariableDescriptor();

    public PlotVariable(String path, String name, String title, EModel emodel)
    {
        this.setPath( path );
        this.setName( name );
        this.setTitle( title );
        this.setEModel( emodel );
    }

    public PlotVariable()
    {
        this( "", TIME_VARIABLE, TIME_VARIABLE, null );
    }

    public EModel getEModel()
    {
        return emodel;
    }
    public void setEModel(EModel emodel)
    {
        this.emodel = emodel;

        if( !variables().toSet().contains( name ) ) //this variable is not valid in this context
            this.setName( TIME_VARIABLE );
    }

    public String getCompleteName()
    {
        return getPath().isEmpty() ? getName() : getPath() + "/" + getName();
    }

    @PropertyName ( "Path" )
    public String getPath()
    {
        return path;
    }
    public void setPath(String path)
    {
        String oldValue = this.path;
        this.path = path;
        firePropertyChange( "path", oldValue, path );
        if( !Objects.equals( oldValue, path ) )
        {
            setName( PlotVariable.TIME_VARIABLE );
            setTitle( PlotVariable.TIME_VARIABLE );
            firePropertyChange( "*", "", "" );
        }
    }

    @PropertyName ( "Value" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        String oldValue = this.name;
        this.name = name;
        setTitle( descriptor.getTitle( name, emodel ) );
        firePropertyChange( "name", oldValue, name );
    }

    public String getVariableName()
    {
       return descriptor.getVariableName( name, emodel );
    }
    
    public Variable getVariable(EModel emodel)
    {
        String variableName = getVariableName();
        if (variableName == null)
            return null;
        return emodel.getVariable( variableName );
    }

    public void setVariableName(String name)
    {
        Variable var = emodel.getVariable( name );
        String description = descriptor.getDescription( var, emodel );
        setName( description );
    }

    public StreamEx<String> variables()
    {
        if( emodel == null )
            return StreamEx.empty();
        Diagram diagram = Util.getInnerDiagram( emodel.getParent(), path );
        if( ! ( diagram.getRole() instanceof EModel ) )
            return StreamEx.empty();
        EModel emodel = diagram.getRole( EModel.class );
        return descriptor.getDescriptions( emodel );
    }

    public Stream<String> modules()
    {
        Diagram diagram = emodel.getParent();
        if( !DiagramUtility.isComposite( diagram ) )
            return StreamEx.of( new String[] {""} );

        return StreamEx.of( Util.getSubDiagrams( diagram ) ).map( s -> Util.getPath( s ) ).append( "" );
    }

    public boolean isPathReadOnly()
    {
        return !DiagramUtility.isComposite( getEModel().getParent() );
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
        firePropertyChange( "title", oldValue, title );
    }

    public PlotVariable clone(EModel emodel)
    {
        return new PlotVariable( path, name, title, emodel );
    }

    public boolean isPathHidden()
    {
        Diagram diagram = emodel.getParent();
        return !DiagramUtility.isComposite( diagram );
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return null;
    }
}