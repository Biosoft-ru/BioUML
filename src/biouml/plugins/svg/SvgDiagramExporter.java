package biouml.plugins.svg;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Document;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.graphics.View;
import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.model.DiagramViewBuilder;
import biouml.model.ScalableElementExporter;

/**
 * Exports diagram image in SVG format.
 *
 * @pending move view to offset
 *
 */
public class SvgDiagramExporter extends ScalableElementExporter
{
    /** Accepts any diagram. */
    @Override
    public int accept(DataElement de)
    {
        if(de instanceof Diagram) return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( Diagram.class );
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        Diagram diagram = (Diagram)de;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        
        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
        AffineTransform at = new AffineTransform();
        at.scale(getScale(), getScale());
        svgGenerator.setTransform(at);
        View view = viewBuilder.createDiagramView(diagram, svgGenerator);
        DiagramFilter filter = diagram.getFilter();
        if( filter != null && filter.isEnabled() )
            filter.apply(diagram);
        view.move(10, 10);
        view.paint(svgGenerator);
        
        // Finally, stream out SVG to the standard output using UTF-8
        // character to byte encoding
        OutputStreamWriter out = new OutputStreamWriter(
                new FileOutputStream(file), SVGGraphics2D.DEFAULT_XML_ENCODING);
        
        boolean useCSS = true; // we want to use CSS style attribute ?
        svgGenerator.stream(out, useCSS);
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if(jobControl != null)
        {
            jobControl.functionStarted();
        }
        doExport(de, file);
        if( jobControl != null && jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST
                && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }
}


