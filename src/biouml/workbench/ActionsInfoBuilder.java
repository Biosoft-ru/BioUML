package biouml.workbench;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import ru.biosoft.access.repository.PluginActions;
import ru.biosoft.gui.Document;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.DocumentFactory;
import com.developmentontheedge.application.DocumentManager;

/**
 * Special eclipse-based application for actions exporting
 * into human readable document with icons
 */
public class ActionsInfoBuilder implements IApplication
{
    @Override
    public Object start(IApplicationContext arg0)
    {
        try
        {
            File configFile = new File( "biouml.lcf" );
            try( FileInputStream fis = new FileInputStream( configFile ) )
            {
                LogManager.getLogManager().readConfiguration( fis );
            }
            catch( Exception e1 )
            {
                Logger.getGlobal().log( Level.SEVERE, "Error init logging", e1 );
            }

            Object appArgs = arg0.getArguments().get( "application.args" );
            if( ! ( appArgs instanceof String[] ) )
            {
                if( appArgs == null )
                    appArgs = "null";
                System.out.println( "Can not start: incorrect input application arguments (" + appArgs + ")" );
                return IApplication.EXIT_OK;
            }

            String[] args = (String[])appArgs;

            System.out.println("-----------------------------");
            if( args.length < 1 )
            {
                System.out.println("Missing output folder name");
            }
            else
            {
                exportActions(args[0]);
            }
            System.out.println("-----------------------------");
        }
        catch( Throwable t )
        {
            System.err.println("Can not start action exporter: " + t);
            t.printStackTrace();

            System.out.println("\nPress any key to continue");
            try
            {
                System.in.read();
            }
            catch( Throwable ignore )
            {
            }
        }

        return IApplication.EXIT_OK;
    }

    @Override
    public void stop()
    {
    }

    protected void exportActions(String folderName) throws Exception
    {
        File folder = new File(folderName);
        folder.delete();
        folder.mkdirs();
        try (PrintWriter pw = new PrintWriter( new File( folder, "index.html" ) ))
        {
            //initialize and hide main application
            ApplicationFrame applicationFrame = new BioUMLApplication( null, new String[0] );
            applicationFrame.setVisible( false );

            //Initialize repository actions
            initRepositoryActions();

            //Initialize document actions
            initDocumentsActions( applicationFrame );

            Hashtable<String, Action> actions = Application.getActionManager().getActions();
            System.out.println( "Actions size: " + actions.size() );
            System.out.println( "Writing to " + folderName + " ..." );
            printActions( pw, folder, actions );
        }
    }

    protected void initRepositoryActions()
    {
        //PluginActions automatically loads all 'ru.biosoft.access.repositoryActionsProvider' extensions
        //ant getActions method initialize each of them
        new PluginActions().getActions(new Object());
    }

    protected void initDocumentsActions(ApplicationFrame applicationFrame)
    {
        DocumentManager documentManager = applicationFrame.getDocumentManager();
        if( documentManager instanceof ru.biosoft.gui.DocumentManager )
        {
            ( (ru.biosoft.gui.DocumentManager)documentManager ).documentFactories().map( DocumentFactory::createDocument )
                .select( Document.class ).forEach( ad -> ad.getActions( null ) );
        }
    }

    protected void printActions(PrintWriter pw, File images, Hashtable<String, Action> actions) throws Exception
    {
        pw.println("<html>");
        pw.println("<body>");
        pw.println("<table border=1>");
        pw.println("<tr><th></th><th>Key</th><th>Icon</th><th>Short description</th><th>Full description</th></tr>");
        int ind=1;
        for( Entry<String, Action> entry : actions.entrySet() )
        {
            String name = entry.getKey();
            Action action = entry.getValue();
            Object icon = action.getValue(Action.SMALL_ICON);
            String iconStr = "";
            if( icon instanceof ImageIcon )
            {
                ImageIcon imageIcon = (ImageIcon)icon;
                Image image = imageIcon.getImage();
                RenderedImage rendered = null;
                if( image instanceof RenderedImage )
                {
                    rendered = (RenderedImage)image;
                }
                else
                {
                    BufferedImage buffered = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(),
                            BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = buffered.createGraphics();
                    g.drawImage(image, 0, 0, null);
                    g.dispose();
                    rendered = buffered;
                }
                ImageIO.write(rendered, "PNG", new File(images, name + ".png"));
                iconStr = "<img src='" + name + ".png'/>";
            }

            pw.println("<tr>");
            pw.println("<td>" + ind++ + "</td>");
            pw.println("<td>" + name + "</td>");
            pw.println("<td>" + iconStr + "</td>");
            pw.println("<td>" + action.getValue(Action.SHORT_DESCRIPTION) + "</td>");
            pw.println("<td>" + action.getValue(Action.LONG_DESCRIPTION) + "</td>");
            pw.println("</tr>");
        }
        pw.println("</table>");
        pw.println("</body>");
        pw.println("</html>");
    }
}