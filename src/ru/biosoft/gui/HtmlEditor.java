package ru.biosoft.gui;

import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;

import com.developmentontheedge.beans.web.HtmlPane;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.SeparatorAction;

/**
 *
 * @pending undo/redo support.
 * The main problem here is synchronisation between different tabs,
 * when we get text from tab and set it in other, undo/redo stack
 * becomes broken.
 */
public class HtmlEditor extends EditorPartSupport
{
    protected JEditorPane htmlEditor;
    protected JEditorPane textEditor;
    protected HtmlPane    preview;
    protected JTabbedPane view;

    public HtmlEditor()
    {
        htmlEditor = new JEditorPane("text/html",  "");
        textEditor = new JEditorPane("text/plain", "");
        preview    = new HtmlPane();

        /*
        UndoableEditListener listener = new UndoableEditListener()
        {
            public void undoableEditHappened(UndoableEditEvent e)
            {
                String presentationName = e.getEdit().getPresentationName();
                TransactionEvent te = new TransactionEvent(e.getSource(), presentationName);

                fireStartTransaction(te);
                fireAddEdit(e.getEdit());
                fireCompleteTransaction();
            }
        };
        
        htmlEditor.getDocument().addUndoableEditListener(listener);
        textEditor.getDocument().addUndoableEditListener(listener);
        */

        textEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));

        view = new JTabbedPane(JTabbedPane.LEFT);
        view.addTab("Normal",   new JScrollPane(htmlEditor));
        view.addTab("Html",     new JScrollPane(textEditor));
        view.addTab("Preview",  new JScrollPane(preview.getEditor()));

        // synchronise content between editors
        view.addChangeListener(new ChangeListener()
        {
            protected int previous = 0;

            @Override
            public void stateChanged(ChangeEvent e)
            {
                int current = view.getSelectedIndex();

                if( current == 1  && previous == 0 )
                    textEditor.setText( htmlEditor.getText() );
                else if ( current == 0  && previous == 1 )
                    htmlEditor.setText( textEditor.getText() );
                else if ( current == 2  && previous == 0 )
                {
                    textEditor.setText( htmlEditor.getText() );
                    preview.setInitialText( htmlEditor.getText() );
                }
                else if ( current == 2  && previous == 1 )
                {
                    htmlEditor.setText( textEditor.getText() );
                    preview.setInitialText( textEditor.getText() );
                }

                previous = current;

                if( current == 0 )
                {
                    for(int i=0; i<12; i++)
                        actions[i].setEnabled(true);
                }
                else if( current == 1 )
                {
                    for(int i=0; i<12; i++)
                        actions[i].setEnabled(i>3);
                }
                else if( current == 2 )
                {
                    for(int i=0; i<12; i++)
                        actions[i].setEnabled(false);
                }

                preview.getBackwardAction().setEnabled(current == 2 && preview.isBackwardActionEnabled() );
                preview.getForwardAction().setEnabled (current == 2 && preview.isForwardActionEnabled() );
            }
        });

        initActions();
        view.setSelectedIndex(2);

        DocumentFocusListener dfl = new DocumentFocusListener();
        htmlEditor.addFocusListener(dfl);
        textEditor.addFocusListener(dfl);
        preview.getEditor().addFocusListener(dfl);
    }

    public class DocumentFocusListener extends FocusAdapter
    {
        @Override
        public void focusGained(FocusEvent e)
        {
            if( getDocument() != null )
                 DocumentManager.setActiveDocument(getDocument(), view);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Action issues
    //

    protected Action[] actions;
    @Override
    public Action[] getActions()
    {
        return actions;
    }


    protected void initActions()
    {
        actions = new Action[]
        {
            new DefaultEditorKit.CopyAction(),
            new DefaultEditorKit.CutAction(),
            new DefaultEditorKit.PasteAction(),

            new SeparatorAction(),
            new StyledEditorKit.BoldAction(),
            new StyledEditorKit.ItalicAction(),
            new StyledEditorKit.UnderlineAction(),

            new SeparatorAction(),
            new StyledEditorKit.AlignmentAction("left",   StyleConstants.ALIGN_LEFT),
            new StyledEditorKit.AlignmentAction("center", StyleConstants.ALIGN_CENTER),
            new StyledEditorKit.AlignmentAction("right",  StyleConstants.ALIGN_RIGHT),

            new SeparatorAction(),
            preview.getBackwardAction(),
            preview.getForwardAction(),
        };

        ActionInitializer initializer = new ActionInitializer(ru.biosoft.gui.resources.MessageBundle.class);
        for(int i=0; i<actions.length; i++)
        {
            if( !(actions[i] instanceof SeparatorAction) )
            {
                initializer.initAction(actions[i]);
                actions[i].setEnabled(true);
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    @Override
    public JComponent getView()
    {
        return view;
    }

    public String getText()
    {
        if( view.getSelectedIndex() == 0 )
            return htmlEditor.getText();
        else
            return textEditor.getText();
    }

    public void setText(String text)
    {
        int current = view.getSelectedIndex();
        if( current == 0 )
            htmlEditor.setText(text);
        else if( current == 1 )
            textEditor.setText(text);
        else if( current == 2 )
        {
            htmlEditor.setText(text);
            textEditor.setText(text);
            preview.setInitialText(text);
        }
    }
}
