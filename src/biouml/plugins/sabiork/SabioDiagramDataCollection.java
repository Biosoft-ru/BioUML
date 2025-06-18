package biouml.plugins.sabiork;

import java.io.FileOutputStream;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.sbgn.SBGNXmlReader;
import biouml.plugins.sbgn.SBGNXmlWriter;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.util.DPSUtils;

/**
 * Collection for SABIO-RK pathways
 */
public class SabioDiagramDataCollection extends SabioDataCollection<Diagram>
{
    public SabioDiagramDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        service = SabiorkUtility.getServiceProvider(Diagram.class.getName());
    }

    @Override
    protected Diagram doGet(String name) throws Exception
    {
        Diagram diagram = super.doGet(name);
        Diagram sbgnDiagram = null;
        DataCollection<FileDataElement> layouts = (DataCollection<FileDataElement>)getOrigin().get("Layouts");
        if( layouts.contains(name + ".xml") )
        {
            //Get layout from Layouts collection
            FileDataElement fde = layouts.get(name + ".xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(fde.getFile());
            Element root = document.getDocumentElement();

            SBGNXmlReader reader = new SBGNXmlReader(this, name, diagram);
            sbgnDiagram = reader.read(root);
        }
        else
        {
            //Create SBGN XML automatically
            SBGNConverter converter = new SBGNConverter();
            sbgnDiagram = converter.convert(diagram, XmlDiagramType.class);
        }
        DynamicProperty dp = new DynamicProperty(SBGNConverter.SBGN_BASE_DIAGRAM, Diagram.class, diagram);
        DPSUtils.makeTransient( dp ); //avoid serializing it with other diagram attributes
        sbgnDiagram.getAttributes().add(dp);
        return sbgnDiagram;
    }

    @Override
    protected void doPut(Diagram sbgnDiagram, boolean isNew) throws Exception
    {
        //Save layout
        Object baseDiagram = sbgnDiagram.getAttributes().getValue(SBGNConverter.SBGN_BASE_DIAGRAM);
        if( baseDiagram instanceof Diagram )
        {
            DataCollection<FileDataElement> layouts = (DataCollection<FileDataElement>)getOrigin().get("Layouts");
            if( layouts != null )
            {
                SBGNXmlWriter writer = new SBGNXmlWriter(sbgnDiagram, (Diagram)baseDiagram);
                FileDataElement fde = new FileDataElement(sbgnDiagram.getName() + ".xml", (FileBasedCollection)layouts);
                try(FileOutputStream fos = new FileOutputStream(fde.getFile()))
                {
                    writer.write(fos);
                }
                layouts.put(fde);
            }
        }
    }

    @Override
    public @Nonnull Class<Diagram> getDataElementType()
    {
        return Diagram.class;
    }
}
