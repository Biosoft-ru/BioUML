package ru.biosoft.server.servlets.support;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import one.util.streamex.IntStreamEx;

import com.developmentontheedge.application.Application;

/**
 * @author tolstyh
 * Utility methods for support servlet
 */
public class Utils
{
    protected static final String PASSWORD_POOL = "abcdefghijklmnopqrstuvwxyz0123456789";
    protected static final String SMTP_HOST_ATTR = "SmtpHost";
    protected static final String SMTP_USER_ATTR = "SmtpUser";
    protected static final String SMTP_PASS_ATTR = "SmtpPass";

    /**
     * Generates random password, containing 8 symbols using english alphabet and numbers.
     *
     * @param userName user name
     * @return generated password
     */
    public static String newRandomPassword(String userName)
    {
        Random random = userName == null ? new Random() : new Random(System.currentTimeMillis() + userName.hashCode());
        return IntStreamEx.of( random, 8, 0, PASSWORD_POOL.length() ).map( PASSWORD_POOL::charAt ).charsToString();
    }

    /**
     * Send email
     * @param recipients
     * @param subject
     * @param message
     * @param from
     * @throws MessagingException
     */
    public static void sendMail(String recipient, String subject, String message, String from, List<File> attachments)
            throws MessagingException
    {
        //get SMTP session
        Session session = getSession();
        session.setDebug(false);

        // create a message
        MimeMessage msg = new MimeMessage(session);

        // set the from and to address
        InternetAddress addressFrom = new InternetAddress(from);
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[1];
        addressTo[0] = new InternetAddress(recipient);
        msg.setRecipients(Message.RecipientType.TO, addressTo);

        // Setting the Subject and Content Type
        msg.setSubject(subject);

        if( attachments == null )
        {
            msg.setContent(message, "text/html");
        }
        else
        {
            // create the message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            //fill message
            messageBodyPart.setText(message);

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            for( File file : attachments )
            {
                messageBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(file);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(file.getName());
                multipart.addBodyPart(messageBodyPart);
            }

            // Put parts in message
            msg.setContent(multipart);
        }
        Transport.send(msg);
    }

    /**
     * Get SMTP session
     * @return
     */
    private static Session getSession()
    {
        //Set the host smtp address
        Properties props = new Properties();
        props.put("mail.smtp.host", Application.getGlobalValue(SMTP_HOST_ATTR));

        String user = Application.getGlobalValue(SMTP_USER_ATTR);
        String pass = Application.getGlobalValue(SMTP_PASS_ATTR);
        if( !user.equals(SMTP_USER_ATTR) && !pass.equals(SMTP_PASS_ATTR) )
        {
            //use authentication
            Authenticator authenticator = new Authenticator(user, pass);
            props.setProperty("mail.smtp.submitter", authenticator.getPasswordAuthentication().getUserName());
            props.setProperty("mail.smtp.auth", "true");
            return Session.getInstance(props, authenticator);
        }
        else
        {
            // get simple default session
            return Session.getDefaultInstance(props, null);
        }
    }

    /**
     * {@link javax.mail.Authenticator} implementation
     */
    private static class Authenticator extends javax.mail.Authenticator
    {
        private final PasswordAuthentication authentication;

        public Authenticator(String username, String password)
        {
            authentication = new PasswordAuthentication(username, password);
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return authentication;
        }
    }
}
