package biouml.plugins.wdl._test;

import java.io.File;
import java.net.URL;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.nextflow.NextFlowImporter;
import biouml.plugins.wdl.FileScriptLoader;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.standard.type.DiagramInfo;

public class TestNextflowImporter
{

    public static void main(String ... args) throws Exception
    {
        String name = "two_steps";
        
        URL rootURL = TestWDL.class.getResource( "../test_examples/nextflow/" );
        
        URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + name + ".nf" );
        if( url == null )
            throw new IllegalArgumentException( "No input file exists: " + name );

        File file = new File( url.getFile() );
        String nextflow = ApplicationUtils.readAsString( file );
        Diagram d = new WDLDiagramType().createDiagram( null, "test", new DiagramInfo( null, "test" ) );
        
        NextFlowImporter importer = new NextFlowImporter();
        importer.setScriptLoader( new FileScriptLoader( new File(rootURL.getFile()) ) );
        importer.importNextflow( nextflow, d );
        
        System.out.println( nextflow );
//        TestWDL.exportImage(d, new File("C:/Users/Damag/dot.png"));
    }

}
