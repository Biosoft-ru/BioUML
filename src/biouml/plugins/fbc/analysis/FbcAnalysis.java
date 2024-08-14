package biouml.plugins.fbc.analysis;

import java.util.function.Function;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.fbc.FbcModel;
import biouml.plugins.fbc.FbcModelCreator;
import biouml.plugins.sbml.SbmlConstants;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/FluxBalanceAnalysis.gif")
public class FbcAnalysis extends AnalysisMethodSupport<FbcAnalysisParameters>
{
    private static final String OPTIMAL_VALUES = "Optimal Values";
    private static final String VALUE_FUNCTION = "Value Function";

    public FbcAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new FbcAnalysisParameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        Diagram diagram = parameters.getDiagramPath().getDataElement(Diagram.class);
        TableDataCollection fbcData = parameters.getFbcDataTablePath().getDataElement(TableDataCollection.class);
        String typeObjectiveFunction = parameters.getTypeObjectiveFunction();
        jobControl.pushProgress( 5, 90 );
        TableDataCollection tdc = getFbcResult( diagram, fbcData, typeObjectiveFunction, parameters.getCreator(), jobControl );
        jobControl.popProgress();
        jobControl.setPreparedness( 90 );
        DataElementPath path = parameters.getFbcResultPath();
        DataCollection<DataElement> origin = path.getParentCollection();
        String name = path.getName();

        origin.remove(name);
        TableDataCollection toPut = getFbcResultToPut(origin, name, diagram, tdc);
        String refTypeStr = diagram.getAttributes().getValueAsString( SbmlConstants.BIOUML_REFERENCE_TYPE_ATTR );
        if( refTypeStr != null && !refTypeStr.isEmpty() )
        {
            refTypeStr = "Reactions" + refTypeStr.substring( refTypeStr.indexOf( ':' ) );
            ReferenceType refType = ReferenceTypeRegistry.optReferenceType( refTypeStr );
            if( refType != null )
                toPut.setReferenceType( refType.getDisplayName() );
        }
        CollectionFactoryUtils.save(toPut);
        return toPut;
    }

    public static TableDataCollection getFbcResult(Diagram diagram, TableDataCollection fbcData, String typeObjectiveFunction,
            FbcModelCreator creator, JobControl jobControl)
    {
        if( diagram != null )
        {
            TableDataCollection tdc = new StandardTableDataCollection(null, "");
            FbcModel model = creator.createModel(diagram, fbcData, typeObjectiveFunction);
            if(model == null)
                return null;
            if( jobControl != null )
                jobControl.setPreparedness( 30 );
            model.optimize();
            if( jobControl != null )
                jobControl.setPreparedness( 80 );
            tdc.getColumnModel().addColumn(OPTIMAL_VALUES, Double.class);
            for( String name : model.getReactionNames() )
            {
                Object[] value = {model.getOptimValue(name)};
                TableDataCollectionUtils.addRow(tdc, name, value);
            }
            Object[] value = {model.getValueObjFunc()};
            TableDataCollectionUtils.addRow(tdc, VALUE_FUNCTION, value);
            return tdc;
        }
        return null;
    }

    public static TableDataCollection getFbcResultToPut(DataCollection<?> origin, String name, Diagram diagram, TableDataCollection tdc)
    {
        if( origin == null || tdc == null )
            return null;

        TableDataCollection st = TableDataCollectionUtils.createTableDataCollection( origin, name );

        tdc.columns().forEach( st.getColumnModel()::addColumn );

        VariableResolver resolver = diagram.getRole() instanceof EModel ? diagram.getRole( EModel.class ).getVariableResolver( 2 ) : null;
        Function<String, String> f = resolver == null ? s -> null : resolver::resolveVariable;

        for( RowDataElement rde : tdc )
        {
            if( rde.getName().equals( VALUE_FUNCTION ) )
            {
                st.getInfo().getProperties().setProperty( VALUE_FUNCTION, rde.getValueAsString( OPTIMAL_VALUES ) );
            }
            else
            {
                String title = f.apply( rde.getName() );
                TableDataCollectionUtils.addRow( st, title != null ? title : rde.getName(), rde.getValues() );
            }
        }

        return st;
    }
}
