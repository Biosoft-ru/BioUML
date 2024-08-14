package ru.biosoft.workbench.script;

import java.awt.BorderLayout;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import one.util.streamex.StreamEx;

import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.script.ScriptTypeRegistry.ScriptType;
import ru.biosoft.gui.EditorPartSupport;

import com.developmentontheedge.application.action.ApplicationAction;

@SuppressWarnings ( "serial" )
public class ScriptConsole extends EditorPartSupport
{
    public static final String PREPROCESSOR_JAVASCRIPT = "JavaScript";

    protected JComboBox<ScriptType> language;
    final EvalTextArea console = new EvalTextArea();
    
    public ScriptConsole()
    {
        setLayout(new BorderLayout());

        language = new JComboBox<>();
        StreamEx.ofValues( ScriptTypeRegistry.getScriptTypes() ).sorted().forEach( language::addItem );
        add(language, BorderLayout.NORTH);
        
        add(new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER);

        action = new ApplicationAction("Script console", "Script console");

        language.addActionListener(e -> console.setScriptType((ScriptType)language.getSelectedItem()));
        console.setScriptType((ScriptType)language.getSelectedItem());
    }
    
    public void eval(String code, String scriptLanguage)
    {
        language.setSelectedItem( ScriptTypeRegistry.getScriptTypes().get( scriptLanguage ) );
        console.append(code, null);
        console.returnPressed();
    }

    @Override
    public Object getModel()
    {
        return STATIC_VIEW;
    }
}
