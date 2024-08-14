package biouml.plugins.users;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.util.logging.Logger;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 * Multiuser chat dialog
 */
@SuppressWarnings ( "serial" )
public class GroupChatDialog extends ChatDialog
{
    private final Logger log = Logger.getLogger(GroupChatDialog.class.getName());

    protected MultiUserChat muc;
    protected JEditorPane users;
    protected Random random = new Random();

    protected Map<String, String> colorMap = new HashMap<>();
    protected Set<String> participants = new HashSet<>();

    protected GroupChatDialog(JFrame frame, final UsersModule usersModule, MultiUserChat muc)
    {
        super(frame, usersModule, null);
        setTitle(MessageFormat.format(messageBundle.getResourceString("CHAT_DIALOG_TITLE"), new Object[] {muc.getRoom()}));

        this.muc = muc;

        muc.addMessageListener(packet -> {
            String from = packet.getFrom();
            String resource = null;
            int ind = from.indexOf('/');
            if( ind != -1 )
            {
                resource = from.substring(ind + 1);
            }
            if( resource != null )
            {
                from = resource + "@" + usersModule.getServerId();
                if( packet instanceof Message )
                {
                    String body = ( (Message)packet ).getBody();
                    if( body != null )
                    {
                        messageList.add(new MessageItem(body, from, resource));
                        updateMessagePane();
                    }
                }
            }
        });

        muc.addParticipantListener(packet -> {
            String jid = packet.getFrom();
            String username = null;
            int ind = jid.lastIndexOf('/');
            if( ind != -1 )
            {
                username = jid.substring(ind + 1);
            }
            if( username != null && packet instanceof Presence )
            {
                Presence.Type type = ( (Presence)packet ).getType();
                if( type == Presence.Type.available )
                {
                    //show in list
                    participants.add(username);
                }
                else
                {
                    //hide from list
                    participants.remove(username);
                }
                updateUsersPane();
            }
        });
    }

    protected void updateUsersPane()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html>");
        for( String username : participants )
        {
            buffer.append("<font color='" + getUserColor(username + "@" + usersModule.getServerId()) + "'>");
            buffer.append(username);
            buffer.append("</font><br/>");
        }
        buffer.append("</html>");
        users.setText(buffer.toString());
    }

    @Override
    protected JPanel initChatPanel(boolean addStatus)
    {
        JPanel content = new JPanel(new BorderLayout());
        JPanel left = super.initChatPanel(false);
        content.add(left, BorderLayout.WEST);

        users = new JEditorPane("text/html", "");
        users.setEditable(false);
        JScrollPane usersScroll = new JScrollPane(users, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        usersScroll.setPreferredSize(new Dimension(100, 350));
        content.add(usersScroll, BorderLayout.EAST);
        return content;
    }

    @Override
    protected String getUserColor(String user)
    {
        String color = colorMap.get(user);
        if( color == null )
        {
            int r = random.nextInt(256);
            int g = random.nextInt(256);
            int b = random.nextInt(256);
            int delta = ( ( r + g + b ) - 255 ) / 3;
            if( r > delta )
                r = r - delta;
            else
                r = 0;
            if( g > delta )
                g = g - delta;
            else
                g = 0;
            if( b > delta )
                b = b - delta;
            else
                b = 0;
            Color c = new Color(r, g, b);
            color = Integer.toHexString(c.getRGB());
            color = "#" + color.substring(2, color.length());
            colorMap.put(user, color);
        }
        return color;
    }

    @Override
    protected void okPressed()
    {
        String msg = input.getText().trim();
        input.setText("");
        if( msg.length() > 0 )
        {
            MessageItem mi = new MessageItem(msg, usersModule.getMyJID(), null);
            try
            {
                muc.sendMessage(mi.getMsg());
            }
            catch( XMPPException e )
            {
                log.log(Level.SEVERE, "Cannot sent message", e);
            }
        }
    }

    @Override
    public void presenceChanged(Presence presence)
    {
        //nothing to do
    }
}
