package biouml.plugins.keynodes.customhub;

import java.util.ArrayList;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon ( "resources/DiagramToPairs.png" )
public class DiagramToPairsConverter extends AnalysisMethodSupport<DiagramToPairsConverterParameters>
{

    public DiagramToPairsConverter(DataCollection<?> origin, String name)
    {
        super( origin, name, new DiagramToPairsConverterParameters() );
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        DiagramToPairsConverterParameters parameters = getParameters();
        Diagram diagram = parameters.getDiagramPath().getDataElement( Diagram.class );
        List<FromToPair> pairs = new ArrayList<>();
        jobControl.setPreparedness( 5 );
        log.info( "Generating iteraction pairs..." );

        //process Reaction nodes
        diagram.recursiveStream().select( Node.class ).filter( n -> n.getKernel() != null && n.getKernel() instanceof Reaction )
                .forEach( n -> {
            Reaction reaction = (Reaction)n.getKernel();
            SpecieReference[] refs = reaction.getSpecieReferences();
            String reactants = StreamEx.of( refs ).filter( sr ->sr.getRole().equals(SpecieReference.REACTANT)).map( sr -> sr.getSpecieName() ).joining( TableDataCollectionUtils.ID_SEPARATOR );
            String products = StreamEx.of( refs ).filter( sr ->sr.getRole().equals(SpecieReference.PRODUCT)).map( sr -> sr.getSpecieName() ).joining( TableDataCollectionUtils.ID_SEPARATOR );
            if( !reactants.isEmpty() && !products.isEmpty() )
                pairs.add( new FromToPair( reactants, products ) );
        } );

        //process direct edges
        diagram.recursiveStream().select( Edge.class ).remove( e -> e.getInput().getKernel() == null || e.getOutput().getKernel() == null )
                .remove( e -> e.getInput().getKernel() instanceof Reaction || e.getOutput().getKernel() instanceof Reaction )
                .forEach( e -> pairs.add( new FromToPair( e.getInput().getKernel().getName(), e.getOutput().getKernel().getName() ) ) );


        jobControl.setPreparedness( 50 );
        log.info( "Generating output table..." );

        DataElementPath tablePath = parameters.getTablePath();
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( tablePath.getParentCollection(),
                tablePath.getName() );
        tdc.getColumnModel().addColumn( "Node from", String.class );
        tdc.getColumnModel().addColumn( "Node to", String.class );
        tdc.getColumnModel().addColumn( "Weigth", Double.class );

        if( jobControl.isStopped() )
            return null;

        for( int rowId = 0; rowId < pairs.size(); rowId++ )
        {
            FromToPair p = pairs.get( rowId );
            String fromName = p.from;
            String toName = p.to;
            String rowName = fromName + " -> " + toName;
            TableDataCollectionUtils.addRow( tdc, rowName, new Object[] {fromName, toName, parameters.getWeight()} );
        }

        jobControl.setPreparedness( 90 );

        if( tdc.getSize() != 0 )
        {
            CollectionFactoryUtils.save( tdc );
        }
        else
            throw new Exception( "Empty result: table was not created" );

        return tdc;
    }

    private static class FromToPair
    {
        String from;
        String to;

        public FromToPair(String from, String to)
        {
            super();
            this.from = from;
            this.to = to;
        }
    }
}
