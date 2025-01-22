package biouml.plugins.virtualcell.diagram;

import java.awt.Dimension;
import java.awt.Point;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.Role;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;

public class TranscriptionProperties implements InitialElementProperties, DataOwner, DataElement
{
    private String name;
    private Node node;
    private DataElementPath transcriptionFactors;
    private String knockedTFS;
    private String line = getLines()[0];
    private String model = getModels()[0];

    public static String[] getLines()
    {
        return new String[] {"K562"};
    }

    public static String[] getModels()
    {
        return new String[] {"FC", "FC_wocn","CB", "CNN"};
    }

    public TranscriptionProperties(String name)
    {
        this.name = name;
    }

    @Override
    public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( compartment, new Stub( compartment, name, "Transcription" ) );
        this.node = result;
        result.setRole( this );
        result.setLocation( location );
        result.setShapeSize( new Dimension( 100, 50 ) );
        compartment.put( result );
        if( viewPane != null )
            viewPane.completeTransaction();
        return new DiagramElementGroup( result );
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
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
        TranscriptionProperties result = new TranscriptionProperties( name );
        result.setTranscriptionFactors( transcriptionFactors );
        result.setDiagramElement( (Node)de );
        result.setModel( model );
        result.setLine( line );
        result.setKnockedTFS( knockedTFS );
        return result;
    }


    @Override
    public String[] getNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return null;
    }

    @PropertyName ( "Transcription factors" )
    public DataElementPath getTranscriptionFactors()
    {
        return transcriptionFactors;
    }

    public void setTranscriptionFactors(DataElementPath transcriptionFactors)
    {
        this.transcriptionFactors = transcriptionFactors;
    }

    @PropertyName ( "Cell line" )
    public String getLine()
    {
        return line;
    }

    public void setLine(String line)
    {
        this.line = line;
    }

    @PropertyName ( "Model" )
    public String getModel()
    {
        return model;
    }
    
    public String getKnockedTFS()
    {
        return knockedTFS;
    }

    @PropertyName("Knocked out Transcription Factors")
    public void setKnockedTFS(String knockedTFS)
    {
        this.knockedTFS = knockedTFS;
    }


    public void setModel(String model)
    {
        this.model = model;
    }
}