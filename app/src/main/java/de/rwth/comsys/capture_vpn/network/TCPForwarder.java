package de.rwth.comsys.capture_vpn.network;

import android.util.Log;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static de.rwth.comsys.capture_vpn.util.CaptureConstants.DEBUG;
import static de.rwth.comsys.capture_vpn.util.CaptureConstants.DEBUG_TCP;

/**
 * forwarder for TCP packets (one socket for each TCP connection)
 * <p>
 * internally keeps track of the connections status:
 * - to drop SYN packets upon connection creation
 * - to insert ACK packets when necessary (connection creation and termination)
 * - to insert FIN packets when the connection has been closed
 */

public class TCPForwarder extends Forwarder
{
    private static final String TAG = "TCPForwarder";
    private static final long MAX_FIELD = 4294967295L; // 2^32
    private static final int WINDOW_SIZE = 65535;

    private TCPState state;
    private long applicationSeqNr;
    private int applicationWindowSize;
    private long remoteSeqNr;
    private long remoteAckNr;
    private int maximumSegmentSize;
    private byte errorCounter;
    private SocketChannel socketChannel;

    public TCPForwarder(TCPPacket tcpPacket) throws IOException
    {
        super(SocketChannel.open(), tcpPacket.getIPPacket());
        socketChannel = (SocketChannel) getChannel();

        state = TCPState.CONNECTING;
        remoteSeqNr = 1000;
        maximumSegmentSize = 1460;
        errorCounter = 0;

        if (DEBUG)
            Log.v(TAG, super.toStringOutgoing() + "Created TCPForwarder");

        handleAppConnecting(tcpPacket);

        captureCentral.registerForwarder(this, SelectionKey.OP_CONNECT);
    }

    public static long addIPPacketToField(long field, IPPacket ipPacket)
    {
        return addNumberToField(field, ((TCPPacket) ipPacket.getPayload()).getPayloadLength());
    }

    public static long addTCPPacketToField(long field, TCPPacket tcpPacket)
    {
        return addNumberToField(field, tcpPacket.getPayloadLength());
    }

    public static long addNumberToField(long field, long number)
    {
        return (field + number) % MAX_FIELD;
    }

    public static long increaseField(long field)
    {
        return addNumberToField(field, 1);
    }

    @Override
    protected void updateForwarder() throws IOException
    {
        if (socketChannel.isOpen() && socketChannel.finishConnect() && socketChannel.isConnected())
        {
            if (state == TCPState.CONNECTING)
            {
                handleConnect();
            }

            processOutgoingPackets();

            if (state.canReceive())
            {
                incomingBuffer.limit(applicationWindowSize);
                int length = socketChannel.read(incomingBuffer);
                if (length > 0)
                {
                    incomingBuffer.flip();
                    handleReadIncomingPacket();
                }
                else if (length < 0)
                {
                    handleEOF();
                }
            }
        }

        if (state.canReceive())
            captureCentral.registerForwarder(this, SelectionKey.OP_READ);
    }

    public void processOutgoingPacket(TransportLayerPacket packet)
    {
        TCPPacket tcpPacket = (TCPPacket) packet;

        try
        {
            if (sanityCheck(tcpPacket))
            {
                applicationWindowSize = Math.min(tcpPacket.getWindowSize(), incomingBuffer.capacity());

                switch (state)
                {
                    // we never reach this state
                    case CONNECTING:
                        // already covered through constructor TCPForwarder()
                        throw new IllegalStateException();

                        // application confirms connection
                    case SOCKET_CREATED:
                        handlingConnectionConfirmation(tcpPacket);
                        break;

                    // application and receiver communicate
                    case SOCKET_ACTIVE:
                        handlingActiveConnection(tcpPacket);
                        break;

                    // we never reach this state
                    case APPLICATION_CLOSING:
                        // this state is triggered from handlingActiveConnection()
                        throw new IllegalStateException();

                        // receiver closed socket connection
                    case RECEIVER_CLOSING:
                        handlingReceiverClosing(tcpPacket);
                        break;

                    // we never reach this state
                    case RECEIVER_CLOSED:
                        // this state is triggered from handlingReceiverClosing()
                        throw new IllegalStateException();

                        // Forwarder should already be closed
                    case CLOSED:
                        handlingClosedState(tcpPacket);
                        break;

                    default:
                        throw new IllegalStateException();
                }
            }
            else
            {
                if (errorCounter >= 2)
                {
                    Log.w(TAG, super.toStringOutgoing() + "Too many errors occurred in " + state.toString());
                    addIncomingPacketToTunIn(IPPacketFactory.createTCPFlagPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), increaseField(tcpPacket.getAcknowledgmentNumber()), addTCPPacketToField(tcpPacket.getSequenceNumber(), tcpPacket), getLocalWindowSize(), TCPPacket.Flag.ACK, TCPPacket.Flag.RST));
                    if (DEBUG)
                        Log.d(TAG, super.toStringIncoming() + "Simulated TCP Packet (RST) in " + state.toString());

                    state = TCPState.CLOSED;
                    close();
                }

            }
        }
        catch (IllegalStateException e)
        {
            // This should not have happened, just drop
            if (DEBUG)
                Log.w(TAG, super.toStringOutgoing() + "Could not handle TCP Packet in illegal state " + state.toString());

            state = TCPState.CLOSED;
            close();
        }
        catch (Exception e)
        {
            // sanity check should not throw this
            e.printStackTrace();
        }

        if (DEBUG && DEBUG_TCP)
            Log.i(TAG, super.toStringOutgoing() + "Current  TCP State (" + state.toString() + ") " + Thread.currentThread().getName());
    }

    private void handleAppConnecting(TCPPacket tcpPacket)
    {
        if (tcpPacket.isSYNFlagSet())
        {
            try
            {
                if (!(socketChannel.isConnectionPending() || socketChannel.isConnected()))
                {
                    incomingBuffer = ByteBuffer.allocateDirect(tcpPacket.getWindowSize());

                    socketChannel.configureBlocking(false);
                    socketChannel.connect(getSocketAddress());
                    remoteAckNr = tcpPacket.getSequenceNumber();
                    applicationSeqNr = increaseField(remoteAckNr); // set seqNumber + 1

                    if (DEBUG)
                        if (DEBUG_TCP)
                            Log.d(TAG, super.toStringOutgoing() + "Processed TCP Packet (SYN) in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
                        else
                            Log.d(TAG, super.toStringOutgoing() + "Processed TCP Packet (SYN) in " + state.toString());
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                if (DEBUG_TCP)
                    Log.e(TAG, super.toStringOutgoing() + "Could not handle TCP Packet (SYN) in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
                else
                    Log.e(TAG, super.toStringOutgoing() + "Could not handle TCP Packet (SYN) in " + state.toString());

                state = TCPState.CLOSED;
                close();
            }
        }
        else
        {
            // This should not occur as we filter beforehand!
            if (DEBUG_TCP)
                Log.e(TAG, super.toStringOutgoing() + "Waited for TCP Packet (SYN) in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
            else
                Log.e(TAG, super.toStringOutgoing() + "Waited for TCP Packet (SYN) in " + state.toString());

            state = TCPState.CLOSED;
            close();
        }
    }

    private void handleReset(TCPPacket tcpPacket)
    {
        if (DEBUG)
            if (DEBUG_TCP)
                Log.d(TAG, super.toStringOutgoing() + "Received TCP Packet (RST) in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
            else
                Log.d(TAG, super.toStringOutgoing() + "Received TCP Packet (RST) in " + state.toString());

        // just close connection as we are resetting
        close();
        state = TCPState.CLOSED;
    }

    private void handlingConnectionConfirmation(TCPPacket tcpPacket)
    {
        if (tcpPacket.isACKFlagSet())
        {
            // do nothing, simply change state
            if (DEBUG)
                if (DEBUG_TCP)
                    Log.d(TAG, super.toStringOutgoing() + "Processed TCP Packet (ACK) in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
                else
                    Log.d(TAG, super.toStringOutgoing() + "Processed TCP Packet (ACK) in " + state.toString());

            state = TCPState.SOCKET_ACTIVE;
        }
        else
        {
            if (DEBUG_TCP)
                Log.e(TAG, super.toStringOutgoing() + "Waited for TCP Packet (ACK) in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
            else
                Log.e(TAG, super.toStringOutgoing() + "Waited for TCP Packet (ACK) in " + state.toString());

            state = TCPState.CLOSED;
            close();
        }
    }

    private void handlingActiveConnection(TCPPacket tcpPacket)
    {
        if (tcpPacket.isRSTFlagSet())
        {
            handleReset(tcpPacket);
            return;
        }
        if (tcpPacket.isFINFlagSet())
        {
            // simulate closing
            if (DEBUG)
                if (DEBUG_TCP)
                    Log.d(TAG, super.toStringOutgoing() + "Processed TCP Packet (FIN) in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
                else
                    Log.d(TAG, super.toStringOutgoing() + "Processed TCP Packet (FIN) in " + state.toString());

            state = TCPState.APPLICATION_CLOSING;
            handlingApplicationClosing();
            return;
        }

        // forward package if necessary
        if (tcpPacket.getPayloadLength() > 0)
        {
            remoteAckNr = addTCPPacketToField(remoteAckNr, tcpPacket);
            tcpPacket.getRawPacket().position(tcpPacket.getPayloadStart());
            handleWriteOutgoingPacket(tcpPacket);
            if (DEBUG)
                if (DEBUG_TCP)
                    Log.v(TAG, super.toStringOutgoing() + "Forwarded TCP Packet (Length: " + tcpPacket.getIPPacket().getTotalLength() + ") in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
                else
                    Log.v(TAG, super.toStringOutgoing() + "Forwarded TCP Packet (Length: " + tcpPacket.getIPPacket().getTotalLength() + ") in " + state.toString());
        }
    }

    private void handlingApplicationClosing()
    {
        // receiver acknowledges close with FIN, ACK
        remoteAckNr = increaseField(remoteAckNr);
        addIncomingPacketToTunIn(IPPacketFactory.createTCPFlagPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, remoteAckNr, getLocalWindowSize(), TCPPacket.Flag.FIN, TCPPacket.Flag.ACK));
        if (DEBUG)
            if (DEBUG_TCP)
                Log.d(TAG, super.toStringOutgoing() + "Simulated TCP Packet (FIN,ACK) in " + state.toString() + " [" + remoteSeqNr + "|" + remoteAckNr + "]");
            else
                Log.d(TAG, super.toStringOutgoing() + "Simulated TCP Packet (FIN,ACK) in " + state.toString());

        state = TCPState.CLOSED;
        close();
    }

    private void handlingReceiverClosing(TCPPacket tcpPacket)
    {
        if (tcpPacket.isRSTFlagSet())
        {
            handleReset(tcpPacket);
            return;
        }
        if (tcpPacket.isFINFlagSet() && tcpPacket.isACKFlagSet())
        {
            // simulate closing
            if (DEBUG)
                if (DEBUG_TCP)
                    Log.d(TAG, super.toStringOutgoing() + "Processed TCP Packet (FIN,ACK) in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
                else
                    Log.d(TAG, super.toStringOutgoing() + "Processed TCP Packet (FIN,ACK) in " + state.toString());

            state = TCPState.RECEIVER_CLOSED;
            handlingReceiverClosed();
        }
        else
        {
            // drop packet
            if (DEBUG)
                if (DEBUG_TCP)
                    Log.w(TAG, super.toStringOutgoing() + "Ignoring TCP Packet in " + state.toString() + " [" + tcpPacket.getSequenceNumber() + "|" + tcpPacket.getAcknowledgmentNumber() + "]");
                else
                    Log.w(TAG, super.toStringOutgoing() + "Ignoring TCP Packet in " + state.toString());

            // simulate ACK to application if necessary
            if (tcpPacket.getPayloadLength() > 0)
            {
                remoteAckNr = addTCPPacketToField(remoteAckNr, tcpPacket);
                addIncomingPacketToTunIn(IPPacketFactory.createTCPFlagPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, remoteAckNr, getLocalWindowSize(), TCPPacket.Flag.ACK));
                if (DEBUG)
                    if (DEBUG_TCP)
                        Log.v(TAG, super.toStringOutgoing() + "Simulated TCP Packet (ACK) in " + state.toString() + " [" + remoteSeqNr + "|" + remoteAckNr + "]");
                    else
                        Log.v(TAG, super.toStringOutgoing() + "Simulated TCP Packet (ACK) in " + state.toString());
            }


        }
    }

    private void handlingReceiverClosed()
    {
        // receiver acknowledges close with ACK
        addIncomingPacketToTunIn(IPPacketFactory.createTCPFlagPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, remoteAckNr, getLocalWindowSize(), TCPPacket.Flag.ACK));
        if (DEBUG)
            if (DEBUG_TCP)
                Log.d(TAG, super.toStringOutgoing() + "Simulated TCP Packet (ACK) in " + state.toString() + " [" + remoteSeqNr + "|" + remoteAckNr + "]");
            else
                Log.d(TAG, super.toStringOutgoing() + "Simulated TCP Packet (ACK) in " + state.toString());

        remoteSeqNr = increaseField(remoteSeqNr);

        state = TCPState.CLOSED;
        close();
    }

    private void handlingClosedState(TCPPacket tcpPacket)
    {
        if (tcpPacket.isRSTFlagSet())
        {
            handleReset(tcpPacket);
            return;
        }
        Log.w(TAG, super.toStringOutgoing() + "Forwarder should already be closed!");
        try
        {
            if (socketChannel.isOpen())
                socketChannel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // call close, just to be sure
        close();
    }

    public void handleWriteOutgoingPacket(TransportLayerPacket packet)
    {
        if (state == TCPState.SOCKET_ACTIVE)
        {
            try
            {
                while (packet.getRawPacket().hasRemaining())
                {
                    //remoteAckNr = remoteAckNr + socketChannel.write(outgoingBuffer);
                    socketChannel.write(packet.getRawPacket().getByteBuffer());
                }
                // simulate ACK to application
                addIncomingPacketToTunIn(IPPacketFactory.createTCPFlagPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, remoteAckNr, getLocalWindowSize(), TCPPacket.Flag.ACK));
                if (DEBUG)
                    if (DEBUG_TCP)
                        Log.v(TAG, super.toStringOutgoing() + "Simulated TCP Packet (ACK) in " + state.toString() + " [" + remoteSeqNr + "|" + remoteAckNr + "]");
                    else
                        Log.v(TAG, super.toStringOutgoing() + "Simulated TCP Packet (ACK) in " + state.toString());
            }
            catch (IOException e)
            {
                e.printStackTrace();
                close();
            }
        }
        else
        {
            Log.e(TAG, super.toStringOutgoing() + "handleWrite failed (" + state.toString() + ")");
            close();
        }
    }

    private void handleEOF()
    {
        if (DEBUG)
            Log.v(TAG, super.toStringOutgoing() + "handleRead detected EOF (" + state.toString() + ")");
        // simulate received FIN
        addIncomingPacketToTunIn(IPPacketFactory.createTCPFlagPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, remoteAckNr, getLocalWindowSize(), TCPPacket.Flag.ACK, TCPPacket.Flag.FIN));

        if (DEBUG)
            if (DEBUG_TCP)
                Log.d(TAG, super.toStringOutgoing() + "Simulated TCP Packet (FIN, ACK) in " + state.toString() + " [" + remoteSeqNr + "|" + remoteAckNr + "]");
            else
                Log.d(TAG, super.toStringOutgoing() + "Simulated TCP Packet (FIN, ACK) in " + state.toString());
        remoteAckNr = increaseField(remoteAckNr);
        remoteSeqNr = increaseField(remoteSeqNr);

        state = TCPState.RECEIVER_CLOSING;
    }

    @Override
    public void handleReadIncomingPacket()
    {
        while (incomingBuffer.hasRemaining())
        {
            IPPacket ipPacket = IPPacketFactory.createTCPDataPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, remoteAckNr, getLocalWindowSize(), incomingBuffer, maximumSegmentSize, TCPPacket.Flag.ACK, TCPPacket.Flag.PSH);

            applicationWindowSize -= ((TCPPacket) ipPacket.getPayload()).getPayloadLength();
            addIncomingPacketToTunIn(ipPacket);
            remoteSeqNr = addIPPacketToField(remoteSeqNr, ipPacket);
            if (DEBUG)
                if (DEBUG_TCP)
                    Log.v(TAG, super.toStringOutgoing() + "Forwarded TCP Packet (Length: " + ipPacket.getTotalLength() + ") in " + state.toString() + " [" + ((TCPPacket) ipPacket.getPayload()).getSequenceNumber() + "|" + ((TCPPacket) ipPacket.getPayload()).getAcknowledgmentNumber() + "]");
                else
                    Log.v(TAG, super.toStringOutgoing() + "Forwarded TCP Packet (Length: " + ipPacket.getTotalLength() + ") in " + state.toString());
        }

        incomingBuffer.clear();
    }

    private void flushIncomingBuffer()
    {
        if (incomingBuffer == null)
        {
            if (DEBUG)
                Log.w(TAG, super.toStringIncoming() + "No incomingBuffer");
            return;
        }
        incomingBuffer.flip();
        while (incomingBuffer.hasRemaining())
        {
            IPPacket ipPacket = IPPacketFactory.createTCPDataPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, remoteAckNr, getLocalWindowSize(), incomingBuffer, maximumSegmentSize, TCPPacket.Flag.ACK, TCPPacket.Flag.PSH);

            applicationWindowSize -= ((TCPPacket) ipPacket.getPayload()).getPayloadLength();
            addIncomingPacketToTunIn(ipPacket);
            remoteSeqNr = addIPPacketToField(remoteSeqNr, ipPacket);

            if (DEBUG)
                Log.v(TAG, super.toStringIncoming() + "Received TCP Packet (Length: " + ipPacket.getTotalLength() + ")");
        }
    }

    @Override
    public void close()
    {
        try
        {
            flushIncomingBuffer();

            if (DEBUG)
                Log.i(TAG, super.toStringOutgoing() + "Closing TCP connection...");

            socketChannel.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, super.toStringOutgoing() + "Closing TCP connection failed.");
            e.printStackTrace();
            kill();
        }
        super.close();
    }

    @Override
    public void kill()
    {
        if (DEBUG)
            Log.i(TAG, super.toStringOutgoing() + "Killing TCP connection...");
        state = TCPState.CLOSED;
        try
        {
            socketChannel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.e(TAG, super.toStringOutgoing() + "Killing TCP connection failed.");
        }

    }

    @Override
    public void handleError(IOException e)
    {
        e.printStackTrace();
    }

    @Override
    public void handleConnect()
    {
        try
        {
            socketChannel.socket().setReceiveBufferSize(incomingBuffer.capacity());
            socketChannel.socket().setKeepAlive(true);

            remoteAckNr = increaseField(remoteAckNr);
            addIncomingPacketToTunIn(IPPacketFactory.createTCPFlagPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, remoteAckNr, getLocalWindowSize(), TCPPacket.Flag.SYN, TCPPacket.Flag.ACK));
            if (DEBUG)
                if (DEBUG_TCP)
                    Log.d(TAG, super.toStringOutgoing() + "Simulated TCP Packet (SYN,ACK) in " + state.toString() + " [" + remoteSeqNr + "|" + remoteAckNr + "]");
                else
                    Log.d(TAG, super.toStringOutgoing() + "Simulated TCP Packet (SYN,ACK) in " + state.toString());
            remoteSeqNr = increaseField(remoteSeqNr);

            state = TCPState.SOCKET_CREATED;
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
    }

    public int getApplicationWindowSize()
    {
        return applicationWindowSize;
    }

    private int getLocalWindowSize()
    {
        return WINDOW_SIZE;
    }

    private boolean sanityCheck(TCPPacket tcpPacket) throws Exception
    {
        if (tcpPacket.getSequenceNumber() == applicationSeqNr)
        {
            applicationSeqNr = addTCPPacketToField(applicationSeqNr, tcpPacket);
            errorCounter = 0;
            if (DEBUG && DEBUG_TCP)
                Log.v(TAG, super.toStringOutgoing() + "Sanity: Healthy");

            return true;
        }

        if (tcpPacket.getSequenceNumber() < applicationSeqNr)
        {
            errorCounter++;
            if (DEBUG)
            {
                Log.i(TAG, super.toStringOutgoing() + "Sanity: Detected retransmission [" + tcpPacket.getSequenceNumber() + "|" + applicationSeqNr + "]");
                if (DEBUG_TCP)
                    Log.v(TAG, tcpPacket.getIPPacket().toString());
            }
            return false;
        }

        if (tcpPacket.getSequenceNumber() > applicationSeqNr)
        {
            addIncomingPacketToTunIn(IPPacketFactory.createTCPFlagPacket(getRemoteIP(), getRemotePort(), getLocalIP(), getLocalPort(), remoteSeqNr, applicationSeqNr, getLocalWindowSize(), TCPPacket.Flag.ACK));
            errorCounter++;
            if (DEBUG)
            {
                Log.d(TAG, super.toStringOutgoing() + "Sanity: Detected missing packets [" + tcpPacket.getSequenceNumber() + "|" + applicationSeqNr + "]");
                if (DEBUG_TCP)
                    Log.v(TAG, tcpPacket.getIPPacket().toString());
            }
            return false;
        }

        throw new Exception("Should never be here!");
    }

    @Override
    public String toString()
    {
        if (DEBUG_TCP)
            return super.toString() + "\nTCP-State: " + state + " (" + applicationSeqNr + ", " + remoteSeqNr + ", " + remoteAckNr + ")";
        else
            return super.toString() + "\nTCP-State: " + state;
    }

    public enum TCPState
    {
        CONNECTING(false), SOCKET_CREATED(false), SOCKET_ACTIVE(true), APPLICATION_CLOSING(false), RECEIVER_CLOSING(false), RECEIVER_CLOSED(false), CLOSED(false);

        private final boolean canReceive;

        TCPState(boolean canReceive)
        {
            this.canReceive = canReceive;
        }

        private boolean canReceive()
        {
            return canReceive;
        }
    }

}
