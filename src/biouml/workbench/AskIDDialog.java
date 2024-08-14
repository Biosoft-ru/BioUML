
package biouml.workbench;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.dialog.OkCancelDialog;

public class AskIDDialog extends OkCancelDialog
{
    protected JTextField id;
    protected JPanel content;

    public AskIDDialog(JDialog dialog, String title, String idName)
    {
        super(dialog, title);
        init(idName);
    }

    public AskIDDialog(JFrame frame, String title, String idName)
    {
        super(frame, title);
        init(idName);
    }

    private void init(String idName)
    {
        content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        id = new JTextField(15);

        content.add(new JLabel(idName),
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.HORIZONTAL,
                                       new Insets(0, 0, 0, 0), 0, 0));
        content.add(id,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                                       GridBagConstraints.BOTH,
                                       new Insets(5, 5, 0, 0), 0, 0));

        id.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyReleased(KeyEvent e)
                {
                    String name = id.getText();
                    okButton.setEnabled(name != null && name.length() > 0);
                }
            });

        setContent(content);
        okButton.setEnabled(false);
    }
    
    public void setDefaultID ( String ID )
    {
        id.setText ( ID );
    }

    public String getIDName()
    {
        return id.getText();
    }

}
