package ru.biosoft.plugins.javascript.document;

import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import ru.biosoft.util.TextUtil2;

public class WatchTable extends JTable
{
    /**
     * Creates a new Evaluator.
     */
    public WatchTable()
    {
        super();
    }

    public void setTableModel(MyTableModel tableModel)
    {
        super.setModel(tableModel);
    }

    /**
     * Table model class for watched expressions.
     */
    public static class MyTableModel extends AbstractTableModel
    {
        /**
         * The debugger GUI.
         */
        private final JSPanel jsPanel;

        /**
         * Vector of watched expressions.
         */
        private final Vector<String> expressions;

        /**
         * Vector of values from evaluated from {@link #expressions}.
         */
        private final Vector<String> values;

        /**
         * Creates a new MyTableModel.
         */
        public MyTableModel(JSPanel jsPanel)
        {
            this.jsPanel = jsPanel;
            expressions = new Vector<>();
            values = new Vector<>();
            expressions.addElement("");
            values.addElement("");
        }

        /**
         * Returns the number of columns in the table (2).
         */
        @Override
        public int getColumnCount()
        {
            return 2;
        }

        /**
         * Returns the number of rows in the table.
         */
        @Override
        public int getRowCount()
        {
            return expressions.size();
        }

        /**
         * Returns the name of the given column.
         */
        @Override
        public String getColumnName(int column)
        {
            switch( column )
            {
                case 0:
                    return "Expression";
                case 1:
                    return "Value";
                default:
                    return null;
            }
        }

        /**
         * Returns whether the given cell is editable.
         */
        @Override
        public boolean isCellEditable(int row, int column)
        {
            return true;
        }

        /**
         * Returns the value in the given cell.
         */
        @Override
        public Object getValueAt(int row, int column)
        {
            switch( column )
            {
                case 0:
                    return expressions.elementAt(row);
                case 1:
                    return values.elementAt(row);
                default:
                    return "";
            }
        }

        /**
         * Sets the value in the given cell.
         */
        @Override
        public void setValueAt(Object value, int row, int column)
        {
            switch( column )
            {
                case 0:
                    String expr = value.toString();
                    expressions.setElementAt(expr, row);
                    String result = "";
                    if( expr.length() > 0 )
                    {
                        result = TextUtil2.nullToEmpty( jsPanel.getDim().eval(expr) );
                    }
                    values.setElementAt(result, row);
                    updateModel();
                    if( row + 1 == expressions.size() )
                    {
                        expressions.addElement("");
                        values.addElement("");
                        fireTableRowsInserted(row + 1, row + 1);
                    }
                    break;
                case 1:
                    // just reset column 2; ignore edits
                    fireTableDataChanged();
            }
        }

        /**
         * Re-evaluates the expressions in the table.
         */
        void updateModel()
        {
            for( int i = 0; i < expressions.size(); ++i )
            {
                Object value = expressions.elementAt(i);
                String expr = value.toString();
                String result;
                if( expr.length() > 0 )
                {
                    result = TextUtil2.nullToEmpty( jsPanel.getDim().eval(expr) );
                }
                else
                {
                    result = "";
                }
                result = result.replace('\n', ' ');
                values.setElementAt(result, i);
            }
            fireTableDataChanged();
        }
    }
}
