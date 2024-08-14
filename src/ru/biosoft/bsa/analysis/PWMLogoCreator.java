
package ru.biosoft.bsa.analysis;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;

import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author anna
 * @author lan
 */
public class PWMLogoCreator
{
    private static final Font FONT = new Font("Arial", Font.PLAIN, 80);
    private static final Color[] colors = new Color[] {
        new Color(255, 0, 0), new Color(58, 95, 216), new Color(255, 210, 0), new Color(13, 170, 65)};
    private static final byte[] letterToCode = Nucleotide5LetterAlphabet.getInstance().letterToCodeMatrix();

    public static CompositeView createLogoView(FrequencyMatrix matrix)
    {
        Alphabet alphabet = matrix.getAlphabet();
        Graphics2D graphics = ApplicationUtils.getGraphics();
        CompositeView logoView = new CompositeView();
        int letterWidth = getLetterWidth(graphics, alphabet)/alphabet.codeLength();
        logoView.add(new BoxView(null, new Brush(new Color(0,0,0,0)), 0, 0, letterWidth*matrix.getLength(), graphics.getFontMetrics(FONT).getHeight()*2));
        for( int j = 0; j < matrix.getLength(); j++ )
        {
            double[] dist = new double[alphabet.basicSize()];
            int i=0;
            for( byte code : alphabet.basicCodes() )
                dist[i++] = matrix.getFrequency(j, code);
            CompositeView positionView = createPositionView(dist, graphics, alphabet);
            if(positionView.size() > 0)
            {
                positionView.move(letterWidth*j, 0);
                logoView.add(positionView);
            }
        }
        return logoView;
    }
    
    private static int getLetterWidth(Graphics2D graphics, Alphabet alphabet)
    {
        java.awt.FontMetrics fm = graphics.getFontMetrics(FONT);
        int maxWidth = 0;
        for(byte code: alphabet.basicCodes())
        {
            maxWidth = Math.max(maxWidth, fm.stringWidth(alphabet.codeToLetters(code).toUpperCase()));
        }
        return maxWidth;
    }

    private static CompositeView createPositionView(double[] d, Graphics2D graphics, Alphabet alphabet)
    {
        byte size = alphabet.basicSize();
        double sum = 0;
        for(int i = 0; i < size; i++)
            sum+=d[i];
        double max = d[0];
        int i;
        for( i = 1; i < size; i++ )
            if( d[i] > max )
                max = d[i];
        double base = Math.log(size);
        for( i = 0; i < size; i++ )
        {
            double di = d[i] / sum;
            base += di > 0 ? di * Math.log(di) : 0;
        }
        base /= Math.log(2);
        double[] h = new double[size];
        for( i = 0; i < size; i++ )
            h[i] = base * d[i] / sum;
        CompositeView letter = new CompositeView();
        ColorFont exampleFont = new ColorFont(FONT, Color.BLACK);
        java.awt.FontMetrics fm = graphics.getFontMetrics(exampleFont.getFont());
        int baseLetterHeight = fm.getHeight() * 2;//116
        double spaceFromBottom = fm.getLeading() / 2 + fm.getDescent();//21.5;
        int widthG = fm.stringWidth("G");
        int[] widthShift = new int[] { ( -fm.stringWidth("A") + widthG ) / 2, ( -fm.stringWidth("C") + widthG ) / 2, 0,
                ( -fm.stringWidth("T") + widthG ) / 2};
        double lastSpaceFromBottom = spaceFromBottom;

        int lastYShift = 0;
        int fullHeight = 0;
        for( i = 0; i < size; i++ )
        {
            int curPos = -1;
            double min = -2;
            for( int j = 0; j < size; j++ )
            {
                if( d[j] > min && d[j] >= 0 )
                {
                    min = d[j];
                    curPos = j;
                }
            }
            if( h[curPos] > 0.015 )
            {
                byte[] letters = alphabet.codeToLetters((byte)curPos).getBytes();
                double height = 0;
                lastYShift -= h[curPos] * fm.getLeading() / 2;
                for(int pos=0; pos<letters.length; pos++)
                {
                    byte code = letterToCode[letters[pos]];
                    ColorFont colorFont = new ColorFont(FONT, colors[code]);
                    TextView tv = new TextView(String.valueOf(Character.toUpperCase((char)letters[pos])), new Point(0, 0), TextView.LEFT
                            | TextView.BASELINE, colorFont, graphics);
                    tv.setLocation(new Point((widthShift[code]+pos*widthG)/letters.length, lastYShift));
                    tv.scale(1.0/letters.length, h[curPos]);
                    height = tv.getBounds().getHeight();
                    letter.add(tv, CompositeView.REL);
                }
                lastSpaceFromBottom = Math.max(spaceFromBottom * h[curPos], 1);
                fullHeight = (int) ( lastYShift + height );
                lastYShift += height - lastSpaceFromBottom;
            }
            d[curPos] = -1;
        }
        double shift = baseLetterHeight - fullHeight - 2 * spaceFromBottom + lastSpaceFromBottom;

        if( shift > 0 )
            letter.setLocation(new Point(0, (int)shift));
        return letter;
    }
}
