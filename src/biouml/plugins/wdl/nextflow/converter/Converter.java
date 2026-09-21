package biouml.plugins.wdl.nextflow.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.util.DiagramImageGenerator;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.graph.Graph;
import biouml.plugins.wdl.FileScriptLoader;
import biouml.plugins.wdl.ScriptLoader;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.model.ScriptInfo;

public class Converter
{

    public static void main(String ... args)
    {

//    	args = new String[] {"C:/Users/Damag/nextflowtest/ngs-pipelines-main/ngs-pipelines-main/common/wdl/tasks/vep.wdl"};
//    	args = new String[] {"C:/Users/Damag/nextflowtest/ngs-pipelines-main/ngs-pipelines-main/pipelines/brca/wdl/workflows/basic_directory.wdl", "-i"};
//    	args = new String[] {"C:/Users/Damag/nextflowtest/modelseed_from_annotation.wdl", "-i"};
//    	args = new String[] {"C:/Users/Damag/nextflowtest/1.5_PROKKA"};
    	args = new String[] {"C:/Users/Damag/nextflowtest/ngs-pipelines-main/ngs-pipelines-main/pipelines/brca/wdl/workflows/brca.wdl", "-i"};
//    	args = new String[] {"C:/Users/Damag/nextflowtest/ngs-pipelines-main/ngs-pipelines-main/common/wdl/tasks/multiqc.wdl"};
//    	args = new String[] {"C:/Users/Damag/nextflowtest/ngs-pipelines-main/ngs-pipelines-main/common/wdl/tasks/samtools_.wdl"};
//    	args = new String[] {"C:/Users/Damag/nextflowtest/ss.wdl"};
    	try
        {
            ConverterParameters parameters = new ConverterParameters(args);

            if( parameters.showHelp )
                log("HELP WILL BE ADDED LATER");

            String filePath = parameters.filePath;

            Path path = Paths.get(filePath);
            String absolutePath;
            String parent;
            if( path.isAbsolute() )
            {
                absolutePath = path.toString();
                parent = path.getParent().toString();
            }
            else
            {
                String jarPath = Converter.class.getProtectionDomain().getCodeSource().getLocation().getFile();
                parent = new File(jarPath).getParent();
                absolutePath = parent + "/" + filePath;
            }
            
            File inputFile = new File(absolutePath);
             
			String name = inputFile.getName();
			name = name.substring(0, name.lastIndexOf("."));
					
			Map<String, Diagram> diagrams = loadDiagrams(absolutePath);

			if (diagrams.size() == 1) 
			{
				Diagram diagram = diagrams.values().iterator().next();
				generateResults( diagram,  inputFile.getParentFile(),  name,   parameters);
                NextFlowRunner.generateFunctions( inputFile.getParentFile().getCanonicalPath() );
			}
			else
			{
				File resultFolder = new File(inputFile.getParentFile(), name);
				resultFolder.mkdir();
				for (Entry<String, Diagram> entry: diagrams.entrySet())
				{
					String entryName = entry.getKey();
					entryName = entryName.endsWith(".wdl") ? entryName.substring(0, entryName.length() - 4) : entryName;
					Diagram entryDiagram = entry.getValue();
					generateResults( entryDiagram,  resultFolder,  entryName,   parameters);
					
					if (entryDiagram.getName().contains( "brca" ))
                    {
                        Graph graph = DiagramToGraphTransformer.generateGraph( entryDiagram, null );
                        ru.biosoft.graph.Util.outGraph( parent+"/brca/brca.graph", graph );
                    }
				}
				 NextFlowRunner.generateFunctions( resultFolder.getCanonicalPath() );
				 generateConfig(resultFolder);
			}
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
    
    private static void generateResults(Diagram diagram, File parent, String name,  ConverterParameters parameters) throws Exception
    {
		System.out.println("Should generate image: " + parameters.showImage);
		if (parameters.showImage) 
		{
			File imageFile = new File(parent + "/" + name + ".png");
			exportImage(diagram, imageFile);
			log("Image generated: " + imageFile.getName());
		}

		String nextFlow = new NextFlowGenerator().generate(diagram);
		File nextFlowFile = new File(parent + "/" + name + ".nf");
		ApplicationUtils.writeString(nextFlowFile, nextFlow);
		log("Nextflow script generated: " + nextFlowFile.getName());
		log("All done!");
    }

    protected static Map<String, Diagram> loadDiagrams(String path) throws Exception
    {
        File f = new File(path);
        String name = f.getName();
        name = f.getName().endsWith(".wdl") ? name.substring(0, name.length() - 4) : name;
        WDLImporter importer = new WDLImporter();
        importer.setScriptLoader(new FileScriptLoader(ScriptLoader.WDL_TYPE, f.getParentFile()));
        String text = ApplicationUtils.readAsString(f);
        ScriptInfo scriptInfo = importer.readScript(name, text);
        DiagramGenerator generator = new DiagramGenerator();
        generator.generateDiagram(scriptInfo, null, name);
        return generator.getAllImports();
    }

    public static void exportImage(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        BufferedImage image = DiagramImageGenerator.generateDiagramImage(diagram, 1, true);

        ImageWriter writer = ImageIO.getImageWritersBySuffix("png").next();

        file.delete();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(file))
        {
            writer.setOutput(stream);
            writer.write(image);
        }
        writer.dispose();
    }

    private static void log(String s)
    {
        System.out.println(getCurrentTime() + s);
    }

    public static String getCurrentTime()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("[ HH:mm:ss ] ");
        return sdf.format(cal.getTime());
    }
    
    private static File generateConfig(File parent) throws Exception
    {
        File config = new File(parent, "nextflow.config");
        ApplicationUtils.writeString( config, "docker.enabled = true" );
        return config;
    }
}