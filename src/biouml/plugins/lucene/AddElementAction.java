package biouml.plugins.lucene;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

@SuppressWarnings ( "serial" )
public class AddElementAction extends AbstractAction
{
    protected static final MessageBundle messageBundle = new MessageBundle();
    
    public static final String KEY = "Add element";
    public static final String VIEW_PART = "luceneViewPart";
    
    public AddElementAction ( )
    {
        super ( KEY );
    }

    @Override
    public void actionPerformed ( ActionEvent arg0 )
    {
        LuceneSearchViewPart viewPart = ( LuceneSearchViewPart ) getValue ( VIEW_PART );
        viewPart.addElement ( );
        //putValue ( VIEW_PART, null );
    }
    
}
