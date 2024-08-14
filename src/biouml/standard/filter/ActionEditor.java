package biouml.standard.filter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;

import ru.biosoft.graphics.Brush;

import com.developmentontheedge.beans.editors.ColorComboBox;
import com.developmentontheedge.beans.editors.CustomEditorSupport;

public class ActionEditor extends CustomEditorSupport
{
    private static ActionRenderer renderer;
    protected ActionComboBox editor;

    private static Font FONT = new Font( "Arial", Font.PLAIN, 11 );
    private static int REN_WIDTH = 30;

    @Override
    public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    {
        if( renderer == null )
            renderer = new ActionRenderer();

        renderer.setAction( (Action)getValue() );
        return renderer;
    }

    @Override
    public Component getCustomEditor()
    {
        if( editor == null )
        {
            editor = new ActionComboBox();
            editor.addColorPropertyChangeListener( new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    Color c = (Color)evt.getNewValue();

                    if( c == ActionComboBox.hide )
                        setValue( HideAction.instance);
                    else
                        setValue( new HighlightAction(new Brush(c))  );
                }
            } );
        }

        return editor;
    }

    @Override
    protected Object processValue()
    {
        Object value = editor.getSelectedItem();
        if( value instanceof Color )
            return new HighlightAction( new Brush((Color)value) );

        return HideAction.instance;
    }

    ///////////////////////////////////////////////////////////////////
    // ActionRenderer
    //

    @SuppressWarnings ( "serial" )
    public static class ActionRenderer extends JLabel
    {
        protected Action action;
        public Action getAction()
        {
            return action;
        }
        public void setAction(Action action)
        {
            this.action = action;
        }

        @Override
        public void paint(Graphics g)
        {
            if( action instanceof HideAction )
            {
                g.setFont(FONT);
                g.setColor(Color.black);
                g.drawString("hide", 9, 12);
            }
            else if( action instanceof HighlightAction )
            {
                Color color = (Color) ((HighlightAction)action).getBrush().getPaint();
                g.setColor(color);
                g.fillRect(9, 5, REN_WIDTH, 8);

                g.setColor(Color.black);
                g.drawRect(9, 5, REN_WIDTH, 8);

                String text = ColorComboBox.getText(color);

                if (text == null)
                    text = "custom";

                g.setFont(FONT);
                g.setColor(Color.black);
                g.drawString(text, 20+REN_WIDTH, 12);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // ActionEditor
    //

    @SuppressWarnings ( "serial" )
    public static class ActionComboBox extends ColorComboBox
    {
        static ColorComboBox.NamedColor hide = new ColorComboBox.NamedColor(Color.white, "hide", false);

        public ActionComboBox()
        {
            super(hide);
        }
    }
}
