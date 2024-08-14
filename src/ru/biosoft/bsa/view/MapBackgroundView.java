
package ru.biosoft.bsa.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.GraphicProperties;


public class MapBackgroundView extends BoxView
{
    private static Color COLOR = new Color(234, 243, 242);
    private double step;
    private double letterWidth;
    private boolean startColor;
    private int start;

    public MapBackgroundView(int width, int height, int letters, double letterWidth, int leftOffset, int start)
    {
        super(GraphicProperties.getInstance().penDefault, null, leftOffset, 0, width, height);
        this.step = letterWidth * letters;
        if(this.step != 0)
        {
            this.startColor = start%(2*letters)/letters == 0;
            this.start = (int) ( (start%letters)*letterWidth );
            this.letterWidth = letterWidth;
        }
    }

    @Override
    public void paint(Graphics2D g2)
    {
        if( isVisible() && step > 0 )
        {
            Rectangle c = g2.getClipBounds();
            Rectangle b = getBounds();
            if( c == null )
                c = b;
            int offset = b.x-start;
            boolean color = startColor;
            int endPos = Math.min(b.x+b.width, c.x+c.width);
            int startPos = offset+(int)(letterWidth/2);
            for(int i=0; ; i++)
            {
                color = !color;
                int pos = (int) ( startPos+i*step );
                if(pos > endPos) break;
                if(c.x > pos+step) continue;
                int start = Math.max(pos, b.x);
                int end = (int)Math.min(pos+step, endPos);
                Rectangle rect = new Rectangle(start, b.y, end-start+1, b.height);
                g2.setColor( color ? Color.white : COLOR);
                g2.fill(rect);
            }
        }
    }
}
