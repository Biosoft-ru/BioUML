package ru.biosoft.gui.setupwizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

/**
 * System properties wizard
 * 
 * @author tolstyh
 */
public class SetupWizardDialog extends JDialog
{
    public static final String SETUP_WIZARD_PREFERENCES = "Setup wizard";
    public static final String SETUP_WIZARD_USAGE = "Show on startup";

    public static final int WINDOW_WIDTH = 700;
    public static final int WINDOW_HEIGHT = 650;
    public static final String BACK_BUTTON_TEXT = "Previous";
    public static final String NEXT_BUTTON_TEXT = "Next";
    public static final String FINISH_BUTTON_TEXT = "Finish";
    public static final String HELP_BUTTON_TEXT = "Help";
    public static final String USE_WIZARD_TEXT = "Show on startup";

    protected JButton backButton;
    protected JButton nextButton;
    protected JButton finishButton;

    protected Component parent;
    protected final WizardPageInfo[] pages;
    protected int currentPage;
    protected final JTabbedPane tabbedPane;
    protected JLabel info;
    protected JLabel description;
    protected Preferences setupPreferences;
    protected Icon icon;

    public SetupWizardDialog(Component parent, WizardPageInfo[] pageInfos, String title, Preferences setupPreferences, Icon icon)
    {
        super(JOptionPane.getFrameForComponent(parent), title, true);
        this.parent = parent;
        this.pages = pageInfos;
        this.setupPreferences = setupPreferences;
        this.icon = icon;

        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        for( WizardPageInfo pageInfo : pages )
        {
            tabbedPane.add(pageInfo.getName(), pageInfo.getPage().getPanel());
        }
        tabbedPane.addChangeListener(event -> {
            currentPage = tabbedPane.getSelectedIndex();
            pageChanged(-1);
        });

        currentPage = 0;

        contentPane.add(getTopPanel(), BorderLayout.NORTH);
        contentPane.add(tabbedPane, BorderLayout.CENTER);
        contentPane.add(getBottomPanel(), BorderLayout.SOUTH);

        pageChanged( -1);

        setModal(false);
    }

    protected JPanel getBottomPanel()
    {
        JPanel buttonPanel = new JPanel(new GridBagLayout());

        JCheckBox useWizardCheckBox = new JCheckBox();
        final DynamicProperty usage = setupPreferences.getProperty(SETUP_WIZARD_USAGE);
        useWizardCheckBox.setSelected((Boolean)usage.getValue());
        useWizardCheckBox.addActionListener(event -> usage.setValue( ! ( (Boolean)usage.getValue() )));
        JLabel useWizardText = new JLabel(USE_WIZARD_TEXT);


        buttonPanel.add(useWizardCheckBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        buttonPanel.add(useWizardText, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        buttonPanel.add(new JLabel(""), new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        backButton = new JButton(BACK_BUTTON_TEXT);
        backButton.setEnabled(false);
        nextButton = new JButton(NEXT_BUTTON_TEXT);
        nextButton.setDefaultCapable(true);
        finishButton = new JButton(FINISH_BUTTON_TEXT);

        nextButton.setPreferredSize(backButton.getPreferredSize());
        finishButton.setPreferredSize(backButton.getPreferredSize());

        nextButton.addActionListener(event -> nextButtonAction());

        backButton.addActionListener(event -> backButtonAction());

        finishButton.addActionListener(event -> finishButtonAction());

        buttonPanel.add(backButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        buttonPanel.add(nextButton, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        buttonPanel.add(finishButton, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));

        buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        return buttonPanel;
    }

    protected void nextButtonAction()
    {
        int oldPage = currentPage;
        currentPage++;
        tabbedPane.setSelectedIndex(currentPage);
        pageChanged(oldPage);
    }

    protected void finishButtonAction()
    {
        boolean close = true;
        for( WizardPageInfo page : pages )
        {
            try
            {
                page.getPage().saveSettings();
                pages[currentPage].getPage().fireClosePage();
            }
            catch( IncorrectDataException e )
            {
                JOptionPane.showMessageDialog(this, e.getMessage());
                close = false;
            }
        }
        if( close )
        {
            hide();
            dispose();
        }
    }

    protected void backButtonAction()
    {
        int oldPage = currentPage;
        currentPage--;
        tabbedPane.setSelectedIndex(currentPage);
        pageChanged(oldPage);
    }

    protected JPanel getTopPanel()
    {
        JPanel topPanel = new JPanel(new GridBagLayout());

        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(icon);

        topPanel.add(iconLabel, new GridBagConstraints(0, 0, 0, 2, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                10, 10, 10, 10), 0, 0));

        info = new JLabel();
        info.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        description = new JLabel();

        topPanel.add(info, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10,
                10, 10, 10), 0, 0));

        topPanel.add(description, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(10, 10, 10, 10), 0, 0));

        return topPanel;
    }

    protected void pageChanged(int oldPage)
    {
        if( currentPage == 0 )
        {
            backButton.setEnabled(false);
        }
        else
        {
            backButton.setEnabled(true);
        }

        if( currentPage == pages.length - 1 )
        {
            nextButton.setEnabled(false);
        }
        else
        {
            nextButton.setEnabled(true);
        }

        if( oldPage > -1 )
        {
            pages[oldPage].getPage().fireClosePage();
        }
        pages[currentPage].getPage().fireOpenPage();

        info.setText("Step " + ( currentPage + 1 ) + " of " + pages.length);
        description.setText(pages[currentPage].getDescription());
    }

    public void showWizard()
    {
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }
}
