package de.rwth.comsys.capture_vpn.network;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import static de.rwth.comsys.capture_vpn.util.CaptureConstants.DEBUG;

/**
 * forwarder for UDP datagrams
 */

public class UDPForwarder extends Forwarder
{
    private static final String TAG = "UDPForwarder";
    private static final int maxPacketLength = 1472;
    private DatagramChannel datagramChannel;
    private boolean open;

    public UDPForwarder(UDPPacket udpPacket) throws IOException
    {
        super(DatagramChannel.open(), udpPacket.getIPPacket());
        incomingBuffer = ByteBuffer.allocateDirect(65535);

        datagramChannel = (DatagramChannel) getChannel();
        datagramChannel.socket().setReceiveBufferSize(incomingBuffer.capacity());
        datagramChannel.configureBlocking(false);

        datagramChannel.connect(getSocketAddress());

        open = true;

        if (DEBUG)
            Log.v(TAG, super.toStringOutgoing() + "Created UDPForwarder");

        processOutgoingPacket(udpPacket);

        captureCentral.registerForwarder(this, SelectionKey.OP_READ);
    }

    @Override
    protected void updateForwarder() throws IOException
    {
        processOutgoingPackets();

        int length;
        while ((length = datagramChannel.read(incomingBuffer)) > 0)
        {
            incomingBuffer.flip();
            handleReadIncomingPacket();

            processOutgoingPackets();
        }
        if (length < 0)
        {
            close();
        }

        if (!isClosed())
            captureCentral.registerForwarder(this, SelectionKey.OP_READ);
    }

    @Override
    public void processOutgoingPacket(TransportLayerPacket packet)
    {
        UDPPacket udpPacket = (UDPPacket) packet;
        try
        {
            if (!open)
                throw new IOException("Closed!");

            handleWriteOutgoingPacket(packet);
            if (DEBUG)
                Log.v(TAG, super.toStringOutgoing() + "  Sent   UDP Packet (Length: " + udpPacket.getIPPacket().getTotalLength() + ")");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            close();
        }
    }

    @Override
    public void handleWriteOutgoingPacket(TransportLayerPacket packet)
    {
        UDPPacket udp = (UDPPacket) packet;
        try
        {
            packet.getRawPacket().position(udp.getPayloadStart());
            while (packet.getRawPacket().hasRemaining())
            {
                datagramChannel.write(packet.getRawPacket().getByteBuffer());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            close();
        }
    }

    @Override
    public void handleReadIncomingPacket()
    {
        while (incomingBuffer.hasRemaining())
        {

            IPPacket ipPacket = IPPacketFactory.createUDPDataPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), incomingBuffer, maxPacketLength);
            try
            {
                if (!open)
                    throw new IOException("Closed!");
                addIncomingPacketToTunIn(ipPacket);
                if (DEBUG)
                    Log.v(TAG, super.toStringIncoming() + "Received UDP Packet (Length: " + ipPacket.getTotalLength() + ")");
            }
            catch (IOException e)
            {
                e.printStackTrace();
                close();
            }
        }

        incomingBuffer.clear();
    }

    @Override
    public void close()
    {
        try
        {
            if (DEBUG)
                Log.v(TAG, super.toString() + " Closing");
            if (datagramChannel != null)
                datagramChannel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            kill();
        }
        super.close();
    }

    @Override
    public void kill()
    {
        try
        {
            open = false;
            if (DEBUG)
                Log.v(TAG, super.toString() + "Killing");
            datagramChannel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void handleError(IOException e)
    {

    }

    @Override
    public void handleConnect()
    {

    }

    @Override
    public String toString()
    {
        return super.toString() + "\nUDP-Connection: " + (open ? "open" : "closed");
    }
}
