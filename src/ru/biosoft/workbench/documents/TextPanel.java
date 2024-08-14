package ru.biosoft.workbench.documents;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.StyledDocument;


public class TextPanel extends JPanel
{
    private FileTextPane textArea;
    private JScrollPane p;


    public TextPanel(String type, String text, StyledDocument document)
    {
        super(new BorderLayout());
        textArea = document == null ? new FileTextPane() : new FileTextPane(document);
        textArea.setContentType( type );
        p = new JScrollPane();
        p.setViewportView(textArea);
        add(p, BorderLayout.CENTER);
        updateText(text);
        textArea.select(0);
    }
    
    public TextPanel(String text, StyledDocument document)
    {
        super(new BorderLayout());
        textArea = document == null ? new FileTextPane() : new FileTextPane(document);
        p = new JScrollPane();
        p.setViewportView(textArea);
        add(p, BorderLayout.CENTER);
        updateText(text);
        textArea.select(0);
    }

    public String getText()
    {
        return textArea.getText();
    }

    public void updateText(String text)
    {
        if( !textArea.getText().equals(text) )
        {
            textArea.setText(text);
            int pos = 0;
            textArea.select(pos);
        }
    }
    
    /**
     * Add undo/redo manager
     */
    public void addUndoableEditListener(UndoableEditListener listener)
    {
        textArea.getDocument().addUndoableEditListener(listener);
    }

    /**
     * Remove undo/redo manager
     */
    public void removeUndoableEditListener(UndoableEditListener listener)
    {
        textArea.getDocument().removeUndoableEditListener(listener);
    }

}