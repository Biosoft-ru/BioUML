package biouml.plugins.fbc;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

@SuppressWarnings ( "serial" )
public class OptionsDialog extends OkCancelDialog
{
    protected JComboBox<String> fbcType = new JComboBox<>();
    protected JComboBox<String> fbcSolver = new JComboBox<>();
    public String type;
    FbcModelCreator creator = new ApacheModelCreator();

    public OptionsDialog()
    {
        super(Application.getApplicationFrame(), "Type function");

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 30, 10, 30));

        contentPane.add(new JLabel("Type:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(fbcType, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(
                5, 5, 0, 0), 0, 0));

        setContent(contentPane);
        for( String type : FbcConstant.getAvailableFunctionTypes() )
        {
            fbcType.addItem( type );
        }



        contentPane.add(new JLabel("Solver:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(fbcSolver, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(5, 5, 0, 0), 0, 0));

        setContent(contentPane);
        for( String solver : FbcConstant.getAvailableSolverNames() )
        {
            fbcSolver.addItem( solver );
        }
    }
    @Override
    protected void okPressed()
    {
        try
        {
            type = fbcType.getSelectedItem().toString();
            String solverType = fbcSolver.getSelectedItem().toString();
            creator = FbcConstant.getSolverByType( solverType );
            super.okPressed();
        }
        catch( Throwable t )
        {

        }
    }
}
