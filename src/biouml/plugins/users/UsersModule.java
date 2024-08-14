package biouml.plugins.users;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.developmentontheedge.application.Application;

import biouml.plugins.server.access.ClientModule;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.exception.CollectionLoginException;
import ru.biosoft.exception.MissingParameterException;
import ru.biosoft.util.NetworkConfigurator;

/**
 *  Special module type for users collection with XMPP support
 */
public class UsersModule extends ClientModule
{
    protected Connection xmppConnection = null;
    protected String host = null;
    protected String serverId = null;
    protected String username = null;

    protected static final String XMPP_SERVER_PROPERTY = "xmppServer";
    protected static final String XMPP_SERVER_ID_PROPERTY = "xmppServerId";
    protected static final String CONFERENCE_SERVER_PREFIX = "conference.";

    /**
     * Map of exists chat dialogs
     */
    protected Map<String, JDialog> chatMap = new HashMap<>();

    /**
     * Get {@link UsersModule} object by {@link ru.biosoft.access.core.DataElement}
     */
    public static UsersModule getUsersModule(DataElement de)
    {
        DataCollection<?> dc = de.getOrigin();
        while( ( dc != null ) && ! ( dc instanceof UsersModule ) )
        {
            dc = dc.getOrigin();
        }
        if( dc == null )
            return null;
        return (UsersModule)dc;
    }

    public UsersModule(DataCollection<?> primaryCollection, Properties properties) throws Exception
    {
        super(primaryCollection, properties);
    }

    @Override
    public void login(String username, String password)
    {
        if(username.isEmpty())
            throw new MissingParameterException( "User name" );
        if( xmppConnection != null )
        {
            //disconnect from XMPP server
            xmppConnection.disconnect();
        }
        super.login(username, password);
        if( !username.isEmpty() )
        {
            //login to XMPP after login
            this.host = getInfo().getProperty(XMPP_SERVER_PROPERTY);
            this.serverId = getInfo().getProperties().getProperty(XMPP_SERVER_ID_PROPERTY, this.host);
            this.username = username;

            try
            {
                if( NetworkConfigurator.isProxyUsed() )
                {
                    //connect via proxy if necessary
                    ProxyInfo proxyInfo = new ProxyInfo(ProxyInfo.ProxyType.HTTP, NetworkConfigurator.getHost(),
                            NetworkConfigurator.getPort(), NetworkConfigurator.getUsername(), NetworkConfigurator.getPassword());
                    ConnectionConfiguration config = new ConnectionConfiguration(host, 5222, proxyInfo);
                    //config.setSASLAuthenticationEnabled(false);
                    this.xmppConnection = new XMPPConnection(config);
                }
                else
                {
                    this.xmppConnection = new XMPPConnection(host);
                }

                xmppConnection.connect();
                xmppConnection.login(StringUtils.escapeNode(username), password);

                xmppConnection.getChatManager().addChatListener((chat, createdLocally) -> {
                    if( !createdLocally )
                    {
                        String jid = getJID(chat.getParticipant());
                        int ind = jid.indexOf('@');
                        if( ind != -1 )
                        {
                            String host = jid.substring(ind + 1);
                            if( !host.startsWith(CONFERENCE_SERVER_PREFIX) )
                            {
                                JDialog chatDialog = chatMap.get(jid);
                                if( chatDialog == null )
                                {
                                    chatDialog = createChatDialog(chat);
                                    chatMap.put(jid, chatDialog);
                                }
                            }
                        }
                    }
                });

                Roster roster = xmppConnection.getRoster();
                roster.addRosterListener(new RosterListener()
                {
                    @Override
                    public void presenceChanged(Presence presence)
                    {
                        String jid = getJID(presence.getFrom());
                        JDialog chatDialog = chatMap.get(jid);
                        if( chatDialog instanceof ChatListener )
                        {
                            ( (ChatListener)chatDialog ).presenceChanged(presence);
                        }
                    }

                    @Override
                    public void entriesUpdated(Collection<String> addresses)
                    {
                    }

                    @Override
                    public void entriesDeleted(Collection<String> addresses)
                    {
                    }

                    @Override
                    public void entriesAdded(Collection<String> addresses)
                    {
                    }
                });
            }
            catch( Exception e )
            {
                throw new CollectionLoginException(new BiosoftNetworkException(e, host), this, username);
            }
        }
    }
    public String getUserName()
    {
        return username;
    }

    public String getServerId()
    {
        return serverId;
    }

    public String getMyJID()
    {
        return StringUtils.escapeNode(username)+'@'+serverId;
    }

    /**
     * Create simple chat and open in dialog
     */
    public void openChatDialog(String targetUser)
    {
        String targetJID = StringUtils.escapeNode(targetUser) + "@" + serverId;
        JDialog chatDialog = chatMap.get(targetJID);
        if( chatDialog == null )
        {
            Chat chat = xmppConnection.getChatManager().createChat(targetJID, null);
            chatDialog = createChatDialog(chat);
            chatMap.put(targetJID, chatDialog);
        }
        chatDialog.setVisible(true);
    }

    protected JDialog createChatDialog(Chat chat)
    {
        JFrame frame = Application.getApplicationFrame();
        JDialog chatDialog = new ChatDialog(frame, this, chat);
        chatDialog.pack();
        chatDialog.setLocationRelativeTo(Application.getApplicationFrame());
        return chatDialog;
    }

    /**
     * Create multiuser chat and open in dialog
     */
    public void openGroupChatDialog(DataCollection<?> targetDC)
    {
        String targetJID = StringUtils.escapeNode(targetDC.getName()) + "@" + CONFERENCE_SERVER_PREFIX + serverId;
        JDialog chatDialog = chatMap.get(targetJID);
        if( chatDialog == null )
        {
            MultiUserChat muc = new MultiUserChat(xmppConnection, targetJID);
            try
            {
                muc.create(username);
                muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
                //invite group users
                for( String user : targetDC.getNameList() )
                {
                    String jName = StringUtils.escapeNode(user);
                    if( !username.equals(jName) )
                    {
                        muc.invite(jName, "Group chat started: " + targetJID);
                    }
                }
            }
            catch( XMPPException e )
            {
                //group exists
                try
                {
                    muc.join(username);
                }
                catch( XMPPException ex )
                {
                }
            }
            chatDialog = createGroupChatDialog(muc);
            chatMap.put(targetJID, chatDialog);
        }
        chatDialog.setVisible(true);
    }

    protected JDialog createGroupChatDialog(MultiUserChat muc)
    {
        JFrame frame = Application.getApplicationFrame();
        JDialog chatDialog = new GroupChatDialog(frame, this, muc);
        chatDialog.pack();
        chatDialog.setLocationRelativeTo(Application.getApplicationFrame());
        return chatDialog;
    }

    protected String getJID(String participant)
    {
        String jid = participant;
        int ind = jid.indexOf('/');
        if( ind != -1 )
        {
            jid = jid.substring(0, ind);
        }
        return jid;
    }

    public interface ChatListener
    {
        public void presenceChanged(Presence presence);
    }
}
