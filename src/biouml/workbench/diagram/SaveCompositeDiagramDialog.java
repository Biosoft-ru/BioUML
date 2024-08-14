package biouml.workbench.diagram;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import one.util.streamex.EntryStream;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class SaveCompositeDiagramDialog extends OkCancelDialog
{
    protected List<JCheckBox> checkBoxes;
    protected List<JLabel> labels;

    protected JButton selectAllButton;

    public SaveCompositeDiagramDialog(List<String> changedSubdiagramNames)
    {
        super(Application.getApplicationFrame(), "Save dialog");
        init(changedSubdiagramNames);
    }

    protected void init(List<String> changedSubdiagramNames)
    {
        setResizable(false);

        checkBoxes = new ArrayList<>();
        labels = new ArrayList<>();

        selectAllButton = new JButton("Select all");
        selectAllButton.setDefaultCapable(true);
        selectAllButton.addActionListener(e -> {
            for( JCheckBox checkBox : checkBoxes )
                checkBox.setSelected(true);
        });
        buttonPanel.add(selectAllButton, 0);

        paint(changedSubdiagramNames);
    }

    protected void paint(List<String> changedSubdiagramNames)
    {
        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        String message = "<html>The following diagrams have been changed.<br> What changes do you want to save?</html>";
        contentPane.add(new JLabel(message), new GridBagConstraints(0, 0, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 10, 0), 0, 0));

        for( int i = 0; i < changedSubdiagramNames.size(); ++i )
        {
            JCheckBox checkBox = new JCheckBox();
            JLabel label = new JLabel(changedSubdiagramNames.get(i));

            contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

            contentPane.add(checkBox, new GridBagConstraints(0, i + 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
            contentPane.add(label, new GridBagConstraints(1, i + 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(5, 5, 0, 0), 0, 0));

            checkBox.setSelected(true);
            checkBoxes.add(checkBox);
            labels.add(label);
        }

        setContent(contentPane);
    }

    public List<String> getSubdiagramNamesToBeSaved()
    {
        return EntryStream.zip(checkBoxes, labels).filterKeys( JCheckBox::isSelected ).values().map( JLabel::getText ).toList();
    }
}
