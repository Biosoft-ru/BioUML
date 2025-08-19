package biouml.workbench.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.ScalableElementExporter;
import biouml.model.util.DiagramImageGenerator;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.ImageElement;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * Exports diagram image in formats supported by {@link javax.imageio.ImageIO}.
 */
public class ImageExporter extends ScalableElementExporter
{
    private static final String ANTI_ALIASING_PROPERTY = "anti-aliasing";
    private static final PropertyDescriptor ANTI_ALIASING_PD = StaticDescriptor.create(ANTI_ALIASING_PROPERTY, "Smooth image");

    private static Logger log = Logger.getLogger(ImageExporter.class.getName());

    protected ImageWriter writer;
    protected String format;
    protected String suffix;

    protected void initWriter(Iterator<ImageWriter> i)
    {
        if( i == null )
            return;

        while( i.hasNext() )
        {
            if( writer == null )
            {
                writer = i.next();
                log.info("ImageWriter " + writer + " is used for format=" + format + ", suffix=" + suffix);
            }
            else
            {
                log.warning("Alternative ImageWriter " + writer + " for format=" + format + ", suffix=" + suffix);
            }
        }
    }

    /** Accepts any diagram. */
    @Override
    public int accept(DataElement de)
    {
        if(de instanceof Diagram || de instanceof ImageElement) return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        BufferedImage image = getImage(de);

        if(!format.equals("PNG"))
        {
            BufferedImage convertedImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            convertedImg.getGraphics().setColor(Color.WHITE);
            convertedImg.getGraphics().fillRect(0, 0, image.getWidth(), image.getHeight());
            convertedImg.getGraphics().drawImage(image, 0, 0, null);
            image = convertedImg;
        }

        file.delete();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream( file ))
        {
            writer.setOutput( stream );
            writer.write( image );
        }
        writer.dispose();
    }

    protected Boolean isAntialiased()
    {
        return (Boolean) ( properties.getProperty(ANTI_ALIASING_PROPERTY) == null
                || properties.getProperty(ANTI_ALIASING_PROPERTY).getValue() == null ? false : properties.getProperty(ANTI_ALIASING_PROPERTY).getValue() );
    }

    private BufferedImage getImage(DataElement de)
    {
        if(de instanceof Diagram)
        {
            return DiagramImageGenerator.generateDiagramImage( (Diagram) de, getScale(), isAntialiased() );
        } else if(de instanceof ImageElement)
        {
            Dimension size = ((ImageElement)de).getImageSize();
            size.width = (int)(size.width*getScale());
            size.height = (int)(size.height*getScale());
            return ((ImageElement)de).getImage(size);
        }
        return null;
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
        this.format = properties.getProperty(DataElementExporterRegistry.FORMAT);
        this.suffix = properties.getProperty(DataElementExporterRegistry.SUFFIX);

        // check whether this format or suffix is supported by ImageIO
        initWriter( ImageIO.getImageWritersByFormatName(format) );
        if( writer == null && suffix != null )
            initWriter( ImageIO.getImageWritersBySuffix(suffix) );

        if( writer == null )
        {
            log.log(Level.SEVERE, "Could not find ImageWriter for format=" + format + ", suffix=" + suffix);
            return false;
        }

        return true;
    }

    @Override
    public Object getProperties(DataElement de, File file)
    {
        if(de instanceof Diagram)
        {
            properties.add(new DynamicProperty(ANTI_ALIASING_PD, Boolean.class, true));
        }
        return super.getProperties(de, file);
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( ImageElement.class, Diagram.class );
    }
}
