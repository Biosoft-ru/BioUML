package biouml.plugins.physicell.ode;

import java.io.File;
import java.io.FileInputStream;
//import java.util.HashMap;
//import java.util.List;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlImporter;
//import biouml.plugins.fbc.GLPKModelCreator;
//import biouml.plugins.fbc.SbmlModelFBCReader2;
//import biouml.plugins.sbml.SbmlImporter;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.SbmlModelReader;
//import biouml.plugins.sbml.SbmlModelReader_31;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import ru.biosoft.access.core.DataCollection;
//import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Phenotype;
//import ru.biosoft.physicell.fba.IntracellularFBA;
//import ru.biosoft.physicell.fba.IntracellularFBA.exchange_data;
//import ru.biosoft.physicell.fba.IntracellularFBA.kinetic_parm;
import ru.biosoft.physicell.xml.IntracellularReader;
import ru.biosoft.physicell.xml.ModelReaderSupport;

public class BioUMLIntraReader extends ModelReaderSupport implements IntracellularReader
{
    private DataCollection dc;

    private Map<String, File> additionalFiles = new HashMap<>();
    /**
     * Data collection to which model is imported, additional diagrams will be also put there
     */
    public void setDataCollection(DataCollection dc)
    {
        this.dc = dc;
    }

    @Override
    public void setAdditionalFiles(Map<String, File> additional)
    {
        additionalFiles = additional;
    }

    @Override
    public void readIntracellular(Path path, Element el, Model model, CellDefinition cd) throws Exception
    {
        Phenotype p = cd.phenotype;
        String type = getAttr( el, "type" );
        if( type.equals( "dfba" ) )
        {
            //            readIntracellularFBA( f, el, model, cd );
            return;
        }
        IntracellularODEBioUML intracellular = new IntracellularODEBioUML( model, cd );
        p.intracellular = intracellular;
        for( Element child : getAllElements( el ) )
        {
            String tag = child.getTagName();
            if( tag.equals( "intracellular_dt" ) )
            {
                double dt = getDoubleVal( child );
                intracellular.setDT( dt );
            }
            else if( tag.equals( "map" ) )
            {
                String species = getAttr( child, "sbml_species" );

                if( hasAttr( child, "PC_substrate" ) )
                {
                    String substrate = getAttr( child, "PC_substrate" );
                    intracellular.addPhenotypeSpecies( substrate, species );
                }
                else if( hasAttr( child, "PC_phenotype" ) )
                {
                    String code = getAttr( child, "PC_phenotype" );
                    intracellular.addPhenotypeSpecies( code, species );
                }
            }
            else if( tag.equals( "sbml_filename" ) )
            {
                String value = getVal( child );
                String name = value.contains( "/" ) ? value.substring( value.lastIndexOf( "/" ) + 1 ) : value;
                if( value.startsWith( "./" ) )
                    value = value.substring( 2 );

                InputStream stream = null;

                if( additionalFiles.containsKey( value ) )
                    stream = new FileInputStream( additionalFiles.get( value ) );
                else
                    stream = new FileInputStream( path.getParent().resolve( value ).toFile() );
                Diagram diagram = readSBML( stream, name );
                dc.put( diagram );
                intracellular.setDiagram( diagram );
            }
        }
    }

    public Diagram readSBML(InputStream stream, String name) throws Exception
    {
        Diagram diagram = SbmlModelFactory.readDiagram( stream, name, dc, name );
        diagram = SBGNConverterNew.convert( diagram );
        return diagram;

    }

    //    void readIntracellularFBA(File f, Element element, Model model, CellDefinition cd) throws Exception
    //    {
    //        Microenvironment m = model.getMicroenvironment();
    //        cd.phenotype.intracellular = new IntracellularFBA( model, cd );
    //        ( (IntracellularFBA)cd.phenotype.intracellular ).substrate_exchanges = new HashMap<>();
    //        Element sbml = findElement( element, "sbml_filename" );
    //        String path = f.toPath().resolve( getVal( sbml ) ).toAbsolutePath().toString();
    //        this.readSBML( path, cd );
    //
    //        List<Element> exchangeElements = findAllElements( element, "exchange" );
    //
    //        for( Element exchangeElement : exchangeElements )
    //        {
    //            String substrate = getAttr( exchangeElement, "substrate" );
    //            int index = m.findDensityIndex( substrate );
    //            String actualName = m.densityNames[index];
    //            Element fluxElement = findElement( exchangeElement, "fba_flux" );
    //            String fluxName = getVal( fluxElement );
    //            Element kmElement = findElement( exchangeElement, "Km" );
    //            double km = getDoubleVal( kmElement );
    //            Element vmaxElement = findElement( exchangeElement, "Vmax" );
    //            double vmax = getDoubleVal( vmaxElement );
    //
    //            exchange_data ed = new exchange_data();
    //            ed.density_index = index;
    //            ed.density_name = actualName;
    //            ed.fba_flux_id = fluxName;
    //            ed.Km = new kinetic_parm();
    //            ed.Km.name = "Km";
    //            ed.Km.value = km;
    //            ed.Vmax = new kinetic_parm();
    //            ed.Vmax.name = "Vmax";
    //            ed.Vmax.value = vmax;
    //            ( (IntracellularFBA)cd.phenotype.intracellular ).substrate_exchanges.put( substrate, ed );
    //        }
    //    }

    public Diagram readSBML(String path, DataCollection dc) throws Exception
    {
        File f = new File( path );
        Diagram diagram = SbmlModelFactory.readDiagram( f, dc, f.getName(), null );
        diagram = SBGNConverterNew.convert( diagram );
        dc.put( diagram );
        return diagram;
    }

    public Diagram readSBML2(String path, CellDefinition cd, DataCollection dc) throws Exception
    {

        //        String path = clazz.getResource( path ).getFile();
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new File( path ) );
        SbmlModelReader reader = (SbmlModelReader)SbmlModelFactory.getReader( document );
        //        SbmlModelFBCReader2 packageReader = new SbmlModelFBCReader2();
        return reader.read( document, "CancerMetabolism", dc );
        //        GLPKModelCreator creator = new GLPKModelCreator( "" ); //usr/local/lib64/jni" );
        //        ( (IntracellularFBA)cd.phenotype.intracellular ).model.fbcModel = (GLPKModel)creator.createModel( diagram );
    }
}