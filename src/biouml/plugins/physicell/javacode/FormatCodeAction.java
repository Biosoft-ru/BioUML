package biouml.plugins.physicell.javacode;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import java.util.logging.Logger;

@SuppressWarnings ( "serial" )
public class FormatCodeAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(FormatCodeAction.class.getName());

    public static final String KEY = "format code";
    public static final String DOCUMENT_ELEMENT = "document";

    public FormatCodeAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JavaDocument javaDocument = (JavaDocument)getValue(DOCUMENT_ELEMENT);
        String code = javaDocument.getJavaPanel().getText(false);
        code = new JavaCodeFormatter().format( code );
        javaDocument.getJavaPanel().textArea.setText( code );
    }
}
