package biouml.plugins.beakerx;

import com.twosigma.beakerx.BeakerXClient;
import com.twosigma.beakerx.TryResult;
import com.twosigma.beakerx.autocomplete.AutocompleteResult;
import com.twosigma.beakerx.autocomplete.MagicCommandAutocompletePatterns;
import com.twosigma.beakerx.evaluator.BaseEvaluator;
import com.twosigma.beakerx.evaluator.BxInspect;
import com.twosigma.beakerx.evaluator.ClasspathScanner;
import com.twosigma.beakerx.evaluator.JobDescriptor;
import com.twosigma.beakerx.evaluator.TempFolderFactory;
import com.twosigma.beakerx.evaluator.TempFolderFactoryImpl;
import com.twosigma.beakerx.inspect.Inspect;
import com.twosigma.beakerx.jvm.classloader.BeakerXUrlClassLoader;
import com.twosigma.beakerx.jvm.object.EvaluationObject;
import com.twosigma.beakerx.jvm.threads.BeakerCellExecutor;
import com.twosigma.beakerx.jvm.threads.CellExecutor;
import com.twosigma.beakerx.kernel.Classpath;
import com.twosigma.beakerx.kernel.EvaluatorParameters;
import com.twosigma.beakerx.kernel.ExecutionOptions;
import com.twosigma.beakerx.kernel.ImportPath;
import com.twosigma.beakerx.kernel.Imports;
import com.twosigma.beakerx.kernel.PathToJar;

import com.twosigma.beakerx.mimetype.MIMEContainer;
import com.twosigma.beakerx.MIMEContainerFactory;

import static com.twosigma.beakerx.evaluator.BaseEvaluator.INTERUPTED_MSG;

import java.lang.reflect.InvocationTargetException;

import java.util.concurrent.Executors;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;

import ru.biosoft.access.ImageElement;

import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.plugins.javascript.Global;
import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.plugins.javascript.JScriptVisiblePlugin;

import one.util.streamex.StreamEx;
		
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

public class BeakerXtoBioUMLEvaluator extends BaseEvaluator 
{
    private BeakerXUrlClassLoader beakerxUrlClassLoader;

    public BeakerXtoBioUMLEvaluator(
                           String id,
                           String sId,
                           EvaluatorParameters evaluatorParameters,
                           BeakerXClient beakerxClient,
                           MagicCommandAutocompletePatterns autocompletePatterns,
                           ClasspathScanner classpathScanner,
                           Inspect inspect) {
      this(id,
              sId,
              new BeakerCellExecutor("javascript"),
              new TempFolderFactoryImpl(),
              evaluatorParameters,
              beakerxClient,
              autocompletePatterns,
              classpathScanner,
              inspect);
    }

    public BeakerXtoBioUMLEvaluator(
                           String id,
                           String sId,
                           CellExecutor cellExecutor,
                           TempFolderFactory tempFolderFactory,
                           EvaluatorParameters evaluatorParameters,
                           BeakerXClient beakerxClient,
                           MagicCommandAutocompletePatterns autocompletePatterns,
                           ClasspathScanner classpathScanner,
                           Inspect inspect) {
        super(id,
                sId,
                cellExecutor,
                tempFolderFactory,
                evaluatorParameters,
                beakerxClient,
                autocompletePatterns,
                classpathScanner,
                inspect);
        reloadClassloader();
    }

    @Override
    public TryResult evaluate( EvaluationObject seo, String script, ExecutionOptions executionOptions )
    {
        TryResult either;
        try 
        {
            seo.setOutputHandler();
            StringBuilder out = new StringBuilder();
            List<MIMEContainer> list = new ArrayList<>(); 
            JupyterScriptEnviroment jse = new JupyterScriptEnviroment( out, list, seo );

            Context ctx = JScriptContext.getContext();
            ScriptableObject scope = JScriptContext.getScope();
            scope.put( "InternalVariable_EvaluationObject", scope, seo );

            seo.started();
            
            String execScript = 
               "importPackage(com.twosigma.beakerx.chart);" +
               "importPackage(com.twosigma.beakerx.chart.legend);" +
               "importPackage(com.twosigma.beakerx.chart.xychart);" +
               "importPackage(com.twosigma.beakerx.chart.xychart.plotitem);" +
               "importClass(com.twosigma.beakerx.chart.categoryplot.CategoryPlot);" +
               "importPackage(com.twosigma.beakerx.chart.categoryplot.plotitem);" +
               "importClass(com.twosigma.beakerx.chart.heatmap.HeatMap);" +
               "importClass(com.twosigma.beakerx.chart.histogram.Histogram);" +
               "importClass(com.twosigma.beakerx.chart.treemap.TreeMap);" +
               "importPackage(com.twosigma.beakerx.easyform);" +
               "importPackage(com.twosigma.beakerx.table);" +
               "importClass(com.twosigma.beakerx.fileloader.CSV);" +
               "importPackage(com.twosigma.beakerx.widget);" +
               "importClass(com.twosigma.beakerx.jvm.object.OutputCell);" +
               "function display(v){com.twosigma.beakerx.Display.display(v)};" +
               "function HTML(v){return com.twosigma.beakerx.mimetype.MIMEContainer.HTML(v)};" +
               "function SVG(v){return com.twosigma.beakerx.mimetype.SVGContainer.SVG(v)};" +
               "function Image(v){return com.twosigma.beakerx.mimetype.ImageContainer.Image(v)};" +
               "function FileLink(v){return com.twosigma.beakerx.mimetype.FileLinkContainer.FileLink(v)};" +
               "function FileLinks(v){return com.twosigma.beakerx.mimetype.FileLinkContainer.FileLinks(v)};" +
               "function JavaScript(v){return com.twosigma.beakerx.mimetype.MIMEContainer.JavaScript(v)};" +
               "function Javascript(v){return com.twosigma.beakerx.mimetype.MIMEContainer.Javascript(v)};" +
               "function Latex(v){return com.twosigma.beakerx.mimetype.MIMEContainer.Latex(v)};" +
               "function Math(v){return com.twosigma.beakerx.mimetype.MIMEContainer.Math(v)};" +
               "function Markdown(v){return com.twosigma.beakerx.mimetype.MIMEContainer.Markdown(v)};" +
               "function Video(v){return com.twosigma.beakerx.mimetype.MIMEContainer.Video(v)};" +
               "function YoutubeVideo(v,p1,p2){return com.twosigma.beakerx.mimetype.MIMEContainer.YoutubeVideo(v,p1,p2)};" +
               "com.twosigma.beakerx.evaluator.InternalVariable.setValue(InternalVariable_EvaluationObject);" 
                + script; 
   
            Object result = JScriptContext.evaluateString( execScript, jse );            
            if( jse.isEmpty && result != null )
            {
                either = TryResult.createResult( result );
            }
            else
            {
                either = TryResult.createResult( list.get( 0 ) );
            }
            return super.processResult( either );
        }
        catch( Throwable e )
        {
            if( e instanceof InvocationTargetException ) 
            {
                e = ( ( InvocationTargetException )e ).getTargetException();
            }

            if( e instanceof InterruptedException || e instanceof InvocationTargetException || e instanceof ThreadDeath ) 
            {
                either = TryResult.createError( INTERUPTED_MSG );
            } 
            else 
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace( pw );
                String value = "<pre><font color=\"red\">" + sw + "</font></pre>";
                either = TryResult.createError( value );
            }            
        }
        finally 
        {
           seo.clrOutputHandler();
        }
        return either;   
    }

    @Override
    public AutocompleteResult autocomplete(String code, int caretPosition)
    {
        return null;   
    }


    @Override
    public ClassLoader getClassLoader() 
    {
        return this.getClass().getClassLoader();
    }

    @Override
    protected void doResetEnvironment() 
    {
    }

    @Override
    protected void addJarToClassLoader( PathToJar pathToJar )
    {
       this.beakerxUrlClassLoader.addJar( pathToJar );
    }

    @Override
    protected void addImportToClassLoader(ImportPath anImport) 
    {
    }

    @Override
    public void exit()
    {
        super.exit();
        killAllThreads();
    }

    private void reloadClassloader() 
    {
        //this.beakerxUrlClassLoader = newParentClassLoader(getClasspath());
    }

    public static class JupyterScriptEnviroment implements ScriptEnvironment
    {
        private StringBuilder out;   
        private List<MIMEContainer> list;
        private EvaluationObject seo;

        boolean isEmpty = true;

        public JupyterScriptEnviroment( StringBuilder out, List<MIMEContainer> list, EvaluationObject seo )
        {
            this.out = out;
            this.list = list;
            this.seo = seo;
        }
        
        @Override
        public void error( String msg )
        {
            out.append( "<b><font color=\"red\">" + msg + "</font></b>" );
            list.add( MIMEContainerFactory.createMIMEContainers( MIMEContainer.HTML( "<b><font color=\"red\">" + msg + "</font></b>" ) ).get( 0 ) );
            isEmpty = false;
        }

        @Override
        public void print( String msg )
        {
            out.append( msg );
            list.add( MIMEContainerFactory.createMIMEContainers( MIMEContainer.Text( msg ) ).get( 0 ) );
            isEmpty = false;
        }

        @Override
        public void info( String msg )
        {
            com.twosigma.beakerx.Display.display( msg );
        }

        public String tryShowObject( Object result )
        {
            if( result instanceof com.twosigma.beakerx.widget.DisplayableWidget ||
                result instanceof com.twosigma.beakerx.table.TableDisplay ||
                result instanceof com.twosigma.beakerx.chart.xychart.plotitem.XYGraphics ||
                result instanceof MIMEContainer )
            {
                list.add( MIMEContainerFactory.createMIMEContainers( result ).get( 0 ) );
                isEmpty = false;
                return ""; 
            }

            return null;
        }

        @Override
        public void showGraphics( BufferedImage image )
        {
            try
            {
                java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write( image, "png", java.util.Base64.getEncoder().wrap( os ) );
                out.append( "<img src=\"data:image/png;base64," + os.toString("UTF-8") + "\" />" );
                list.add( MIMEContainerFactory.createMIMEContainers( MIMEContainer.HTML( "<img src=\"data:image/png;base64," + os.toString("UTF-8") + "\" />" ) ).get( 0 ) );
            }
            catch( IOException ioe )
            {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter( sw );

                ioe.printStackTrace( pw );
                out.append( "<pre><font color=\"red\">" + sw + "</font></pre>" );
                list.add( MIMEContainerFactory.createMIMEContainers( MIMEContainer.HTML( "<b><font color=\"red\">" + sw + "</font></b>" ) ).get( 0 ) );
            }
            isEmpty = false;
        }

        @Override
        public void showGraphics(ImageElement element) 
        {
            showGraphics( element.getImage( null ) );
            isEmpty = false;
        }
        
        @Override
        public void showHtml( String html )
        {
            out.append( html );
            list.add( MIMEContainerFactory.createMIMEContainers( MIMEContainer.HTML( html ) ).get( 0 ) );
            isEmpty = false;
        }

        @Override
        public void showTable( TableDataCollection dataCollection )
        {
            java.io.StringWriter sw = new java.io.StringWriter();
            sw.write( "<table>\n<th><td>" );
            sw.write( dataCollection.columns().map( TableColumn::getName ).joining( "</td><td>" ) );
            sw.write( "</td></th>\n" );

            for( RowDataElement id : dataCollection )
            {
                sw.write( "<tr><td>" );
                sw.write( StreamEx.of( id.getValues() ).prepend( id ).joining( "</td><td>" ) );
                sw.write( "</td></tr>\n" );
            }
            out.append( sw.toString() );
            list.add( MIMEContainerFactory.createMIMEContainers( MIMEContainer.HTML( sw.toString() ) ).get( 0 ) );
            isEmpty = false;
        }

        @Override
        public void warn(String msg)
        {
            out.append( "<b><font color=\"orange\">" + msg + "</font></b>" );
            list.add( MIMEContainerFactory.createMIMEContainers( MIMEContainer.HTML( "<b><font color=\"orange\">" + msg + "</font></b>" ) ).get( 0 ) );
            isEmpty = false;
        }

        @Override
        public boolean isStopped()
        {
            return false;
        }

        @Override
        public String addImage(BufferedImage image)
        {
            try
            {
                java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write( image, "png", java.util.Base64.getEncoder().wrap( os ) );
                return "data:image/png;base64,"+os.toString("UTF-8");                
            }
            catch( IOException ioe )
            {
            }
            return null;
        }
    }
}
