package biouml.standard.diagram;

import java.awt.Point;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.editor.ViewEditorPane;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;

@SuppressWarnings ( "serial" )
@PropertyName ( "Initial properties" )
@PropertyDescription ( "Initial properties." )
public class StubInitialProperties extends InitialElementPropertiesSupport
{    
    private String name;
    private String title;
    private String type;
    private Class<? extends Node> clazz;
    
    @Override
    public DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        Stub kernel = new Stub( null, name, type );
        name = DefaultSemanticController.generateUniqueNodeName( c, name );
        Node node = clazz.getConstructor( ru.biosoft.access.core.DataCollection.class, Base.class ).newInstance( c, kernel );
        return new DiagramElementGroup( node );
    }

    public StubInitialProperties(Compartment c, String type, Class<? extends Node> clazz)
    {
        this.type = type;
        this.name = DefaultSemanticController.generateUniqueNodeName( c, type );
        this.title = name;
        this.clazz = clazz;
    }


    @PropertyName ( "Title" )
    @PropertyDescription ( "Element title." )
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }

    @PropertyName ( "Name" )
    @PropertyDescription ( "Element name." )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
        this.title = name;
    }
}