package xmppclient.jingle;

import xmppclient.jingle.packet.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.XMPPConnection;

/**
 * Receives Jingle file transfers
 * Used for testing Jingle file transfer
 * @author Lee Boynton (323326)
 */
public class TestReceiver implements JingleSessionRequestListener
{
    /**
     * Logs into the server, awaits jingle session requests
     */
    public TestReceiver()
    {
        try
        {
            XMPPConnection.DEBUG_ENABLED = true;
            XMPPConnection connection = new XMPPConnection("praveen");
            connection.connect();
            JingleManager jingleManager = new JingleManager(connection);
            jingleManager.addSessionRequestListener(this);
            connection.login("test_user2", "test_pass2", "home");
        }
        catch (Exception ex)
        {
            Logger.getLogger(TestReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Creates a new test receiver
     * @param args not used
     */
    public static void main(String args[])
    {
        new TestReceiver();
    }

    /**
     * Called when a jingle session has been requested
     * @param request The request
     */
    @Override
    public void sessionRequested(JingleSessionRequest request)
    {
        System.out.println("Session request received");
        System.out.printf("From: %s\n", request.getFrom());

        Jingle jingle = request.getJingle();
        System.out.printf("Action: %s\n", jingle.getAction());
        System.out.printf("Initiator: %s\n", jingle.getInitiator());
        System.out.printf("Responder: %s\n", jingle.getResponder());
        
        request.accept();
    }
}
