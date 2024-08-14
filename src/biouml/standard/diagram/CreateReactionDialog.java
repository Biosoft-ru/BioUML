package biouml.standard.diagram;

import java.awt.Dimension;

import javax.swing.JFrame;

import com.developmentontheedge.application.dialog.OkCancelDialog;

/**
 * Create new reaction dialog
 */
public class CreateReactionDialog extends OkCancelDialog
{
    ReactionPane reactionPane;

    public CreateReactionDialog(JFrame frame, ReactionPane reactionPane)
    {
        this( frame, reactionPane, "New reaction" );
    }

    public CreateReactionDialog(JFrame frame, ReactionPane reactionPane, String title)
    {
        super( frame, title, false );
        this.reactionPane = reactionPane;
        setAlwaysOnTop( true );
        setContent(reactionPane);
        pack();
        setPreferredSize( new Dimension( 800, 500 ) );
        setMinimumSize(new Dimension(0,500));
        setLocationRelativeTo(frame);
        setVisible(true);
    }
    
    @Override
    protected void okPressed()
    {
        if( reactionPane.okPressed() )
            super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        reactionPane.cancelPressed();
        super.cancelPressed();
    }
}