package biouml.plugins.microarray;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

public class ActionTemp extends AbstractAction
{

    public ActionTemp(String name)
    {
        super(name);
    }

    public ActionTemp(String name, Icon icon)
    {
        super(name, icon);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
    }
}
