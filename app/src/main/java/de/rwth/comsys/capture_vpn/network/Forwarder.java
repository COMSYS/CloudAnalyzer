package de.rwth.comsys.capture_vpn.network;

import android.util.Log;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.CaptureService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * common functionality of all traffic forwarders
 * (independent of the actual protocol)
 */

public abstract class Forwarder
{
    private static final String TAG = "Forwarder";

    protected ByteBuffer incomingBuffer;
    protected SelectableChannel channel;
    protected CaptureCentral captureCentral;
    protected LinkedBlockingQueue<IPPacket> tunInQueue;
    protected LinkedBlockingQueue<TransportLayerPacket> tunOutQueue;
    protected IPPacket.IPKey key;
    protected TransportLayerPacket.Protocol protocol;
    protected ReentrantLock updateLock;
    private InetSocketAddress socketAddress;
    private boolean closed;
    private Boolean waitingForUpdate;

    protected Forwarder(SelectableChannel channel, IPPacket packet) throws UnknownHostException
    {
        this.waitingForUpdate = false;
        this.closed = false;
        this.key = packet.getKey();
        this.protocol = packet.getPayload().getProtocol();
        this.channel = channel;
        this.updateLock = new ReentrantLock();
        // create a socket for remote destination
        socketAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[]{(byte) getRemoteIP()[0], (byte) getRemoteIP()[1], (byte) getRemoteIP()[2], (byte) getRemoteIP()[3]}), getRemotePort());

        boolean res = false;
        switch (protocol)
        {
            case TCP:
                res = CaptureService.protect((SocketChannel) channel);
                break;
            case UDP:
                res = CaptureService.protect((DatagramChannel) channel);
                break;
        }

        if (!res)
        {
            Log.e(TAG, "Protecting failed: " + getSocketAddress().toString());
            close();
        }
        else
        {
            captureCentral = CaptureCentral.getInstance();
            tunInQueue = captureCentral.getTunInQueue();
            tunOutQueue = new LinkedBlockingQueue<>();
        }
    }

    public TransportLayerPacket.Protocol getProtocol()
    {
        return protocol;
    }

    public short[] getRemoteIP()
    {
        return key.getRemoteIP();
    }

    public int getRemotePort()
    {
        return key.getRemotePort();
    }

    public short[] getLocalIP()
    {
        return key.getLocalIP();
    }

    public int getLocalPort()
    {
        return key.getLocalPort();
    }

    public ByteBuffer getIncomingBuffer()
    {
        return incomingBuffer;
    }

    public SelectableChannel getChannel()
    {
        return channel;
    }

    public InetSocketAddress getSocketAddress()
    {
        return socketAddress;
    }

    protected abstract void processOutgoingPacket(TransportLayerPacket packet);

    // add a new incoming packet
    public void addIncomingPacketToTunIn(IPPacket ipPacket)
    {
        ipPacket.setCount(IPPacket.getPacketCount());
        ipPacket.setIncoming(true);
        ipPacket.setForeground(CaptureCentral.isScreenOn());
        ipPacket.setNetworkType(CaptureCentral.getNetworkType());

        try
        {
            tunInQueue.put(ipPacket);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void registerForUpdate()
    {
        synchronized (this)
        {
            if (!waitingForUpdate)
            {
                captureCentral.addForwarderToUpdateQueue(this);
                setWaitingForUpdate(true);
            }
        }
    }

    public void setWaitingForUpdate(boolean waiting)
    {
        synchronized (this)
        {
            waitingForUpdate = waiting;
        }
    }

    public void close()
    {
        closed = true;
        captureCentral.getForwarderManager().removeForwarder(this);
    }

    public boolean isClosed()
    {
        return closed;
    }

    public abstract void kill();

    public abstract void handleError(IOException e);

    public abstract void handleConnect();

    public abstract void handleReadIncomingPacket();

    public abstract void handleWriteOutgoingPacket(TransportLayerPacket packet);

    public boolean update() throws IOException
    {
        // try to lock
        if (updateLock.tryLock())
        {
            try
            {
                // We will update this forwarder, hence we set waitingForUpdate to false.
                // Afterwards, this forwarder can be put into the update queue again as we cannot ensure
                // that after updateForwarder() all packets are processed.
                setWaitingForUpdate(false);

                updateForwarder();

                return true;
            }
            finally
            {
                updateLock.unlock();
            }
        }
        else
            return false;
    }

    protected abstract void updateForwarder() throws IOException;

    protected void processOutgoingPackets()
    {
        TransportLayerPacket packet;
        while ((packet = tunOutQueue.poll()) != null)
        {
            processOutgoingPacket(packet);
        }
    }

    public LinkedBlockingQueue<TransportLayerPacket> getTunOutQueue()
    {
        return tunOutQueue;
    }

    public IPPacket.IPKey getKey()
    {
        return key;
    }

    @Override
    public String toString()
    {
        return "Connection: " + IPPacket.ipToString(getLocalIP()) + ":" + getLocalPort() + " <-> " + IPPacket.ipToString(getRemoteIP()) + ":" + getRemotePort();
    }

    public String toStringOutgoing()
    {
        return "-> (" + getLocalPort() + ", " + IPPacket.ipToString(getRemoteIP()) + ":" + getRemotePort() + "): ";
    }

    public String toStringIncoming()
    {
        return "<- (" + getLocalPort() + ", " + IPPacket.ipToString(getRemoteIP()) + ":" + getRemotePort() + "): ";
    }
}
