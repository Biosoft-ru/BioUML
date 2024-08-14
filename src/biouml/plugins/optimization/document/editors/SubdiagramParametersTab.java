package biouml.plugins.optimization.document.editors;

import java.awt.BorderLayout;
import java.util.Iterator;

import javax.swing.JTable;

import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.optimization.OptimizationParameters;

import com.developmentontheedge.beans.swing.TabularPropertyInspector;

import ru.biosoft.gui.ViewPartSupport;

public class SubdiagramParametersTab extends ViewPartSupport
{
    private final EModel emodel;
    private final TabularPropertyInspector editor;

    public SubdiagramParametersTab(EModel emodel, Iterator it, OptimizationParameters optParams)
    {
        this.emodel = emodel;

        editor = new TabularPropertyInspector();
        editor.explore(it);
        editor.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        add(BorderLayout.CENTER, editor);
    }

    public EModel getEModel()
    {
        return this.emodel;
    }

    public Variable[] getSelectedParameters()
    {
        int[] rowNumbers = editor.getTable().getSelectedRows();
        Variable[] selectedParams = new Variable[rowNumbers.length];
        for( int i = 0; i < rowNumbers.length; i++ )
        {
            selectedParams[i] = (Variable)editor.getModelForRow(rowNumbers[i]);
        }
        return selectedParams;
    }

    public boolean isChanged()
    {
        return false;
    }
}
