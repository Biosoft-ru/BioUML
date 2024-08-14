package biouml.standard.diagram;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;

import ru.biosoft.gui.Document;
import ru.biosoft.gui.TabularPropertiesEditor;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SubDiagram;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionManager;

@SuppressWarnings ( "serial" )
public class CompositeReactionsEditor extends TabularPropertiesEditor implements PropertyChangeListener
{
    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof biouml.model.Diagram )
        {
            return ( (Diagram)model ).getType() instanceof CompositeDiagramType;
        }
        return false;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        try
        {
            TableDataCollection table = getRowModel();
            explore(table.iterator());
            getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        }
        catch( Exception e )
        {
            explore((Iterator<?>)null);
        }
    }

    protected TableDataCollection getRowModel() throws Exception
    {
        Diagram compositeDiagram = (Diagram)model;
        HashSet<HashSet<Node>> reactions = CompositeModelUtility.getConnectedReactions( compositeDiagram );
        StandardTableDataCollection reactionCollection = new StandardTableDataCollection(null, "");

        ColumnModel columns = reactionCollection.getColumnModel();
        compositeDiagram.stream( SubDiagram.class ).map( SubDiagram::getName )
                .forEach( name -> columns.addColumn( name, DataType.BooleanType.class ) );

        int columnCount = columns.getColumnCount();

        for( HashSet<Node> reactionSet : reactions )
        {
            String key = null;

            Boolean[] row = new Boolean[columnCount];

            for( int i = 0; i < columnCount; i++ )
            {
                row[i] = null;

                for( Node reaction : reactionSet )
                {
                    if( key == null )
                        key = reaction.getName();

                    if( Diagram.getDiagram(reaction).getName().equals(columns.getColumn(i).getName()) )
                    {
                        row[i] = false;
                        break;
                    }
                }
            }

            TableDataCollectionUtils.addRow(reactionCollection, key, row);
        }
        return reactionCollection;
    }
    ////////////////////////////////////////////////////////////////////////////
    // Update issues
    //

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    protected FindConnectedReactionsAction findConnectedReactionsAction;

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( findConnectedReactionsAction == null )
        {
            findConnectedReactionsAction = new FindConnectedReactionsAction();
            actionManager.addAction(FindConnectedReactionsAction.KEY, findConnectedReactionsAction);
            
            findConnectedReactionsAction.setEnabled(true);
        }

        return new Action[] {findConnectedReactionsAction};
    }

    public class FindConnectedReactionsAction extends AbstractAction
    {
        public static final String KEY = "Refresh parameters";

        public FindConnectedReactionsAction()
        {
            super(KEY);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
                explore(model, document);
        }
    }
}
