package biouml.plugins.physicell;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlExporter;
import biouml.plugins.sbml.SbmlExporter.SbmlExportProperties;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.FileExporter;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericZipExporter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.physicell.core.CycleModel;
import ru.biosoft.physicell.core.PhaseLink;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.util.TempFiles;

public class PhysicellExporter implements DataElementExporter
{
    protected static final Logger log = Logger.getLogger( PhysicellExporter.class.getName() );
    private Diagram diagram;
    private Document document;

    @Override
    public int accept(DataElement de)
    {
        if( de instanceof Diagram && ( (Diagram)de ).getType() instanceof PhysicellDiagramType )
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        else
            return DataElementExporter.ACCEPT_UNSUPPORTED;
    }


    @Override
    public void doExport(DataElement de, File file, FunctionJobControl jobControl) throws Exception
    {
        this.doExport( de, file );
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( ru.biosoft.access.core.DataCollection.class );
    }


    @Override
    public void doExport(DataElement dataElement, File file) throws Exception
    {
        List<DataElement> des = getRelatedElements( dataElement );

        log.info( "Next elements will be exported:" );
        for( ru.biosoft.access.core.DataElement de : des )
            log.info( de.getCompletePath().toString() );

        try (ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( file ) ))
        {
            for( ru.biosoft.access.core.DataElement de : des )
            {

                DataElementExporter exporter = getExporter( de );
                String extension = generateExtension( de );

                File elementFile = null;
                try
                {
                    elementFile = TempFiles.file( "zipExport." + extension );
                    exporter.doExport( de, elementFile );
                }
                catch( Exception e )
                {
                    if( elementFile != null )
                        elementFile.delete();
                    continue;
                }

                if( elementFile.exists() )
                {
                    String name = de.getName();
                    if( !name.endsWith( extension ) )
                        name = name + "." + extension;
                    ZipEntry entry = new ZipEntry( name );
                    zip.putNextEntry( entry );
                    GenericZipExporter.copyStream( new FileInputStream( elementFile ), zip );
                    zip.closeEntry();
                    elementFile.delete();
                }

            }

            Diagram diagram = (Diagram)dataElement;
            File elementFile = null;
            try
            {
                elementFile = TempFiles.file( "zipExport.xml" );
                this.write( diagram, elementFile );
                
            }
            catch( Exception e )
            {
                if( elementFile != null )
                    elementFile.delete();
            }
            if( elementFile.exists() )
            {
                ZipEntry entry = new ZipEntry( "PhysiCell_settings.xml" );
                zip.putNextEntry( entry );
                GenericZipExporter.copyStream( new FileInputStream( elementFile ), zip );
                zip.closeEntry();
                elementFile.delete();
            }
            
            if( hasRules( diagram.getRole( MulticellEModel.class ) ) )
            {
                File rulesFile =  TempFiles.file( "cell_rules.csv" );
                exportRules(diagram, rulesFile);
                if( rulesFile.exists() )
                {
                    ZipEntry entry = new ZipEntry( "cell_rules.csv" );
                    zip.putNextEntry( entry );
                    GenericZipExporter.copyStream( new FileInputStream( rulesFile ), zip );
                    zip.closeEntry();
                    rulesFile.delete();
                }
            }
            zip.closeEntry();
        }
        catch( Exception e )
        {
            file.delete();
            throw e;
        }
    }

    private void exportRules(Diagram diagram, File file) throws IOException
    {
            writeRules( file );
    }

    private List<DataElement> getRelatedElements(DataElement dataElement)
    {
        Set<String> paths = new HashSet<String>();
        MulticellEModel model = ( (Diagram)dataElement ).getRole( MulticellEModel.class );
        tryAdd( model.getReportProperties().getReportPath(), paths );
        tryAdd( model.getReportProperties().getVisualizerPath(), paths );
        tryAdd( model.getReportProperties().getGlobalReportPath(), paths );

        tryAdd( model.getInitialCondition().getCustomConditionCode(), paths );
        tryAdd( model.getInitialCondition().getCustomConditionTable(), paths );

        for( CellDefinitionProperties cdp : model.getCellDefinitions() )
        {
            tryAdd( cdp.getFunctionsProperties().getPhenotypeUpdateCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getContactCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getInstantiateCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getMembraneDistanceCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getMembraneInteractionCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getOrientationCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getVelocityUpdateCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getVolumeUpdateCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getCustomRuleCustom(), paths );
            tryAdd( cdp.getFunctionsProperties().getMigrationUpdateCustom(), paths );
        }
        return StreamEx.of(paths).map( s -> DataElementPath.create( s ).getDataElement() ).toList();
    }


    private void tryAdd(DataElementPath dep, Set<String> result)
    {
        if( dep != null && !dep.isEmpty() )
            result.add( dep.toString() );
    }

    private DataElementExporter getExporter(DataElement de)
    {
        if( de instanceof JSElement )
            return new FileExporter();
        if( de instanceof TableDataCollection )
        {
            Properties properties = new Properties();
            properties.setProperty( DataElementExporterRegistry.SUFFIX, "csv" );
            TableElementExporter exporter = new TableElementExporter();
            exporter.init( properties );
            return exporter;
        }
        else if( de instanceof Diagram )
        {
            SbmlExporter exporter = new SbmlExporter();
            SbmlExportProperties properties = (SbmlExportProperties)exporter.getProperties( de, null );
            properties.setSaveBioUMLAnnotation( true );
            return exporter;
        }
        return null;
    }

    private String generateExtension(DataElement de)
    {
        if( de instanceof JSElement )
            return "java";
        if( de instanceof TableDataCollection )
            return "csv";
        else if( de instanceof Diagram )
            return "xmls";
        return "xml";
    }

    public void write(Diagram diagram, File file) throws Exception
    {
        Document document = createDOM( diagram );
        writeDocument( file, document );
    }

    public Document createDOM(Diagram sourceDiagram) throws Exception
    {
        if( sourceDiagram == null )
            throw new NullPointerException( "Diagram to export not found." );

        diagram = sourceDiagram.clone( null, sourceDiagram.getName() );
        diagram.getType().getDiagramViewBuilder().createDiagramView( diagram, ApplicationUtils.getGraphics() );
        Util.moveToPositive( diagram );

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.newDocument();

        Element rootElement = document.createElement( "PhysiCell_settings" );
        rootElement.setAttribute( "version", "devel-version" );
        document.appendChild( rootElement );

        writeDiagram( rootElement );
        return document;
    }

    public static void writeDocument(File file, Document document) throws Exception
    {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty( javax.xml.transform.OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" ); // set because default indent amount is zero
        try (OutputStream os = new FileOutputStream( file ))
        {
            transformer.transform( new DOMSource( document ), new StreamResult( os ) );
        }
    }

    protected void writeDiagram(Element el) throws Exception
    {
        writeDomain( el );
        writeOverall( el );
        writeSave( el );
        writeOptions( el );
        writeMicroenvironment( el );
        writeCellDefinitions( el );
        writeInitialCondition( el );
        writeUserParameters( el );
        writeRulesElement( el, "cell_rules.csv" );
    }

    private Element createSimpleElement(Element parent, String name, String value)
    {
        Element result = document.createElement( name );
        result.setTextContent( value );
        parent.appendChild( result );
        return result;
    }

    private Element createSimpleElement(Element parent, String name, int value)
    {
        return createSimpleElement( parent, name, String.valueOf( value ) );
    }


    private Element createSimpleElement(Element parent, String name, double value)
    {
        return createSimpleElement( parent, name, String.valueOf( value ) );
    }

    private Element createSimpleElement(Element parent, String name, boolean value)
    {
        return createSimpleElement( parent, name, String.valueOf( value ) );
    }

    private Element createSimpleElement(Element parent, String name)
    {
        Element result = document.createElement( name );
        parent.appendChild( result );
        return result;
    }

    protected void writeDomain(Element map)
    {
        DomainOptions domain = diagram.getRole( MulticellEModel.class ).getDomain();
        Element domainElement = createSimpleElement( map, "domain" );
        createSimpleElement( domainElement, "x_min", domain.getXFrom() );
        createSimpleElement( domainElement, "x_max", domain.getXTo() );
        createSimpleElement( domainElement, "y_min", domain.getYFrom() );
        createSimpleElement( domainElement, "y_max", domain.getYTo() );
        createSimpleElement( domainElement, "z_min", domain.getZFrom() );
        createSimpleElement( domainElement, "z_max", domain.getZTo() );
        createSimpleElement( domainElement, "dx", domain.getXStep() );
        createSimpleElement( domainElement, "dy", domain.getYStep() );
        createSimpleElement( domainElement, "dz", domain.getZStep() );
        createSimpleElement( domainElement, "use_2D", domain.isUse2D() );
    }

    protected void writeOverall(Element map)
    {
        PhysicellSimulationEngine engine = (PhysicellSimulationEngine)DiagramUtility.getEngine( diagram );
        PhysicellOptions opts = (PhysicellOptions)engine.getSimulatorOptions();
        Element overallElement = createSimpleElement( map, "overall" );
        createSimpleElement( overallElement, "max_time", opts.getFinalTime() );
        createSimpleElement( overallElement, "dt_diffusion", opts.getDiffusionDt() );
        createSimpleElement( overallElement, "dt_mechanics", opts.getMechanicsDt() );
        createSimpleElement( overallElement, "dt_phenotype", opts.getPhenotypeDt() );
    }

    protected void writeSave(Element map)
    {
        PhysicellSimulationEngine engine = (PhysicellSimulationEngine)DiagramUtility.getEngine( diagram );
        PhysicellOptions opts = (PhysicellOptions)engine.getSimulatorOptions();
        Element saveElement = createSimpleElement( map, "save" );
        createSimpleElement( saveElement, "folder", "." );
        Element fullData = createSimpleElement( saveElement, "full_data" );
        createSimpleElement( fullData, "interval", opts.getReportInterval() );
        createSimpleElement( fullData, "enable", opts.isSaveReport() );
        Element svgElement = createSimpleElement( saveElement, "SVG" );
        createSimpleElement( svgElement, "interval", opts.getImageInterval() );
        createSimpleElement( svgElement, "enable", opts.isSaveImage() );
        Element legacyElement = createSimpleElement( saveElement, "legacy_data" );
        createSimpleElement( legacyElement, "enable", false );

        ReportProperties report = diagram.getRole( MulticellEModel.class ).getReportProperties();
        if( report.isCustomReport() )
        {
            Element reportElement = createSimpleElement( saveElement, "report" );
            reportElement.setAttribute( "enabled", "true" );
            reportElement.setAttribute( "type", "java" );
            createSimpleElement( reportElement, "folder", "." );
            createSimpleElement( reportElement, "filename", report.getReportPath().getName() );
        }

        if( report.isCustomVisualizer() )
        {
            Element reportElement = createSimpleElement( saveElement, "visualizer" );
            reportElement.setAttribute( "enabled", "true" );
            reportElement.setAttribute( "type", "java" );
            createSimpleElement( reportElement, "folder", "." );
            createSimpleElement( reportElement, "filename", report.getVisualizerPath().getName() );
        }
    }

    protected void writeOptions(Element map)
    {
        Element optionsElement = createSimpleElement( map, "options" );
        createSimpleElement( optionsElement, "legacy_random_points_on_sphere_in_divide", false );
        createSimpleElement( optionsElement, "virtual_wall_at_domain_edge", false );
        createSimpleElement( optionsElement, "disable_automated_spring_adhesions",
                diagram.getRole( MulticellEModel.class ).getOptions().isDisableAutomatedAdhesions() );
    }

    protected void writeMicroenvironment(Element map)
    {
        Element microenvironmentElement = createSimpleElement( map, "microenvironment_setup" );
        MulticellEModel emodel = diagram.getRole( MulticellEModel.class );
        PhysicellSimulationEngine engine = (PhysicellSimulationEngine)DiagramUtility.getEngine( diagram );
        PhysicellOptions opts = (PhysicellOptions)engine.getSimulatorOptions();
        for( int i = 0; i < emodel.getSubstrates().size(); i++ )
        {
            SubstrateProperties substrate = emodel.getSubstrates().get( i );
            writeSubstrate( microenvironmentElement, substrate, i );
        }
        Element optionsElement = createSimpleElement( microenvironmentElement, "options" );
        createSimpleElement( optionsElement, "calculate_gradients", opts.isCalculateGradient() );
        createSimpleElement( optionsElement, "track_internalized_substrates_in_each_agent", opts.isTrackInnerSubstrates() );
    }

    protected void writeSubstrate(Element parent, SubstrateProperties substrate, int id)
    {
        Element variableElement = createSimpleElement( parent, "variable" );
        variableElement.setAttribute( "name", substrate.getName() );
        variableElement.setAttribute( "ID", String.valueOf( id ) );
        variableElement.setAttribute( "units", "dimensionless" );
        Element parametersElement = createSimpleElement( variableElement, "physical_parameter_set" );
        createSimpleElement( parametersElement, "diffusion_coefficient", substrate.getDiffusionCoefficient() );
        createSimpleElement( parametersElement, "decay_rate", substrate.getDecayRate() );
        createSimpleElement( variableElement, "initial_condition", substrate.getInitialCondition() );
        Element dirichlet = createSimpleElement( variableElement, "Dirichlet_boundary_condition", 0 );
        dirichlet.setAttribute( "enabled", String.valueOf( substrate.isDirichletCondition() ) );
        Element dirichletOptions = createSimpleElement( variableElement, "Dirichlet_options" );
        Element xminElement = createSimpleElement( dirichletOptions, "boundary_value", substrate.getXMin() );
        xminElement.setAttribute( "ID", "xmin" );
        xminElement.setAttribute( "enabled", substrate.getXMin() >= 0 ? "True" : "False" );
        Element xmaxElement = createSimpleElement( dirichletOptions, "boundary_value", substrate.getXMax() );
        xmaxElement.setAttribute( "ID", "xmax" );
        xmaxElement.setAttribute( "enabled", substrate.getXMax() >= 0 ? "True" : "False" );
        Element yminElement = createSimpleElement( dirichletOptions, "boundary_value", substrate.getYMin() );
        yminElement.setAttribute( "ID", "ymin" );
        yminElement.setAttribute( "enabled", substrate.getYMin() >= 0 ? "True" : "False" );
        Element ymaxElement = createSimpleElement( dirichletOptions, "boundary_value", substrate.getYMax() );
        ymaxElement.setAttribute( "ID", "ymax" );
        ymaxElement.setAttribute( "enabled", substrate.getYMax() >= 0 ? "True" : "False" );
        Element zminElement = createSimpleElement( dirichletOptions, "boundary_value", substrate.getZMin() );
        zminElement.setAttribute( "ID", "zmin" );
        zminElement.setAttribute( "enabled", substrate.getZMin() >= 0 ? "True" : "False" );
        Element zmaxElement = createSimpleElement( dirichletOptions, "boundary_value", substrate.getZMax() );
        zmaxElement.setAttribute( "ID", "zmax" );
        zmaxElement.setAttribute( "enabled", substrate.getZMax() >= 0 ? "True" : "False" );
    }

    protected void writeCellDefinitions(Element parent) throws Exception
    {
        Element cellDefinitionsElement = createSimpleElement( parent, "cell_definitions" );
        List<CellDefinitionProperties> definitions = diagram.getRole( MulticellEModel.class ).getCellDefinitions();
        for( int i = 0; i < definitions.size(); i++ )
            writeCellDefinition( cellDefinitionsElement, definitions.get( i ), i );
    }

    protected void writeCellDefinition(Element parent, CellDefinitionProperties cdp, int id) throws Exception
    {
        Element cellDefinitionElement = createSimpleElement( parent, "cell_definition" );
        cellDefinitionElement.setAttribute( "name", cdp.getName() );
        cellDefinitionElement.setAttribute( "ID", String.valueOf( id ) );
        Element phenotypeElement = createSimpleElement( cellDefinitionElement, "phenotype" );

        writeCycle( phenotypeElement, cdp.getCycleProperties() );
        writeDeath( phenotypeElement, cdp.getDeathProperties() );
        writeVolume( phenotypeElement, cdp.getVolumeProperties() );
        writeMechanics( phenotypeElement, cdp.getMechanicsProperties() );
        writeMotility( phenotypeElement, cdp.getMotilityProperties() );
        writeSecretion( phenotypeElement, cdp.getSecretionsProperties() );
        writeCellInteractions( phenotypeElement, cdp.getInteractionsProperties() );
        writeCellTransformations( phenotypeElement, cdp.getTransformationsProperties() );
        writeCustomDatas( cellDefinitionElement, cdp.getCustomDataProperties() );
        writeFunctions( cellDefinitionElement, cdp.getFunctionsProperties() );
    }


    private void setAttr(Element el, String name, int val)
    {
        el.setAttribute( name, String.valueOf( val ) );
    }

    private void setAttr(Element el, String name, boolean val)
    {
        el.setAttribute( name, String.valueOf( val ) );
    }

    protected void writeCycle(Element parent, CycleProperties cycleProperties) throws Exception
    {
        Element cycleElement = createSimpleElement( parent, "cycle" );
        CycleModel cycle = cycleProperties.createCycle();
        cycleElement.setAttribute( "name", cycle.name );
        cycleElement.setAttribute( "code", String.valueOf( cycle.code ) );
        for( List<PhaseLink> list : cycle.phaseLinks )
        {
            for( PhaseLink link : list )
            {
                Element linkElement = createSimpleElement( cycleElement, "phase_transition_rates" );
                Element rateElement = createSimpleElement( linkElement, "rate",
                        cycle.data.getTransitionRate( link.getStartPhase().index, link.getEndPhase().index ) );
                setAttr( rateElement, "start_index", link.getStartPhase().index );
                setAttr( rateElement, "end_index", link.getEndPhase().index );
                setAttr( rateElement, "fixed_duration", link.fixedDuration );
            }
        }
    }

    protected void writeDeath(Element parent, DeathProperties deathProperties) throws Exception
    {
        //        Death death = deathProperties.createDeath( );
        Element deathElement = createSimpleElement( parent, "death" );

        for( DeathModelProperties model : deathProperties.getDeathModels() )
        {
            Element modelElement = createSimpleElement( deathElement, "model" );
            CycleModel cycle = model.getCycleProperties().createCycle();
            modelElement.setAttribute( "code", String.valueOf( cycle.code ) );
            modelElement.setAttribute( "name", model.getCycleProperties().getCycleName() );
            createSimpleElement( modelElement, "death_rate", model.getRate() );
            Element durationsElement = createSimpleElement( modelElement, "phase_durations" );
            for( int i = 0; i < cycle.phaseLinks.size(); i++ )
            {
                List<PhaseLink> list = cycle.phaseLinks.get( i );
                for( PhaseLink link : list )
                {
                    Element durationElement = createSimpleElement( durationsElement, "duration", 1.0 / cycle.data.getExitRate( i ) );

                    //                    double exitRate = cycle.data.getExitRate( i );
                    //                    models.get( death_index ).data.setExitRate( index, 1.0 / ( durationValue + 1e-16 ) );
                    //                    models.get( death_index ).phaseLinks.get( index ).get( 0 ).fixedDuration = fixedDuration;

                    durationElement.setAttribute( "index", String.valueOf( i ) );
                    durationElement.setAttribute( "fixed_duration", String.valueOf( link.fixedDuration ) );
                }
            }
            Element parametersElement = createSimpleElement( modelElement, "parameters" );
            createSimpleElement( parametersElement, "unlysed_fluid_change_rate", model.getParameters().unlysed_fluid_change_rate );
            createSimpleElement( parametersElement, "lysed_fluid_change_rate", model.getParameters().lysed_fluid_change_rate );
            createSimpleElement( parametersElement, "cytoplasmic_biomass_change_rate",
                    model.getParameters().cytoplasmic_biomass_change_rate );
            createSimpleElement( parametersElement, "nuclear_biomass_change_rate", model.getParameters().nuclear_biomass_change_rate );
            createSimpleElement( parametersElement, "calcification_rate", model.getParameters().calcification_rate );
            createSimpleElement( parametersElement, "relative_rupture_volume", model.getParameters().relative_rupture_volume );
        }
    }
    //  <death>
    //  <model code="100" name="apoptosis">
    //    <death_rate units="1/min">0</death_rate>
    //    <phase_durations units="min">
    //      <duration index="0" fixed_duration="true">516</duration>
    //    </phase_durations>
    //    <parameters>
    //      <unlysed_fluid_change_rate units="1/min">0.05</unlysed_fluid_change_rate>
    //      <lysed_fluid_change_rate units="1/min">0</lysed_fluid_change_rate>
    //      <cytoplasmic_biomass_change_rate units="1/min">1.66667e-02</cytoplasmic_biomass_change_rate>
    //      <nuclear_biomass_change_rate units="1/min">5.83333e-03</nuclear_biomass_change_rate>
    //      <calcification_rate units="1/min">0</calcification_rate>
    //      <relative_rupture_volume units="dimensionless">2.0</relative_rupture_volume>
    //    </parameters>
    //  </model>
    //  <model code="101" name="necrosis">
    //    <death_rate units="1/min">0.0</death_rate>
    //    <phase_durations units="min">
    //      <duration index="0" fixed_duration="true">0</duration>
    //    <duration index="1" fixed_duration="true">86400</duration>
    //    </phase_durations>
    //    <parameters>
    //      <unlysed_fluid_change_rate units="1/min">1.11667e-2</unlysed_fluid_change_rate>
    //      <lysed_fluid_change_rate units="1/min">8.33333e-4</lysed_fluid_change_rate>
    //      <cytoplasmic_biomass_change_rate units="1/min">5.33333e-5</cytoplasmic_biomass_change_rate>
    //      <nuclear_biomass_change_rate units="1/min">2.16667e-3</nuclear_biomass_change_rate>
    //      <calcification_rate units="1/min">0</calcification_rate>
    //      <relative_rupture_volume units="dimensionless">2.0</relative_rupture_volume>
    //    </parameters>
    //  </model>
    //</death>
    protected void writeVolume(Element parent, VolumeProperties volumeProperties) throws Exception
    {
        Element volumeElement = createSimpleElement( parent, "volume" );
        createSimpleElement( volumeElement, "total", volumeProperties.getTotal() );
        createSimpleElement( volumeElement, "fluid_fraction", volumeProperties.getFluid_fraction() );
        createSimpleElement( volumeElement, "nuclear", volumeProperties.getNuclear() );
        createSimpleElement( volumeElement, "cytoplasmic_biomass_change_rate", volumeProperties.getCytoplasmic_biomass_change_rate() );
        createSimpleElement( volumeElement, "nuclear_biomass_change_rate", volumeProperties.getNuclear_biomass_change_rate() );
        createSimpleElement( volumeElement, "calcified_fraction", volumeProperties.getCalcified_fraction() );
        createSimpleElement( volumeElement, "calcification_rate", volumeProperties.getCalcification_rate() );
        createSimpleElement( volumeElement, "relative_rupture_volume", volumeProperties.getRelative_rupture_volume() );
    }

    protected void writeMechanics(Element parent, MechanicsProperties mechanicsProperties) throws Exception
    {
        Element mechanicsElement = createSimpleElement( parent, "mechanics" );
        createSimpleElement( mechanicsElement, "cell_cell_adhesion_strength", mechanicsProperties.getCellCellAdhesionStrength() );
        createSimpleElement( mechanicsElement, "cell_cell_repulsion_strength", mechanicsProperties.getCellCellRepulsionStrength() );
        createSimpleElement( mechanicsElement, "relative_maximum_adhesion_distance", mechanicsProperties.getRelMaxAdhesionDistance() );
        createSimpleElement( mechanicsElement, "cell_BM_adhesion_strength", mechanicsProperties.getCellBMAdhesionStrength() );
        createSimpleElement( mechanicsElement, "cell_BM_repulsion_strength", mechanicsProperties.getCellBMRepulsionStrength() );
        createSimpleElement( mechanicsElement, "attachment_elastic_constant", mechanicsProperties.getAttachmentElasticConstant() );
        createSimpleElement( mechanicsElement, "attachment_rate", mechanicsProperties.getAttachmentRate() );
        createSimpleElement( mechanicsElement, "detachment_rate", mechanicsProperties.getDetachmentRate() );

        //        Element optionsElement = createSimpleElement(  mechanicsElement, "options" );
        //        Element optionsElement =createSimpleElement(  optionsElement, "set_relative_equilibrium_distance", mechanicsProperties);
        //        Element optionsElement =createSimpleElement(  optionsElement, "set_absolute_equilibrium_distance", mechanicsProperties.getDetachmentRate() );
        //        
        //        Element adhesionElement = createSimpleElement(  mechanicsElement, "cell_adhesion_affinities" );
        //        for 
        //        createSimpleElement(  volumeElement, "calcified_fraction", volumeProperties.getCalcified_fraction() );
        //        createSimpleElement(  volumeElement, "calcification_rate", volumeProperties.getCalcification_rate());
        //        createSimpleElement(  volumeElement, "relative_rupture_volume", volumeProperties.getRelative_rupture_volume() );
    }
    //
    //      <mechanics>
    //        <cell_cell_adhesion_strength units="micron/min">0.4</cell_cell_adhesion_strength>
    //        <cell_cell_repulsion_strength units="micron/min">10.0</cell_cell_repulsion_strength>
    //        <relative_maximum_adhesion_distance units="dimensionless">1.25</relative_maximum_adhesion_distance>
    //        <cell_adhesion_affinities>
    //            <cell_adhesion_affinity name="director cell">1.0</cell_adhesion_affinity>
    //            <cell_adhesion_affinity name="cargo cell">1.0</cell_adhesion_affinity>
    //            <cell_adhesion_affinity name="worker cell">1.0</cell_adhesion_affinity>
    //            </cell_adhesion_affinities>
    //        <options>
    //          <set_relative_equilibrium_distance enabled="false" units="dimensionless">1.8</set_relative_equilibrium_distance>
    //          <set_absolute_equilibrium_distance enabled="false" units="micron">15.12</set_absolute_equilibrium_distance>
    //        </options>
    //        <cell_BM_adhesion_strength units="micron/min">4.0</cell_BM_adhesion_strength>
    //        <cell_BM_repulsion_strength units="micron/min">10.0</cell_BM_repulsion_strength>
    //        <attachment_elastic_constant units="1/min">0.5</attachment_elastic_constant>
    //        <attachment_rate units="1/min">10.0</attachment_rate>
    //        <detachment_rate units="1/min">0.0</detachment_rate>
    //      </mechanics>


    protected void writeMotility(Element parent, MotilityProperties motilityProperties) throws Exception
    {
        Element motilityElement = createSimpleElement( parent, "motility" );
        createSimpleElement( motilityElement, "speed", motilityProperties.getMigrationSpeed() );
        createSimpleElement( motilityElement, "persistence_time", motilityProperties.getPersistenceTime() );
        createSimpleElement( motilityElement, "migration_bias", motilityProperties.getMigrationBias() );
        Element optionsElement = createSimpleElement( motilityElement, "options" );
        createSimpleElement( optionsElement, "enabled", motilityProperties.isMotile() );
        createSimpleElement( optionsElement, "use_2D", motilityProperties.isRestrictTo2D() );

        ChemotaxisProperties[] chemotaxisArray = motilityProperties.getChemotaxis();
        if( chemotaxisArray.length == 1 )
        {
            Element chemotaxisElement = createSimpleElement( optionsElement, "chemotaxis" );
            createSimpleElement( chemotaxisElement, "enabled", "true" );
            createSimpleElement( chemotaxisElement, "substrate", chemotaxisArray[0].getTitle() );
            createSimpleElement( chemotaxisElement, "direction", chemotaxisArray[0].getDirection() );
        }
        else if( chemotaxisArray.length > 1 )
        {
            Element achemotaxisElement = createSimpleElement( optionsElement, "advanced_chemotaxis" );
            createSimpleElement( achemotaxisElement, "enabled", "true" );
            Element sensetivitiesElement = createSimpleElement( achemotaxisElement, "chemotactic_sensitivities" );
            //          createSimpleElement(  achemotaxisElement, "normalize_each_gradient", motilityProperties.isNo);//TODO: check
            for( int i = 0; i < chemotaxisArray.length; i++ )
            {
                Element sensetivityElement = createSimpleElement( sensetivitiesElement, "chemotactic_sensitivity",
                        chemotaxisArray[i].getSensitivity() );
                sensetivityElement.setAttribute( "substrate", chemotaxisArray[i].getTitle() );
            }
        }
    }

    protected void writeSecretion(Element parent, SecretionsProperties secretionsProperties) throws Exception
    {
        Element secretionElement = createSimpleElement( parent, "secretion" );
        MulticellEModel emodel = diagram.getRole( MulticellEModel.class );

        for( SubstrateProperties substrateProperties : emodel.getSubstrates() )
        {
            SecretionProperties secretionProperties = null;
            for( SecretionProperties sp : secretionsProperties.secretions )
            {
                if( sp.getTitle().equals( substrateProperties.getName() ) )
                {
                    secretionProperties = sp;
                    break;
                }
            }

            Element substrateElement = createSimpleElement( secretionElement, "substrate" );
            substrateElement.setAttribute( "name", substrateProperties.getName() );
            if( secretionProperties == null )
            {
                createSimpleElement( substrateElement, "secretion_rate", 0 );
                createSimpleElement( substrateElement, "secretion_target", 0 );
                createSimpleElement( substrateElement, "uptake_rate", 0 );
                createSimpleElement( substrateElement, "net_export_rate", 0 );
            }
            else
            {
                createSimpleElement( substrateElement, "secretion_rate", secretionProperties.getSecretionRate() );
                createSimpleElement( substrateElement, "secretion_target", secretionProperties.getSecretionTarget() );
                createSimpleElement( substrateElement, "uptake_rate", secretionProperties.getUptakeRate() );
                createSimpleElement( substrateElement, "net_export_rate", secretionProperties.getNetExportRate() );
            }

        }
    }

    protected void writeCellInteractions(Element parent, InteractionsProperties interactionProperites) throws Exception
    {
        if( interactionProperites.getInteractions().length == 0 )
            return;

        MulticellEModel emodel = diagram.getRole( MulticellEModel.class );

        Element interactionsElement = createSimpleElement( parent, "cell_interactions" );
        createSimpleElement( interactionsElement, "dead_phagocytosis_rate", interactionProperites.getDeadPhagocytosisRate() );
        createSimpleElement( interactionsElement, "damage_rate", interactionProperites.getDamageRate() );

        Map<String, InteractionProperties> interactionMapping = new HashMap<>();
        for( InteractionProperties interactionProperties : interactionProperites.getInteractions() )
            interactionMapping.put( interactionProperties.getCellType(), interactionProperties );

        Element livePhagocytosisElement = createSimpleElement( interactionsElement, "live_phagocytosis_rates" );
        for( CellDefinitionProperties cdp : emodel.getCellDefinitions() )
        {
            InteractionProperties properties = interactionMapping.get( cdp.getName() );
            Element el = createSimpleElement( livePhagocytosisElement, "phagocytosis_rate",
                    properties == null ? 0 : properties.getPhagocytosisRate() );
            el.setAttribute( "name", cdp.getName() );
        }

        Element attacksElement = createSimpleElement( interactionsElement, "attack_rates" );
        for( CellDefinitionProperties cdp : emodel.getCellDefinitions() )
        {
            InteractionProperties properties = interactionMapping.get( cdp.getName() );
            Element el = createSimpleElement( attacksElement, "attack_rate", properties == null ? 0 : properties.getAttackRate() );
            el.setAttribute( "name", cdp.getName() );
        }

        Element fusionsElement = createSimpleElement( interactionsElement, "fusion_rates" );
        for( CellDefinitionProperties cdp : emodel.getCellDefinitions() )
        {
            InteractionProperties properties = interactionMapping.get( cdp.getName() );
            Element el = createSimpleElement( fusionsElement, "fusion_rate", properties == null ? 0 : properties.getFuseRate() );
            el.setAttribute( "name", cdp.getName() );
        }

    }



    protected void writeCellTransformations(Element parent, TransformationsProperties interactionsProperites) throws Exception
    {
        if( interactionsProperites.getTransformations().length == 0 )
            return;

        MulticellEModel emodel = diagram.getRole( MulticellEModel.class );
        Element transformationsElement = createSimpleElement( parent, "cell_transformations" );
        Element ratesElement = createSimpleElement( transformationsElement, "transformation_rates" );

        Map<String, TransformationProperties> transformationMapping = new HashMap<>();
        for( TransformationProperties transformationProperties : interactionsProperites.getTransformations() )
            transformationMapping.put( transformationProperties.getCellType(), transformationProperties );

        for( CellDefinitionProperties cdp : emodel.getCellDefinitions() )
        {
            TransformationProperties properties = transformationMapping.get( cdp.getName() );
            Element el = createSimpleElement( ratesElement, "transformation_rate", properties == null ? 0 : properties.getRate() );
            el.setAttribute( "name", cdp.getName() );
        }
    }

    protected void writeCustomDatas(Element parent, CustomDataProperties properties) throws Exception
    {
        Element customDataProperties = createSimpleElement( parent, "custom_data" );

        for( VariableProperties variableProperties : properties.getVariables() )
        {
            Element transformationsElement = createSimpleElement( customDataProperties, variableProperties.getName(),
                    variableProperties.getValue() );
            transformationsElement.setAttribute( "conserved", String.valueOf( variableProperties.isConserved() ) );
            transformationsElement.setAttribute( "units", "dimensionless" );
        }
    }

    protected void writeInitialCondition(Element parent)
    {
        MulticellEModel emodel = diagram.getRole( MulticellEModel.class );
        InitialCondition ic = emodel.getInitialCondition();
        Element initialConditionElement = createSimpleElement( parent, "initial_conditions" );
        Element positionElement = createSimpleElement( initialConditionElement, "cell_positions" );

        positionElement.setAttribute( "enabled", String.valueOf( ic.isCustomCondition() ) );
        String name = null;
        if( ic.getCustomConditionCode() != null && !ic.getCustomConditionCode().isEmpty() )
        {
            name = ic.getCustomConditionCode().getName();
            positionElement.setAttribute( "type", "java" );
        }
        else if( ic.getCustomConditionTable() != null && !ic.getCustomConditionTable().isEmpty() )
        {
            name = ic.getCustomConditionTable().getName();
            positionElement.setAttribute( "type", "csv" );
        }

        createSimpleElement( positionElement, "folder", "." );
        createSimpleElement( positionElement, "filename", name );
    }

    protected void writeUserParameters(Element parent)
    {
        Element userParametersElement = createSimpleElement( parent, "user_parameters" );
        MulticellEModel emodel = diagram.getRole( MulticellEModel.class );
        for( UserParameter parameter : emodel.getUserParmeters().getParameters() )
        {
            Element parameterElement = createSimpleElement( userParametersElement, parameter.getName(), parameter.getValue() );
            parameterElement.setAttribute( "units", parameter.getUnits() );
            parameterElement.setAttribute( "description", parameter.getDescription() );
        }
    }

    protected void writeFunctions(Element parent, FunctionsProperties properties)
    {
        Element functionsElement = createSimpleElement( parent, "functions" );

        Element phenotypeElement = createSimpleElement( functionsElement, "function" );
        phenotypeElement.setAttribute( "name", "update_phenotype" );
        if( !properties.isDefaultPhenotype() )
        {
            phenotypeElement.setAttribute( "file", properties.getPhenotypeUpdateCustom().getName() );
            phenotypeElement.setAttribute( "type", "java" );

        }
        else
            phenotypeElement.setAttribute( "type", getTypeAttr( properties.getPhenotypeUpdate() ) );

        Element customRuleElement = createSimpleElement( functionsElement, "function" );
        customRuleElement.setAttribute( "name", "custom_cell_rule" );
        if( !properties.isDefaultRule() )
        {
            customRuleElement.setAttribute( "file", properties.getCustomRuleCustom().getName() );
            customRuleElement.setAttribute( "type", "java" );
        }
        else
            customRuleElement.setAttribute( "type", getTypeAttr( properties.getCustomRule() ) );

        Element updateMigrationElement = createSimpleElement( functionsElement, "function" );
        updateMigrationElement.setAttribute( "name", "update_migration_bias" );

        if( !properties.isDefaultMigration() )
        {
            updateMigrationElement.setAttribute( "file", properties.getMigrationUpdateCustom().getName() );
            updateMigrationElement.setAttribute( "type", "java" );
        }
        else
            updateMigrationElement.setAttribute( "type", getTypeAttr( properties.getMigrationUpdate() ) );

        Element instantiateElement = createSimpleElement( functionsElement, "function" );
        instantiateElement.setAttribute( "name", "instantiate_cell" );
        if( !properties.isDefaultInstantiate() )
        {
            instantiateElement.setAttribute( "file", properties.getInstantiateCustom().getName() );
            instantiateElement.setAttribute( "type", "java" );
        }
        else
            instantiateElement.setAttribute( "type", getTypeAttr( properties.getInstantiate() ) );

        Element volumeElement = createSimpleElement( functionsElement, "function" );
        volumeElement.setAttribute( "name", "volume_update_function" );
        if( !properties.isDefaultVolume() )
        {
            volumeElement.setAttribute( "file", properties.getVolumeUpdateCustom().getName() );
            volumeElement.setAttribute( "type", "java" );
        }
        else
            volumeElement.setAttribute( "type", getTypeAttr( properties.getVolumeUpdate() ) );

        Element velocityElement = createSimpleElement( functionsElement, "function" );
        velocityElement.setAttribute( "name", "update_velocity" );
        if( !properties.isDefaultVelocity() )
        {
            velocityElement.setAttribute( "file", properties.getVelocityUpdateCustom().getName() );
            velocityElement.setAttribute( "type", "java" );
        }
        else
            velocityElement.setAttribute( "type", getTypeAttr( properties.getVelocityUpdate() ) );

        Element contactElement = createSimpleElement( functionsElement, "function" );
        contactElement.setAttribute( "name", "contact_function" );
        if( !properties.isDefaultContact() )
        {
            contactElement.setAttribute( "file", properties.getContactCustom().getName() );
            contactElement.setAttribute( "type", "java" );
        }
        else
            contactElement.setAttribute( "type", getTypeAttr( properties.getContact() ) );

        Element membraneInteractionElement = createSimpleElement( functionsElement, "function" );
        membraneInteractionElement.setAttribute( "name", "add_cell_basement_membrane_interactions" );
        if( !properties.isDefaultMBDistance() )
        {
            membraneInteractionElement.setAttribute( "file", properties.getMembraneInteractionCustom().getName() );
            membraneInteractionElement.setAttribute( "type", "java" );
        }
        else
            membraneInteractionElement.setAttribute( "type", getTypeAttr( properties.getMembraneInteraction() ) );

        Element membraneDistanceElement = createSimpleElement( functionsElement, "function" );
        membraneDistanceElement.setAttribute( "name", "calculate_distance_to_membrane" );
        if( !properties.isDefaultMBDistance() )
        {
            membraneDistanceElement.setAttribute( "file", properties.getMembraneDistanceCustom().getName() );
            membraneDistanceElement.setAttribute( "type", "java" );
        }
        else
            membraneDistanceElement.setAttribute( "type", getTypeAttr( properties.getMembraneDistance() ) );
    }

    protected void writeRulesElement(Element parent, String fileName)
    {
        Element rulesElement = createSimpleElement( parent, "cell_rules" );
        Element rulesetsElement = createSimpleElement( rulesElement, "rulesets" );
        Element rulesetElement = createSimpleElement( rulesetsElement, "ruleset" );
        rulesetElement.setAttribute( "protocol", "CBHG" );
        rulesetElement.setAttribute( "version", "2.0" );
        rulesetElement.setAttribute( "format", "csv" );
        rulesetElement.setAttribute( "enabled", String.valueOf( hasRules( diagram.getRole( MulticellEModel.class ) ) ) );
        createSimpleElement( rulesetElement, "folder", "." );
        createSimpleElement( rulesetElement, "filename", fileName );
    }

    protected void writeRules(File f) throws IOException
    {
        try (BufferedWriter bw = new BufferedWriter( new FileWriter( f ) ))
        {
            MulticellEModel role = diagram.getRole( MulticellEModel.class );
            for( CellDefinitionProperties cd : role.getCellDefinitions() )
            {
                RulesProperties rp = cd.getRulesProperties();
                String cell_type = cd.getName();

                for( RuleProperties rule : rp.getRules() )
                {
                    bw.append( cell_type + "," + rule.getSignal() + "," + rule.getDirection() + "," + rule.getBehavior() + ","
                    // + base_value + "," 
                            + rule.getSaturationValue() + "," + rule.getHalfMax() + "," + rule.getHillPower() + "," + (rule.isApplyToDead() ? "1.0": "0.0")
                            + "\n" );
                }
            }
        }
    }

    private boolean hasRules(MulticellEModel emodel)
    {
        for( CellDefinitionProperties cd : emodel.getCellDefinitions() )
        {
            RulesProperties rp = cd.getRulesProperties();
            if( rp.getRules().length > 0 )
                return true;
        }
        return false;
    }

    private String getTypeAttr(String userString)
    {
        if( userString.equals( PhysicellConstants.NOT_SELECTED ) )
            return "NONE";

        return userString;
    }

}