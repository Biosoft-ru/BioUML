package biouml.plugins.physicell;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.model.Diagram;
import biouml.model.util.DiagramXmlWriter;
import biouml.model.util.ModelXmlWriter;
import biouml.plugins.physicell.plot.PlotProperties;
import ru.biosoft.util.DPSUtils;

public class PhysicellDiagramWriter extends DiagramXmlWriter
{
    @Override
    protected ModelXmlWriter getModelWriter()
    {
        return new PhysicellModelWriter();
    }
    
    @Override
    public Element writePlotsInfo(Document doc, String elementName, Diagram diagram, Map<String, String> newPaths)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty("Plots");
        if( dp == null || !(dp.getValue() instanceof PlotProperties) )
            return null;
        Object value = dp.getValue();
        
        Element element = doc.createElement(elementName);
        element.setAttribute("type", value.getClass().getName());
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        DPSUtils.writeBeanToDPS(value, dps, "");
        if( dps.isEmpty() )
            return null;

        serializeDPS(doc, element, dps, null, false);
        return element;
    }
}
