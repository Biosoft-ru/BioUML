package ru.biosoft.table.document.editors;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import ru.biosoft.plugins.javascript.JavaScriptUtils;
import ru.biosoft.table.MessageBundle;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

@SuppressWarnings ( "serial" )
public class ColumnsFilterPanel extends JPanel
{
    public static final String ID_ATTR = "ID";
    private JList<String> columnsList;
    private JTextArea filterString;
    protected ResourceBundle resources;
    protected JScrollPane selectorsScrollPane;
    public ColumnsFilterPanel()
    {
        resources = ResourceBundle.getBundle(MessageBundle.class.getName());
        createFilterPanel(this);
    }

    private void createFilterPanel(JPanel panel)
    {
        JPanel filterPanel = new JPanel();

        filterPanel.setLayout(new GridBagLayout());

        JLabel columnsLabel = new JLabel(resources.getString("FILTER_COLUMNS"));
        filterPanel.add(columnsLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(5, 10, 0, 0), 0, 0));

        columnsList = new JList<>(new DefaultListModel<String>());
        columnsList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(final MouseEvent e)
            {
                if( e.getClickCount() > 1 )
                {
                    String cName = columnsList.getSelectedValue();
                    if( cName != null )
                    {
                        filterString.insert(cName, filterString.getCaretPosition());
                        filterString.requestFocus();
                    }
                }
            }
        });
        columnsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        columnsList.setVisibleRowCount(6);
        JScrollPane columnsListScroll = new JScrollPane(columnsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        filterPanel.add(columnsListScroll, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));

        JLabel exampleLabel = new JLabel();
        exampleLabel
                .setText("<html>Non-word characters in column names<br/>are replaced with underscore character,<br/>expressions joined with operators<br/>&&(and) and ||(or)<br/><br/>Example:<br/> Score > 0.5 && Group_number == 1</html>");
        exampleLabel.setVerticalAlignment(JLabel.CENTER);
        filterPanel.add(exampleLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(5, 10, 5, 0), 0, 0));

        JLabel filterExpressionLabel = new JLabel("Expression:");
        filterExpressionLabel.setHorizontalAlignment(JLabel.LEFT);
        filterExpressionLabel.setVerticalAlignment(JLabel.CENTER);
        filterPanel.add(filterExpressionLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));

        filterString = new JTextArea();
        filterPanel.add(filterString, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));

        selectorsScrollPane = new JScrollPane(filterPanel);
        selectorsScrollPane.setBorder(null);

        panel.setLayout(new BorderLayout());
        panel.add(selectorsScrollPane);
    }

    public void initPanel(TableDataCollection dc)
    {
        filterString.setText("");
        DefaultListModel<String> columnsInfoList = (DefaultListModel<String>)columnsList.getModel();
        columnsInfoList.removeAllElements();
        columnsInfoList.addElement(JavaScriptUtils.getValidName(ID_ATTR));
        for( TableColumn column : dc.getColumnModel() )
        {
            columnsInfoList.addElement(JavaScriptUtils.getValidName(column.getName()));
        }
        selectorsScrollPane.addComponentListener(new ComponentListener()
        {
            @Override
            public void componentResized(ComponentEvent evt)
            {
                int rowHeight = columnsList.getFixedCellHeight();
                if( rowHeight == -1 )
                {
                    rowHeight = (int)Math.ceil(columnsList.getPreferredScrollableViewportSize().getHeight()
                            / columnsList.getVisibleRowCount());
                    columnsList.setFixedCellHeight(rowHeight);
                }
                int numRows = selectorsScrollPane.getViewport().getHeight() / rowHeight - 7;
                if( numRows < 6 )
                    numRows = 6;
                if( numRows > ((DefaultListModel<String>)columnsList.getModel()).size() )
                    numRows = ((DefaultListModel<String>)columnsList.getModel()).size();

                columnsList.setVisibleRowCount(numRows);
                columnsList.revalidate();
            }
            @Override
            public void componentMoved(ComponentEvent evt)
            {
            }
            @Override
            public void componentShown(ComponentEvent evt)
            {
            }
            @Override
            public void componentHidden(ComponentEvent evt)
            {
            }
        });
    }

    public void setText(String text)
    {
        filterString.setText(text);
    }

    public String getText()
    {
        return filterString.getText();
    }
}
