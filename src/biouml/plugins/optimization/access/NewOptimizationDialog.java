package biouml.plugins.optimization.access;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathField;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.gui.DocumentManager;
import biouml.model.Diagram;
import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class NewOptimizationDialog extends OkCancelDialog
{
    protected static final Logger log = Logger.getLogger(NewOptimizationDialog.class.getName());

    protected DataCollection<?> targetDC;
    protected OptimizationMethod<?> optMethod;
    protected DataElementPathField optimizationPath = null;
    protected DataElementPathField diagramPath = null;

    public NewOptimizationDialog(DataCollection<?> targetDC)
    {
        this(targetDC, null);
    }

    public NewOptimizationDialog(DataCollection<?> targetDC, OptimizationMethod<?> optMethod)
    {
        super(Application.getApplicationFrame(), "New optimization");

        this.targetDC = targetDC;
        this.optMethod = optMethod;

        setResizable(false);

        JPanel contentPane = new JPanel(new GridBagLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        contentPane.add(new JLabel("Name: "), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        DataElementPath defaultOptPath = DataElementPath.create(targetDC, "new_optimization");
        optimizationPath = new DataElementPathField("optimizationPath", Optimization.class, "(click to enter a path to new optimization document)", defaultOptPath, false);
        contentPane.add(optimizationPath, new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));

        contentPane.add(new JLabel("Diagram: "), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        DataElementPath defaultPath = DataElementPath.create("databases", "");
        diagramPath = new DataElementPathField("experimentPath", Diagram.class, "(click to select a diagram)", defaultPath, true);
        contentPane.add(diagramPath, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 0, 0), 0, 0));
        setContent(contentPane);
    }

    @Override
    protected void okPressed()
    {
        DataElementPath path = optimizationPath.getPath();
        String name = path.getName();
        if( !name.isEmpty() )
        {
            DataElementPath dPath = diagramPath.getPath();
            DataElement diagram = dPath.optDataElement();
            if( ( diagram == null ) || ! ( diagram instanceof Diagram ) || ( ( (Diagram)diagram ).getRole() == null ) )
            {
                String message = MessageFormat.format(MessageBundle.getMessage("WARN_INCORRECT_DIAGRAM"), new Object[] {dPath.getName()});
                JOptionPane.showMessageDialog(Application.getApplicationFrame(), message);
                return;
            }
            try
            {
                Optimization opt = Optimization.createOptimization(name, path.optParentCollection(), (Diagram)diagram);
                if( optMethod != null )
                    opt.setOptimizationMethod(optMethod);

                path.save(opt);

                DocumentManager.getDocumentManager().openDocument(Optimization.class, opt);
                super.okPressed();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, MessageBundle.getMessage("ERROR_OPTIMIZATION_CREATION"), t);
            }
        }
    }
}
