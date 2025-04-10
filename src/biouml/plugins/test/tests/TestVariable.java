package biouml.plugins.test.tests;

import java.util.List;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.util.TextUtil2;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.util.EModelHelper;
import biouml.plugins.test.TestDocument;
import biouml.plugins.test.TestModel;
import biouml.standard.diagram.Util;

import com.developmentontheedge.beans.Option;

public class TestVariable extends Option
{
    private String name;
    private String subDiagramName;

    public TestVariable(Option parent)
    {
        setParent( parent );
    }

    public TestVariable() throws Exception
    {
        Diagram diagram = getCurrentDiagram();
        if( diagram == null )
        {
            throw new Exception( "Can not create variable for test: no diagram specified" );
        }

        if( diagram.getRole() instanceof EModel )
        {
            subDiagramName = diagram.getName();
            name = EModelHelper.getParameters( diagram.getRole( EModel.class ) )[0];
        }
        else
        {
            List<SubDiagram> subDiagrams = Util.getSubDiagrams( diagram );

            for( SubDiagram subDiagram : subDiagrams )
            {
                Role role = subDiagram.getDiagram().getRole();
                if(role instanceof EModel )
                {
                    subDiagramName = subDiagram.getName();
                    name = EModelHelper.getParameters( (EModel)role )[0];
                    break;
                }
            }
        }
        if( subDiagramName == null || name == null )
        {
            throw new Exception( "Can not create variable for test" );
        }
    }

    public TestVariable(String text)
    {
        String[] names = TextUtil2.split( text, ',' );
        this.subDiagramName = names[0];
        this.name = names[1];
    }

    public void setContent(String text)
    {
        String[] names = TextUtil2.split( text, ',' );
        this.subDiagramName = names[0];
        this.name = names[1];
    }

    @Override
    public String toString()
    {
        return getSubDiagramName() + "," + getName();
    }

    public String getName()
    {
        return name;
    }

    public String geNewtName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSubDiagramName()
    {
        return subDiagramName;
    }

    public void setSubDiagramName(String subDiagramName)
    {
        this.subDiagramName = subDiagramName;
        this.name = EModelHelper.getParameters( getSubDiagram().getRole( EModel.class ) )[0];
    }

    public Diagram getSubDiagram()
    {
        try
        {
            Diagram diagram = getCurrentDiagram();
            if( subDiagramName.equals( diagram.getName() ) )
            {
                return diagram;
            }
            Node node = diagram.findNode( subDiagramName );
            if( node instanceof SubDiagram )
            {
                return ( (SubDiagram)node ).getDiagram();
            }

            return null;
        }
        catch( Exception ex )
        {
            return null;
        }
    }

    public Diagram getCurrentDiagram()
    {
        Document document = Document.getActiveDocument();
        if( document instanceof TestDocument )
        {
            Object model = ( (TestDocument)document ).getModel();
            DataElement de = ( (TestModel)model ).getModelPath().optDataElement();
            return (Diagram)de;
        }
        return null;
    }
}
