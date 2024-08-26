package biouml.plugins.fbc;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.TabularPropertiesEditor;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.fbc.analysis.FbcAnalysis;
import biouml.plugins.fbc.table.FbcBuilderDataTableAnalysis;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;

@SuppressWarnings ( "serial" )
public class FbcReactionsEditor extends TabularPropertiesEditor implements PropertyChangeListener
{
    protected Action[] actions;
    protected Action showTable = new ShowTable();
    protected Action showOptimalValues = new ShowOptimalValues();
    protected Action editOptions = new EditOptions();
    protected Action saveTable = new SaveTable();
    TableDataCollection tableDC;
    TableDataCollection activeTable;
    Diagram diagram;
    String typeObjectiveFunc = FbcConstant.MAX;
    FbcModelCreator creator = new ApacheModelCreator();
    OptionsDialog dialog = new OptionsDialog();

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof biouml.model.Diagram && ( (biouml.model.Diagram)model ).getRole() instanceof EModel;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        try
        {
            this.diagram = (Diagram)model;
            FbcBuilderDataTableAnalysis analysis = new FbcBuilderDataTableAnalysis(null, null);
            tableDC = analysis.getFbcData(this.diagram);
            tableDC.addPropertyChangeListener(this);
            explore(tableDC.iterator());
            activeTable = tableDC;
            getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        }
        catch( Exception e )
        {
            explore( (Iterator<?>)null );
        }
    }

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( actions == null )
        {
            actionManager.addAction(ShowTable.KEY, showTable);
            actionManager.addAction(ShowOptimalValues.KEY, showOptimalValues);
            actionManager.addAction(EditOptions.KEY, editOptions);
            actionManager.addAction(SaveTable.KEY, saveTable);

            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);

            initializer.initAction(showTable, ShowTable.KEY);
            initializer.initAction(showOptimalValues, ShowOptimalValues.KEY);
            initializer.initAction(editOptions, EditOptions.KEY);
            initializer.initAction(saveTable, SaveTable.KEY);

            actions = new Action[] {showTable, showOptimalValues, editOptions, saveTable};
        }

        return actions;
    }

    class ShowTable extends AbstractAction
    {
        public static final String KEY = "Table";

        public ShowTable()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            explore(tableDC.iterator());
            activeTable = tableDC;
        }
    }

    class ShowOptimalValues extends AbstractAction
    {
        public static final String KEY = "Optimal Values";
        public ShowOptimalValues()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            TableDataCollection tdc = FbcAnalysis.getFbcResult(diagram, tableDC, typeObjectiveFunc, creator, null);
            if(tdc == null)
                return;
            explore(tdc.iterator());
            activeTable = tdc;
        }
    }
    class EditOptions extends AbstractAction
    {
        public static final String KEY = "Options";
        public EditOptions()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if( dialog.doModal() )
            {
                typeObjectiveFunc = dialog.type;
                creator = dialog.creator;
            }
        }
    }

    class SaveTable extends AbstractAction
    {
        public static final String KEY = "Save table";
        public SaveTable()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            SavePathDialog dialog = new SavePathDialog();
            if( dialog.doModal() )
            {
                try
                {
                    DataCollection<DataElement> origin = dialog.resultPath.getParentCollection();
                    String name = dialog.resultPath.getName();

                    origin.remove(name);
                    TableDataCollection toPut;

                    toPut = FbcAnalysis.getFbcResultToPut(origin, name, diagram, activeTable);
                    origin.put(toPut);
                }
                catch( Exception e1 )
                {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        FbcDiagramUpdater.update(diagram, evt);
    }
}
