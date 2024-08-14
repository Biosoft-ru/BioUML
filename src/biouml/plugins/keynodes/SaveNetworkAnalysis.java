package biouml.plugins.keynodes;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import biouml.model.util.AddElementsUtils;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.standard.type.Molecule;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.exception.TableNoColumnException;

/**
 * @author anna
 *
 */
@ClassIcon ( "resources/save-network.gif" )
public class SaveNetworkAnalysis extends AnalysisMethodSupport<SaveNetworkAnalysisParameters>
{
    public SaveNetworkAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptKeyNodes.class, new SaveNetworkAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();

        SaveNetworkAnalysisParameters params = getParameters();
        DataElementPath knResultPath = params.getKnResultPath();

        DataElement knResult = knResultPath.getDataElement();
        AnalysisMethod method = AnalysisParametersFactory.readAnalysis( knResult );
        if( ! ( method instanceof PathGenerator ) || ! ( method.getParameters() instanceof BasicKeyNodeAnalysisParameters ) )
            method = AnalysisParametersFactory.readAnalysisPersistent( knResult );
        if( ! ( method instanceof PathGenerator ) || ! ( method.getParameters() instanceof BasicKeyNodeAnalysisParameters ) )
            throw new InvalidParameterException( "This table is not a result of master regulator node analysis or longest chain finder" );
        if( ( (BasicKeyNodeAnalysisParameters)method.getParameters() ).getBioHub() == null
                || ( (PathGenerator)method ).getKeyNodesHub() == null )
            throw new InvalidParameterException( "Result of analysis is incorrect (Unable to read biohub parameter)" );
        if( ! ( knResult instanceof TableDataCollection ) )
            throw new InvalidParameterException("This element is not a table");
    }

    @Override
    public TableDataCollection[] justAnalyzeAndPut() throws Exception
    {
        SaveNetworkAnalysisParameters params = getParameters();
        TableDataCollection knResult = params.getKnResultPath().getDataElement(TableDataCollection.class);
        DataElementPath destination = parameters.getOutputPath();
        List<DataElement> selectedItems = params.getSelectedItems();

        AnalysisMethod method = AnalysisParametersFactory.readAnalysis( knResult );
        if( ! ( method instanceof PathGenerator ) || ! ( method.getParameters() instanceof BasicKeyNodeAnalysisParameters ) )
            method = AnalysisParametersFactory.readAnalysisPersistent( knResult );
        PathGenerator analysis = (PathGenerator)method;

        ColumnModel columnModel = knResult.getColumnModel();
        int hitsColumn = knResult.columns()
                .filter( column -> column.getValueClass() == StringSet.class )
                .mapToInt( column -> columnModel.optColumnIndex(column.getName()) )
                .findFirst().orElseThrow( () -> new TableNoColumnException(knResult, "Hits") );

        DataCollection<?> origin = destination.getParentCollection();

        List<? extends DataElement> rdes = null;
        int totalElements = 0;
        if( selectedItems != null )
        {
            rdes = selectedItems;
            totalElements = selectedItems.size();
        }
        else
        {
            int scoreColumn = columnModel.getColumnIndex( parameters.getRankColumn() );
            knResult.sortTable(scoreColumn, false);
            totalElements = parameters.getNumTopRanking();
            rdes = knResult.stream().limit( totalElements ).collect( Collectors.toList() );
        }

        jobControl.setPreparedness( 3 );
        List<TableDataCollection> result = new ArrayList<>();
        if( params.isSeparateResults() && totalElements > 1 )
        {
            double oneRowProgress = 95.0 / totalElements;

            for( int i = 0; i < totalElements && !jobControl.isStopped(); i++ )
            {
                jobControl.pushProgress( (int) ( i * oneRowProgress ), (int) ( ( i + 1 ) * oneRowProgress ) );
                RowDataElement rde = (RowDataElement)rdes.get( i );
                try
                {
                    String tableName = destination.getName() + "(" + rde.getName() + ")";
                    TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( origin,
                            tableName );
                    log.info( "Creating " + tableName );
                    tdc = fillTable( tdc, Arrays.asList( rde ), hitsColumn, totalElements, analysis );
                    if( tdc == null || tdc.isEmpty() )
                    {
                        log.log(Level.SEVERE,  "Table for '" + rde.getName() + "' is empty; skipping" );
                    }
                    else
                    {
                        ReferenceTypeRegistry.copyCollectionReferenceType( tdc, knResult );
                        CollectionFactoryUtils.save( tdc );
                        result.add( tdc );
                    }
                }
                catch( Exception e1 )
                {
                    log.warning( ExceptionRegistry.log( e1 ) );
                }
                jobControl.popProgress();
            }
        }
        else
        {
            TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( origin, destination.getName() );
            tdc = fillTable( tdc, rdes, hitsColumn, totalElements, analysis );
            if( tdc != null && tdc.getSize() != 0 )
            {
                ReferenceTypeRegistry.copyCollectionReferenceType( tdc, knResult );
                CollectionFactoryUtils.save( tdc );
                result.add( tdc );
            }
            else
                throw new Exception( "Empty result: table was not created" );
        }

        if( result.size() == 0 )
            return null;
        jobControl.setPreparedness( 100 );

        return result.toArray( new TableDataCollection[0] );
    }

    private TableDataCollection fillTable(TableDataCollection result, List<? extends DataElement> rdes, int hitsColumn, int totalElements,
            PathGenerator analysis)
    {
        result.getColumnModel().addColumn( "Molecule name", String.class );
        result.getColumnModel().addColumn( "Role", String.class );

        Set<String> allhits = new HashSet<>();
        Set<String> allkeys = new HashSet<>();
        Set<String> all = new HashSet<>();
        HashMap<String, String> titles = new HashMap<>();

        KeyNodesHub<?> bioHub = analysis.getKeyNodesHub();
        int limit = rdes.size() > totalElements ? totalElements : rdes.size();
        for( int j = 0; j < limit && !jobControl.isStopped(); j++ )
        {
            RowDataElement rde = (RowDataElement)rdes.get( j );
            Object[] values = rde.getValues();

            allkeys.addAll( analysis.getKeysFromName( rde.getName() ) );
            Object value = values[hitsColumn];
            StringSet hits = null;
            if( value instanceof StringSet )
            {
                hits = (StringSet)value;
            }
            if( hits != null )
            {
                allhits.addAll( hits );
                try
                {
                    List<Element[]> paths = analysis.generatePaths( rde.getName(), hits );
                    for( int i = 0; i < paths.size(); i++ )
                    {
                        Element[] path = paths.get( i );
                        if( path == null || path.length == 0 )
                            continue;
                        for( Element elem : path )
                        {
                            DataElement base = AddElementsUtils.optKernel( elem );
                            if( base != null && base instanceof Molecule )
                            {
                                all.add( elem.getAccession() );
                                titles.put( elem.getAccession(), bioHub.getElementTitle( elem ) );
                            }
                        }
                        jobControl.setPreparedness( 3 + j * 67 / totalElements + i * 67 / ( totalElements * paths.size() ) );
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE,  "Error while creating network for '" + rde.getName() + "'", e );
                }
            }
        }

        for( String nodename : all )
        {
            String role = allhits.contains( nodename ) ? allkeys.contains( nodename ) ? "hit, master" : "hit"
                    : allkeys.contains( nodename ) ? "master" : "network element";
            TableDataCollectionUtils.addRow( result, nodename, new String[] {titles.get( nodename ), role} );
        }

        return result;
    }
}
