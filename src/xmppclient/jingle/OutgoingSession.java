package xmppclient.jingle;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.Bytestream;
import xmppclient.jingle.packet.Description;
import xmppclient.jingle.packet.Jingle;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.filter.PacketTypeFilter;

/**
 * An outgoing jingle file transfer
 * Based on XEP-0234: Jingle File Transfer - http://www.xmpp.org/extensions/xep-0234.html
 * @author Lee Boynton (323326)
 */
public class OutgoingSession extends Session implements PacketListener
{
    private File file;
    private InputStream in;
    private DataOutputStream out;
    private Socket socket;
    private ServerSocket serverSocket;
    private byte[] buffer;

    /**
     * Creates a new outgoing file transfer session
     * @param connection The XMPP connection to use to send control information
     * @param responder The remote user who should receive the file
     * @param file The file to be sent
     */
    public OutgoingSession(XMPPConnection connection, String responder, java.io.File file)
    {
        super(connection, responder);
        this.file = file;
        Jingle jingle = new Jingle();
        jingle.setFrom(connection.getUser());
        jingle.setTo(responder);
        jingle.setInitiator(connection.getUser());
        jingle.setResponder(responder);
        jingle.setSid(StringUtils.randomString(5));
        jingle.setAction(Jingle.Action.SESSIONINITIATE);
        jingle.setContent(Jingle.Content.FILEOFFER);
        jingle.setDescription(new Description(
                new xmppclient.jingle.packet.File(file.getName(),
                String.valueOf(file.length()),
                String.valueOf(file.hashCode()))));
        connection.sendPacket(jingle);
        connection.addPacketListener(this, new PacketTypeFilter(Jingle.class));
    }

    /**
     * Opens a server socket on a free port. Waits for the remtoe user to connect,
     * and starts sending the file byte stream. If the remote user closes the
     * connection then it will also close the connection.
     */
    @Override
    public void start()
    {
        System.out.println("Starting");
        InetSocketAddress addr = new InetSocketAddress(getHostAddress(), port);

        try
        {
            buffer = new byte[new Long(file.length()).intValue()];
            serverSocket = new ServerSocket();
            serverSocket.bind(addr);
            System.out.printf("Listening for connections on: %s:%s\n",
                    serverSocket.getInetAddress().getHostAddress(),
                    serverSocket.getLocalPort());
            socket = serverSocket.accept();
            out = new DataOutputStream(socket.getOutputStream());
            in = new BufferedInputStream(new FileInputStream(file));
            System.out.println("Reading in file");
            in.read(buffer);
            System.out.println("Writing out file");
            out.write(buffer);
            System.out.println("Finished");
            //out.flush();
        }
        catch(SocketException ex)
        {
            System.out.println("Peer closed socket");
            terminate();
        }
        catch (Exception ex)
        {
            Logger.getLogger(OutgoingSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Terminates the outgoing file transfer by removing the Jingle session accept
     * listener, and closing the file input stream, the file output stream and 
     * finally the socket.
     */
    @Override
    public void terminate()
    {
        System.out.println("Closing outgoing session");
        
        super.connection.removePacketListener(this);
        
        try
        {
            System.out.println("Closing input stream");
            in.close();
            System.out.println("Closing output stream");
            out.close();
            System.out.println("Closing socket");
            socket.close();
            System.out.println("Connection closed");
        }
        catch (IOException ex)
        {
            Logger.getLogger(OutgoingSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void processPacket(Packet packet)
    {
        Jingle jingle = (Jingle) packet;
        System.out.printf("%s: Jingle packet received\n", this.getClass().getName());

        if (jingle.getAction() == Jingle.Action.SESSIONACCEPT)
        {
            port = getFreePort();
            Bytestream bytestream = new Bytestream();
            bytestream.addStreamHost(super.connection.getUser(), getHostAddress(), port);
            bytestream.setTo(jingle.getFrom());
            bytestream.setFrom(jingle.getTo());
            bytestream.setMode(Bytestream.Mode.tcp);
            bytestream.setType(IQ.Type.SET);
            connection.sendPacket(bytestream);
            start();
        }
    }
}
