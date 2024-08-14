package biouml.standard.diagram;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import biouml.model.DiagramElement;
import biouml.model.Node;

@SuppressWarnings ( "serial" )
public class DiagramElementSelector extends JPanel
{
    private JRadioButton button;
    private JTextField field;

    public DiagramElementSelector(String name)
    {
        super(new BorderLayout(5, 5));

        button = new JRadioButton(name);
        button.setPreferredSize(new Dimension(65, 20));

        field = new JTextField(30);
        field.setEditable(false);

        add(button, BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
    }

    protected DiagramElement diagramElement;
    public DiagramElement getDiagramElement()
    {
        return diagramElement;
    }
    public boolean setDiagramElement(DiagramElement diagramElement)
    {
        if( isSuitable(diagramElement) )
        {
            this.diagramElement = diagramElement;
            field.setText(diagramElement.getCompleteNameInDiagram());

            return true;
        }

        return false;
    }

    public boolean isSuitable(DiagramElement de)
    {
        return de instanceof Node;
    }

    public JRadioButton getRadioButton()
    {
        return button;
    }

    public boolean isSelected()
    {
        return button.isSelected();
    }
}


