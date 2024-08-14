package biouml.plugins.users;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import biouml.plugins.users.UsersModule.ChatListener;

import com.developmentontheedge.application.dialog.OkCancelDialog;

/**
 * Simple chat dialog
 */
@SuppressWarnings ( "serial" )
public class ChatDialog extends OkCancelDialog implements ChatListener
{
    private static final Logger log = Logger.getLogger(ChatDialog.class.getName());
    protected static final MessageBundle messageBundle = new MessageBundle();

    protected JLabel userStatus;
    protected JEditorPane messages;
    protected JTextArea input;

    protected UsersModule usersModule;
    protected Chat chat;

    protected List<MessageItem> messageList = new ArrayList<>();

    protected ChatDialog(JFrame frame, UsersModule usersModule, Chat chat)
    {
        super(frame, "", false);

        this.usersModule = usersModule;
        this.chat = chat;

        getOkButton().setText(messageBundle.getResourceString("CHAT_DIALOG_SEND"));
        getCancelButton().setText(messageBundle.getResourceString("CHAT_DIALOG_CLOSE"));

        JPanel content = initChatPanel(true);
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContent(content);

        if( chat != null )
        {
            setTitle(MessageFormat.format(messageBundle.getResourceString("CHAT_DIALOG_TITLE"), new Object[] {chat.getParticipant()}));
            chat.addMessageListener((arg0, message) -> {
                String from = message.getFrom();
                String resource = StringUtils.parseResource(from);
                from = StringUtils.parseBareAddress(from);
                String body = message.getBody();
                if( body != null )
                {
                    messageList.add(new MessageItem(body, from, resource));
                    updateMessagePane();
                    setVisible(true);
                }
            });
        }
    }
    protected JPanel initChatPanel(boolean addStatus)
    {
        JPanel content = new JPanel(new GridBagLayout());
        if( addStatus )
        {
            content.add(new JLabel(messageBundle.getResourceString("CHAT_DIALOG_STATUS")), new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            userStatus = new JLabel("offline");
            content.add(userStatus, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        }

        messages = new JEditorPane("text/html", "");
        messages.setEditable(false);
        JScrollPane messagesScroll = new JScrollPane(messages, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        messagesScroll.setPreferredSize(new Dimension(400, 300));
        content.add(messagesScroll, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        messagesScroll.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(e.getAdjustable().getMaximum()));

        input = new JTextArea();
        JScrollPane inputScroll = new JScrollPane(input, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setPreferredSize(new Dimension(400, 50));
        content.add(inputScroll, new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

        input.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent ke)
            {
                if( ke.getKeyCode() == KeyEvent.VK_ENTER )
                {
                    okPressed();
                }
            }

            @Override
            public void keyReleased(KeyEvent ke)
            {
                if( ke.getKeyCode() == KeyEvent.VK_ENTER )
                {
                    input.setText("");
                }
            }
        });
        return content;
    }

    protected void updateMessagePane()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html>");
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        for( MessageItem mi : messageList )
        {
            buffer.append("<font color='" + getUserColor(mi.getFrom()) + "'>");
            buffer.append(mi.getFrom());
            buffer.append('(');
            buffer.append(dateFormat.format(mi.getDate()));
            buffer.append(')');
            buffer.append("</font><br/>");
            buffer.append(mi.getMsg());
            buffer.append("<br/>");
        }
        buffer.append("</html>");
        messages.setText(buffer.toString());
    }

    protected String getUserColor(String user)
    {
        if( user.equals(usersModule.getMyJID()) )
        {
            return "blue";
        }
        else if( chat.getParticipant().startsWith(user) )
        {
            return "red";
        }
        return "black";
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
                chat.sendMessage(mi.getMsg());
                messageList.add(mi);
                updateMessagePane();
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
        userStatus.setText(presence.toString());
    }
}
