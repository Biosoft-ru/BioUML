package biouml.plugins.wdl;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.wdl.diagram.WDLDiagramTransformer;
//import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.graphics.View;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.util.ImageGenerator;

@ClassIcon ( "resources/wdl-script.gif" )
@PropertyName ( "WDL-script" )
@PropertyDescription ( "Workflow Description Language script" )
public class WDLScript extends ScriptDataElement implements ImageElement
{
    private File file;

    public WDLScript(DataCollection<?> origin, String name, String content)
    {
        super( name, origin, content );
    }

    public WDLScript(DataCollection<?> origin, String name, File file) throws IOException
    {
        this( origin, name, ApplicationUtils.readAsString( file ) );
        setFile( file );
    }


    public File getFile()
    {
        return file;
    }
    public void setFile(File file)
    {
        this.file = file;
    }

    @Override
    protected ScriptJobControl createJobControl(String content, ScriptEnvironment env, Map<String, Object> scope,
            Map<String, Object> outVars, boolean sessionContext)
    {
        return null;
        //        return new WDLScriptJobControl(content, env) ;
    }

    @Override
    public BufferedImage getImage(Dimension dimension)
    {
        try
        {
            WDLDiagramTransformer transformer = new WDLDiagramTransformer();
            FileDataElement fde = new FileDataElement( getName(), null, getFile() );
            Diagram diagram = transformer.transformInput( fde );
            View view = WebDiagramsProvider.createView( diagram );
            return ImageGenerator.generateImage( view, 1, true );
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Dimension getImageSize()
    {
        BufferedImage image = getImage( null );
        return new Dimension( image.getWidth(), image.getHeight() );
    }

}
