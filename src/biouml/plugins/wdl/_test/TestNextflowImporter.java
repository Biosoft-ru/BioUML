package biouml.plugins.wdl._test;

import java.io.File;
import java.net.URL;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.NextFlowImporter;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.standard.type.DiagramInfo;

public class TestNextflowImporter
{

    public static void main(String ... args) throws Exception
    {
        String name = "two_steps";
        
        URL url = TestWDL.class.getResource( "../test_examples/nextflow/" + name + ".nf" );
        if( url == null )
            throw new IllegalArgumentException( "No input file exists: " + name );

        File file = new File( url.getFile() );
        String nextflow = ApplicationUtils.readAsString( file );
        Diagram d = new WDLDiagramType().createDiagram( null, "test", new DiagramInfo( null, "test" ) );
        new NextFlowImporter().importNextflow( nextflow, d );
        
//        TestWDL.exportImage(d, new File("C:/Users/Damag/dot.png"));
    }

}
