package ru.biosoft.math;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.editor.ViewPane;
import ru.biosoft.math.model.AbstractParser;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Parser;
import ru.biosoft.math.model.VariableResolver;
import ru.biosoft.math.view.FormulaViewBuilder;
import ru.biosoft.math.xml.MathMLFormatter;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.PanelInfo;
import com.developmentontheedge.application.PanelManager;

/**
 * General purpose expression editor.
 *
 * @see Expression
 * @see ExpressionEditorDialog
 */
public class ExpressionEditorPane extends PanelManager
{
    //available panel names
    public static final String TEXT_PANEL_NAME = "Text";
    public static final String MATHML_PANEL_NAME = "MathML";

    protected FormulaViewBuilder viewBuilder = new FormulaViewBuilder();

    protected ViewPane viewPane = new ViewPane();
    protected JTextPane textPane = new JTextPane();
    protected JTextPane mathMLPane = new JTextPane();
    protected JTextPane logPane = new JTextPane();

    protected JButton validateButton = new JButton("Validate");
    protected JButton dumpButton = new JButton("Dump");

    protected JButton okButton;

    protected AbstractParser linearParser = new ru.biosoft.math.parser.Parser();
    protected AbstractParser mathMLParser = new ru.biosoft.math.xml.MathMLParser();

    public static final LinearFormatter linearFormatter = new LinearFormatter();
    public static final MathMLFormatter mathMLFormatter = new MathMLFormatter();

    public ExpressionEditorPane(JButton okButton, String[] panelNames, boolean dumpAvailable)
    {
        this.okButton = okButton;

        // editor pane components
        addPanel(new PanelInfo("view", viewPane, true, null), null, 0);

        JTabbedPane tabs = new JTabbedPane();
        for( String panelName : panelNames )
        {
            if( panelName.equals(TEXT_PANEL_NAME) )
            {
                tabs.add(TEXT_PANEL_NAME, new JScrollPane(textPane));
            }
            else if( panelName.equals(MATHML_PANEL_NAME) )
            {
                tabs.add(MATHML_PANEL_NAME, new JScrollPane(mathMLPane));
            }
        }

        JPanel buttonPane = new JPanel(new java.awt.FlowLayout());
        buttonPane.add(validateButton);
        if( dumpAvailable )
        {
            buttonPane.add(dumpButton);
        }

        JPanel editorPane = new JPanel(new BorderLayout());
        editorPane.add(tabs, BorderLayout.CENTER);
        editorPane.add(buttonPane, BorderLayout.SOUTH);
        addPanel(new PanelInfo("editors", editorPane, true, null), "view", PanelInfo.BOTTOM, 120);

        // log pane
        JScrollPane log = new JScrollPane(logPane);
        log.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2), "Messages:"));
        addPanel(new PanelInfo("log", log, true, null), "editors", PanelInfo.BOTTOM, 150);

        mathMLPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        logPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        logPane.setEditable(false);

        setPreferredSize(new Dimension(400, 400));

        // event listeners
        tabs.addChangeListener(e -> updateTab());

        textPane.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void changedUpdate(DocumentEvent e)
            {
                processDocumentEvent(textPane);
            }
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                processDocumentEvent(textPane);
            }
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                processDocumentEvent(textPane);
            }
        });

        mathMLPane.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void changedUpdate(DocumentEvent e)
            {
                processDocumentEvent(mathMLPane);
            }
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                processDocumentEvent(mathMLPane);
            }
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                processDocumentEvent(mathMLPane);
            }
        });

        validateButton.addActionListener(e -> validateExpression());

        dumpButton.addActionListener(e -> dump());
    }

    public void setVariableResolver(VariableResolver variableResolver)
    {
        linearParser.setVariableResolver(variableResolver);
        mathMLParser.setVariableResolver(variableResolver);
    }

    protected Expression expression;
    public Expression getExpression()
    {
        return expression;
    }
    public void setExpression(Expression expression)
    {
        this.expression = expression;
        textPane.setText(expression.getText());

        // we suggest that expression is valid
        validateButton.setEnabled(false);

        // because nothing is changed, ok button is also disabled
        if( okButton != null )
            okButton.setEnabled(false);
    }

    public void addVariableToFormula(String varName)
    {
        int pos = textPane.getCaretPosition();
        if( pos >= 0 )
        {
            String text = textPane.getText();
            textPane.setText(text.substring(0, pos) + varName + text.substring(pos));
            processDocumentEvent(textPane);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Event processing issues
    //

    /**
     * Indicates whether edited expression is valid.
     * Possible values: Parser.STATUS_OK, Parser.STATUS_WARNING,
     * Parser.STATUS_ERRROR, Parser.STATUS_FATAL_ERROR
     * or combination their combination (excluding Parser.STATUS_OK).
     */
    protected int status;

    /** The source of DocumentEvent, text or mathML JTextPane. */
    protected JTextPane source;

    protected void processDocumentEvent(JTextPane source)
    {
        if( expression == null )
            return;

        this.source = source;

        Parser parser = source == textPane ? linearParser : mathMLParser;
        String text = source == textPane ? textPane.getText() : mathMLPane.getText();

        parser.setDeclareUndefinedVariables(false);
        parser.setContext(expression.getParserContext());
        status = parser.parse(text);

        if( ( status & Parser.STATUS_FATAL_ERROR ) == 0 )
        {
            expression.setText(text);
            expression.setAstStart(parser.getStartNode());
            updateView();
        }

        validateButton.setEnabled(true);
        okButton.setEnabled(false);
    }

    protected void updateView()
    {
        if( isVisible() )
        {
            CompositeView expressionView = viewBuilder.createView(expression.getAstStart(), ApplicationUtils.getGraphics());
            int x = 0;
            if( viewPane.getWidth() > expressionView.getBounds().getWidth() )
            {
                x = (int) ( viewPane.getWidth() - expressionView.getBounds().getWidth() ) / 2;
            }
            int y = 0;
            if( viewPane.getHeight() > expressionView.getBounds().getHeight() )
            {
                y = (int) ( viewPane.getHeight() - expressionView.getBounds().getHeight() ) / 2;
            }
            viewPane.setView(expressionView, new java.awt.Point(x, y));
        }
    }

    protected void updateTab()
    {
        if( source == textPane )
        {
            if( ( status & Parser.STATUS_FATAL_ERROR ) != 0 )
            {
                logPane.setForeground(Color.red.darker());
                logPane.setText("Can not generate MathML, reason: text expression is incorrect.\n"
                        + "Press 'Validate' button to display parsing error messages\n" + "or you can edit current MathML expression.");
            }
            else
            {
                String math = ( mathMLFormatter.format(expression.getAstStart()) )[1];
                mathMLPane.setText(math);
                source = mathMLPane;
            }
        }

        // MathML
        else
        {
            if( ( status & Parser.STATUS_FATAL_ERROR ) != 0 )
            {
                logPane.setForeground(Color.red.darker());
                logPane.setText("Can not generate text expression, reason: MathML expression is incorrect.\n"
                        + "Press 'Validate' button to display parsing error messages\n" + "or you can edit current text expression.");
            }
            else
            {
                String text = ( linearFormatter.format(expression.getAstStart()) )[1];
                textPane.setText(text);
                source = textPane;
            }
        }
    }

    protected void validateExpression()
    {
        StringBuffer msg = new StringBuffer();
        List<String> errorList = null;

        if( source == textPane )
            errorList = linearParser.getMessages();
        else
            errorList = mathMLParser.getMessages();

        if( status == Parser.STATUS_OK )
        {
            logPane.setForeground(Color.black);
            msg.append("Expression parsed successfully.");
        }
        else if( errorList != null )
        {
            if (status > Parser.STATUS_WARNING)
                logPane.setForeground(Color.red.darker());
            else
                logPane.setForeground(Color.black);
            msg.append( String.join( "\n", errorList ) );
        }

        logPane.setText(msg.toString());

        if( status < Parser.STATUS_ERROR && okButton != null )
            okButton.setEnabled(true);
        else
            okButton.setEnabled(false);
    }

    protected void dump()
    {
        StringBuffer msg = new StringBuffer();

        if( expression.getAstStart() == null || ( status & Parser.STATUS_FATAL_ERROR ) != 0 )
            msg.append("Dump is unavailable.");
        else
        {
            msg.append("AST tree dump: \n");
            expression.getAstStart().dump(msg, "  ");
        }

        logPane.setForeground(Color.blue.darker());
        logPane.setText(msg.toString());
    }

}
