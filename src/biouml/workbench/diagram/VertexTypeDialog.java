package biouml.workbench.diagram;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JRadioButton;

import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.util.OkCancelDialog;
import biouml.model.Edge;

import com.developmentontheedge.application.Application;

public class VertexTypeDialog extends OkCancelDialog
{
    protected JRadioButton radio1, radio2, radio3;
    protected Edge edge;
    protected int point;
    protected ViewPane viewPane;

    public VertexTypeDialog(ViewPane viewPane, Edge edge, int point, int type)
    {
        super(Application.getApplicationFrame(), "Vertex type");

        this.viewPane = viewPane;
        this.edge = edge;
        this.point = point;

        JPanel mainPanel = new JPanel(new GridLayout(3, 1));
        radio1 = new JRadioButton("line");
        radio1.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setSelectedRadio(0);
            }
        });
        mainPanel.add(radio1);
        radio2 = new JRadioButton("quadric");
        radio2.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setSelectedRadio(1);
            }
        });
        mainPanel.add(radio2);
        radio3 = new JRadioButton("cubic");
        radio3.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setSelectedRadio(2);
            }
        });
        mainPanel.add(radio3);

        setSelectedRadio(type);
        setContent(mainPanel);
    }

    protected void setSelectedRadio(int type)
    {
        if( type == 0 )
        {
            radio1.setSelected(true);
            radio2.setSelected(false);
            radio3.setSelected(false);
        }
        else if( type == 1 )
        {
            radio1.setSelected(false);
            radio2.setSelected(true);
            radio3.setSelected(false);
        }
        else
        {
            radio1.setSelected(false);
            radio2.setSelected(false);
            radio3.setSelected(true);
        }
    }

    @Override
    protected void okPressed()
    {
        Path path = edge.getPath();
        int type = 0;
        if( radio2.isSelected() )
        {
            type = 1;
        }
        else if( radio3.isSelected() )
        {
            type = 2;
        }
        path.pointTypes[point] = type;

        DiagramDocument.updateDiagram(viewPane, edge);

        super.okPressed();
    }
}
