package biouml.plugins.physicell;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.physicell.BioUMLFunctionsReader.FunctionInfo;
import biouml.plugins.physicell.ode.BioUMLIntraReader;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.GreedyLayouter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.biofvm.MicroenvironmentOptions;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CellInteractions;
import ru.biosoft.physicell.core.CellTransformations;
import ru.biosoft.physicell.core.HypothesisRuleset;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.core.Motility;
import ru.biosoft.physicell.core.Secretion;
import ru.biosoft.physicell.xml.ModelReader;
import ru.biosoft.physicell.xml.ModelReader.ExternalFile;
import ru.biosoft.plugins.javascript.JSElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class PhysicellImporter implements DataElementImporter
{
    private PhysicellImportProperties properties;
    private Map<String, Node> densityNodes;
    private Map<String, File> additionalFiles = new HashMap<>();
    private BioUMLFunctionsReader functionsReader = new BioUMLFunctionsReader();
    private DataCollection dc;

    public void setAdditionalFiles(Map<String, File> files)
    {
        this.additionalFiles = files;
    }

    public Diagram read(File f, DataCollection dc, String name) throws Exception
    {
        this.dc = dc;
        Diagram result = new PhysicellDiagramType().createDiagram( dc, name, new DiagramInfo( name ) );
        result.setNotificationEnabled( false );
        ModelReader reader = new ModelReader();
        reader.setFunctionsReader( functionsReader );
        BioUMLIntraReader intracellularReader = new BioUMLIntraReader();
        intracellularReader.setDataCollection( dc );
        reader.setIntracellularReader( intracellularReader );
        intracellularReader.setAdditionalFiles( additionalFiles );
        reader.setAdditionalFiles( additionalFiles );
        ru.biosoft.physicell.core.Model model = reader.read( f, Model.class );

        convertSimulationOptions( model, result );
        convertDomain( model, result );
        convertOptions( model, result );
        convertParameters( model, result );
        convertSubstrates( model, result );
        convertCellDefinitions( model, result );
        updateCellDefinitions( model, result );
        convertInitial( new File( f.getParent() ), model, result );
        convertReport( model, result );
        convertVisualizer( model, result );
        layout( result );
        result.setNotificationEnabled( true );
        return result;
    }

    private void convertSimulationOptions(Model model, Diagram result)
    {
        PhysicellSimulationEngine engine = (PhysicellSimulationEngine)DiagramUtility.getEngine( result );
        DiagramUtility.setPreferredEngine( result, engine );
        PhysicellOptions options = (PhysicellOptions)engine.getSimulatorOptions();
        options.setFinalTime( model.getTMax() );
        options.setDiffusionDt( model.getDiffusionDt() );
        options.setMechanicsDt( model.getMechanicsDt() );
        options.setPhenotypeDt( model.getPhenotypeDt() );
        options.setImageInterval( model.getSaveImgInterval() );
        options.setReportInterval( model.getSaveFullInterval() );
        options.setCalculateGradient( model.getMicroenvironment().options.calculate_gradients );
        options.setTrackInnerSubstrates( model.getMicroenvironment().options.track_internalized_substrates_in_each_agent );
    }

    private void convertReport(Model model, Diagram result) throws Exception
    {
        ExternalFile external = model.getReportInfo();
        if( external != null && external.format.equals( "java" ) )
        {
            DataElement de = importExternalCode( external.path );
            MulticellEModel emodel = result.getRole( MulticellEModel.class );
            emodel.getReportProperties().setReportPath( de.getCompletePath() );
            emodel.getReportProperties().setCustomReport( true );
        }
    }

    private void convertVisualizer(Model model, Diagram result) throws Exception
    {
        ExternalFile external = model.getVisualizerInfo();
        if( external != null && external.format.equals( "java" ) )
        {
            DataElement de = importExternalCode( external.path );
            MulticellEModel emodel = result.getRole( MulticellEModel.class );
            emodel.getReportProperties().setVisualizerPath( de.getCompletePath() );
            emodel.getReportProperties().setCustomVisualizer( true );
        }
    }


    private void convertInitial(File folder, Model model, Diagram result) throws Exception
    {
        ExternalFile external = model.getInitialInfo();
        if( external == null )
            return;

        if( external.format.equals( "java" ) )
        {
            DataElement de = importExternalCode( external.path );
            MulticellEModel emodel = result.getRole( MulticellEModel.class );
            emodel.getInitialCondition().setCustomCondition( true );
            emodel.getInitialCondition().setCustomConditionCode( de.getCompletePath() );
            return;
        }
        String cellsFileName = external.path;
        if( cellsFileName == null )
            return;
        if( cellsFileName.startsWith( "./" ) )
            cellsFileName = cellsFileName.substring( 2 );
        
        List<String> cells = new ArrayList<>();
        String tableName = cellsFileName.substring( cellsFileName.lastIndexOf( "/" ) + 1 );
        if( additionalFiles == null || !additionalFiles.containsKey( cellsFileName ))
        {
            File cellsFile = new File( folder, cellsFileName );
            cells = ApplicationUtils.readAsList( cellsFile );
        }
        else
        {
            InputStream is = new FileInputStream( additionalFiles.get( cellsFileName ) );
            cells = ApplicationUtils.readAsList( is );
        }

        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( dc, tableName );

        //        String[] header = cells.get(0).split(","); //todo: read header, derive column types
        table.getColumnModel().addColumn( "x", DataType.Float );
        table.getColumnModel().addColumn( "y", DataType.Float );
        table.getColumnModel().addColumn( "z", DataType.Float );
        table.getColumnModel().addColumn( "type", DataType.Text );

        for( int i = 1; i < cells.size(); i++ )
        {
            String[] data = cells.get( i ).split( "," );
            Double x = Double.parseDouble( data[0] );
            Double y = Double.parseDouble( data[1] );
            Double z = Double.parseDouble( data[2] );
            String type = data[3];
            TableDataCollectionUtils.addRow( table, String.valueOf( i ), new Object[] {x, y, z, type} );
        }
        dc.put( table );
        InitialCondition c = result.getRole( MulticellEModel.class ).getInitialCondition();
        c.setCustomCondition( true );
        c.setCustomConditionTable( DataElementPath.create( table ) );
    }

    private DataElement importExternalCode(String path) throws IOException
    { 
        if( path.startsWith( "./" ) || path.startsWith( ".\\" ) )
            path = path.substring( 2 );
        File f = new File( path );
        String name = f.getName();
        if( additionalFiles != null && additionalFiles.containsKey( path ) )
            f = additionalFiles.get( path );
        String code = ApplicationUtils.readAsString( f );
        JSElement element = new JSElement( dc, name, code );
        dc.put( element );
        return element;
    }


    private void convertSubstrates(Model model, Diagram result) throws Exception
    {
        Microenvironment m = model.getMicroenvironment();
        densityNodes = new HashMap<>();
        int densities = m.densityNames.length;
        for( int i = 0; i < densities; i++ )
        {
            SubstrateProperties sp = new SubstrateProperties( m.densityNames[i] );
            Node node = sp.doCreateElements( result, new Point(), null ).nodesStream().findAny().orElse( null );
            result.put( node );
            densityNodes.put( m.densityNames[i], node );
            sp.setDecayRate( m.decayRates[i] );
            sp.setDiffusionCoefficient( m.diffusionCoefficients[i] );
            sp.setInitialCondition( m.getOptions().initial_condition_vector[i] );
            sp.setXMin( m.options.Dirichlet_xmin[i]? m.options.Dirichlet_xmin_values[i]: -1 );
            sp.setXMax( m.options.Dirichlet_xmax[i]? m.options.Dirichlet_xmax_values[i]: -1 );
            sp.setYMin( m.options.Dirichlet_ymin[i]? m.options.Dirichlet_ymin_values[i]: -1 );
            sp.setYMax( m.options.Dirichlet_ymax[i]? m.options.Dirichlet_ymax_values[i]: -1 );
            sp.setZMin( m.options.Dirichlet_zmin[i]? m.options.Dirichlet_zmin_values[i]: -1 );
            sp.setZMax( m.options.Dirichlet_zmax[i]? m.options.Dirichlet_zmax_values[i]: -1 );
        }
    }

    private void convertParameters(Model model, Diagram result)
    {
        MulticellEModel emodel = result.getRole( MulticellEModel.class );
        for( String s : model.getParameters() )
        {
            ru.biosoft.physicell.core.UserParameter userParameter = model.getParameter( s );
            UserParameter p = new UserParameter();
            p.setName( userParameter.getName() );
            p.setValue( userParameter.getValue() );
            p.setDescription( userParameter.getDescription() );
            emodel.addUserParameter( p );
        }
    }

    private void convertCellDefinitions(Model model, Diagram result) throws Exception
    {
        Microenvironment m = model.getMicroenvironment();
        int densities = m.densityNames.length;
        for( CellDefinition cd : model.getCellDefinitions() )
        {
            if( !properties.isImportDefaultDefinition() && cd.name.equals( "default" ) && model.getDefinitionsCount() != 1 )
                continue;
            CellDefinitionProperties cdp = new CellDefinitionProperties( cd.name );
            Node cdNode = cdp.doCreateElements( result, new Point(), null ).nodesStream().findAny().orElse( null );
            result.put( cdNode );
            cdp.setDefinition( cd );

            Secretion sec = cd.phenotype.secretion;
            Motility mot = cd.phenotype.motility;
            for( int i = 0; i < densities; i++ )
            {
                Node substrateNode = densityNodes.get( m.densityNames[i] );
                if( sec.netExportRates[i] != 0 || sec.secretionRates[i] != 0 || sec.uptakeRates[i] != 0 )
                {
                    Edge e = new SecretionCreator().createEdge( cdNode, substrateNode, false );
                    result.put( e );
                    SecretionProperties secretionProperties = e.getRole( SecretionProperties.class );
                    secretionProperties.setNetExportRate( sec.netExportRates[i] );
                    secretionProperties.setSecretionRate( sec.secretionRates[i] );
                    secretionProperties.setUptakeRate( sec.uptakeRates[i] );
                    secretionProperties.setSecretionTarget( sec.saturationDensities[i] );
                    cdp.getSecretionsProperties().addSecretion( secretionProperties );
                }
                if( mot.isMotile )
                {
                    if( mot.chemotacticSensitivities.length > 0 && mot.chemotacticSensitivities[i] > 0 )
                    {
                        Edge e = new ChemotaxisCreator().createEdge( cdNode, substrateNode, false );
                        result.put( e );
                        ChemotaxisProperties properties = e.getRole( ChemotaxisProperties.class );
                        cdp.getMotilityProperties().addChemotaxis( properties );
                        properties.setSensitivity( mot.chemotacticSensitivities[i] );
                    }
                    else if( mot.chemotaxisIndex == i )
                    {
                        Edge e = new ChemotaxisCreator().createEdge( cdNode, substrateNode, false );
                        result.put( e );
                        ChemotaxisProperties properties = e.getRole( ChemotaxisProperties.class );
                        cdp.getMotilityProperties().addChemotaxis( properties );
                        properties.setSensitivity( mot.chemotaxisDirection );
                    }
                }
            }
        }
    }

    /**
     * Second round - all cell definitions are already loaded into model
     */
    private void updateCellDefinitions(Model model, Diagram result) throws Exception
    {
        for( CellDefinition cd : model.getCellDefinitions() )
        {
            if( !properties.isImportDefaultDefinition() && cd.name.equals( "default" ) )
                continue;
            Node node = result.findNode( cd.name );
            CellDefinitionProperties cdp = node.getRole( CellDefinitionProperties.class );

            HypothesisRuleset ruleset = model.getRules().findRuleset( cd );
            if( ruleset != null )
                cdp.setRules( ruleset );

            CellInteractions interactions = cd.phenotype.cellInteractions;
            CellTransformations transformations = cd.phenotype.cellTransformations;
            for( int i = 0; i < model.getDefinitionsCount(); i++ )
            {
                if( interactions.attackRates[i] > 0 || interactions.fusionRates[i] > 0 || interactions.livePhagocytosisRates[i] > 0 )
                {
                    CellDefinition otherCD = model.getCellDefinition( i );
                    Node otherNode = result.findNode( otherCD.name );
                    Edge e = new InteractionCreator().createEdge( node, otherNode, false );
                    result.put( e );
                    InteractionProperties properties = e.getRole( InteractionProperties.class );
                    properties.setAttackRate( interactions.attackRates[i] );
                    properties.setFuseRate( interactions.fusionRates[i] );
                    properties.setPhagocytosisRate( interactions.livePhagocytosisRates[i] );
                    cdp.getInteractionsProperties().addInteraction( properties );
                }
                else if( transformations.transformationRates[i] > 0 )
                {
                    CellDefinition otherCD = model.getCellDefinition( i );
                    Node otherNode = result.findNode( otherCD.name );
                    Edge e = new TransformationCreator().createEdge( node, otherNode, false );
                    result.put( e );
                    TransformationProperties properties = e.getRole( TransformationProperties.class );
                    properties.setRate( transformations.transformationRates[i] );
                    cdp.getTransformationsProperties().addTransformation( properties );
                }
            }
            Map<String, FunctionInfo> functionsInfo = functionsReader.getFunctionsInfo( cd.name );
            for( Entry<String, FunctionInfo> functionInfo : functionsInfo.entrySet() )
            {
                String name = functionInfo.getKey();
                FunctionInfo info = functionInfo.getValue();
                String type = info.type;
                if( type.equals( "java" ) )
                {
                    String path = info.path;
                    DataElement de = importExternalCode( path );
                    cdp.getFunctionsProperties().setCustom( name, de.getCompletePath() );
                }
                else if( type.equals( "NONE" ) )
                {
                    cdp.getFunctionsProperties().setNotSelected( "-" );
                }
                else
                {
                    cdp.getFunctionsProperties().setValue( name, type );
                }
            }
        }
    }

    private void convertDomain(Model model, Diagram result)
    {
        Microenvironment m = model.getMicroenvironment();
        MicroenvironmentOptions options = m.getOptions();
        MulticellEModel emodel = result.getRole( MulticellEModel.class );
        DomainOptions domain = emodel.getDomain();
        domain.setXStep( options.dx );
        domain.setYStep( options.dy );
        domain.setZStep( options.dz );
        domain.setXFrom( options.X_range[0] );
        domain.setXTo( options.X_range[1] );
        domain.setYFrom( options.Y_range[0] );
        domain.setYTo( options.Y_range[1] );
        domain.setZFrom( options.Z_range[0] );
        domain.setZTo( options.Z_range[1] );
        domain.setUse2D( options.simulate2D );
    }

    private void convertOptions(Model model, Diagram result)
    {
        MulticellEModel emodel = result.getRole( MulticellEModel.class );
        ModelOptions options = emodel.getOptions();
        options.setDisableAutomatedAdhesions( model.disableAutomatedSpringAdhesions );
    }

    private void layout(Diagram d)
    {
        GreedyLayouter layouter = new GreedyLayouter();
        Graph graph = DiagramToGraphTransformer.generateGraph( d, null );
        layouter.doLayout( graph, null );
        DiagramToGraphTransformer.applyLayout( graph, d );
    }

    private boolean detectSettings(File file)
    {
        boolean isPhysicell = false;
        try (BufferedReader br = ApplicationUtils.utfReader( file ))
        {
            String line = br.readLine();
            while( line != null )
            {
                if( !isPhysicell && line.contains( "<PhysiCell_settings" ) )
                    isPhysicell = true;
                line = br.readLine();
            }
        }
        catch( Exception ex )
        {
            return false;
        }
        return isPhysicell;
    }

    @Override
    public Object getProperties(DataCollection parent, File file, String elementName)
    {
        properties = new PhysicellImportProperties();
        return properties;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String diagramName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();
        try (FileInputStream in = new FileInputStream( file );
                InputStreamReader reader = new InputStreamReader( in, StandardCharsets.UTF_8 ))
        {
            if( properties.getDiagramName() == null )
                throw new Exception( "Please specify diagram name." );

            Diagram diagram = read( file, parent, properties.getDiagramName() );

            if( jobControl != null )
                jobControl.functionFinished();
            CollectionFactoryUtils.save( diagram );
            return diagram;
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError( e );
            throw e;
        }
    }

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent.isAcceptable( Diagram.class ) )
            return file == null ? ACCEPT_HIGH_PRIORITY : accept( file );
        return ACCEPT_UNSUPPORTED;
    }

    public int accept(File file)
    {
        return ( !file.canRead() || !detectSettings( file ) ) ? ACCEPT_UNSUPPORTED : ACCEPT_HIGHEST_PRIORITY;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return Diagram.class;
    }
}