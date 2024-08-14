package biouml.plugins.fbc.server;

import biouml.model.Diagram;
import biouml.plugins.fbc.FbcModel;
import biouml.plugins.fbc.FbcModelCreator;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class FbcJobControl extends AbstractJobControl
{
    private static final String OPTIMAL_VALUES = "Optimal Values";
    private static final String VALUE_FUNCTION = "Value Function";

    private FbcModel model;
    private TableDataCollection resultTable;
    FbcModelCreator creator;
    Diagram diagram;
    TableDataCollection fbcDataTable;
    String typeObjectiveFunction;

    public FbcJobControl(FbcModel model)
    {
        super( null );
        this.model = model;
    }

    public FbcJobControl(FbcModelCreator creator, Diagram diagram, TableDataCollection table, String type)
    {
        super( null );
        this.creator = creator;
        this.diagram = diagram;
        this.fbcDataTable = table;
        this.typeObjectiveFunction = type;
    }

    @Override
    protected void doRun() throws JobControlException
    {
        if( model == null )
            model = creator.createModel( diagram, fbcDataTable, typeObjectiveFunction );
        model.optimize();
        resultTable = new StandardTableDataCollection( null, "" );
        resultTable.getColumnModel().addColumn( OPTIMAL_VALUES, Double.class );
        for( String name : model.getReactionNames() )
        {
            Object[] value = {model.getOptimValue( name )};
            TableDataCollectionUtils.addRow( resultTable, name, value );
        }
        Object[] value = {model.getValueObjFunc()};
        TableDataCollectionUtils.addRow( resultTable, VALUE_FUNCTION, value );
    }

    public TableDataCollection getResult()
    {
        return resultTable;
    }

}
