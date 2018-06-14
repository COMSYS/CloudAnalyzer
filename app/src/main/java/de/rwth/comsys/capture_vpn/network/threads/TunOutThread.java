package de.rwth.comsys.capture_vpn.network.threads;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.network.*;
import de.rwth.comsys.capture_vpn.util.AppLookup;
import de.rwth.comsys.capture_vpn.util.ByteBufferWrapper;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static de.rwth.comsys.capture_vpn.util.CaptureConstants.DEBUG;

/**
 * Thread that handles outgoing packets
 * <p>
 * an applications sends data that is assigned to a forwarder to handle incoming traffic
 */

public class TunOutThread extends Thread
{

    private static final String TAG = "TunOutThread";
    private FileInputStream tunOut;
    private volatile boolean exit;
    private CaptureCentral captureCentral;
    private ForwarderManager forwarderManager;
    private TimerTask cleanupTask;
    private short[] tunAddr;
    private int sleepTime;
    private long cleanupPeriod;

    public TunOutThread(FileDescriptor tunFd)
    {
        super(TAG);
        this.tunOut = new FileInputStream(tunFd);
        this.setDaemon(false);
        this.exit = false;
        this.captureCentral = CaptureCentral.getInstance();
        this.forwarderManager = this.captureCentral.getForwarderManager();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(captureCentral.getContext());
        String sharedTunAddr = sharedPrefs.getString("pref_tweaks_tunipv4", CaptureConstants.TUN_ADDR_STRING);
        tunAddr = new short[4];
        for (int i = 0; i < 4; i++)
        {
            tunAddr[i] = Short.parseShort(sharedTunAddr.split("\\.")[i]);
        }
        sleepTime = Integer.parseInt(sharedPrefs.getString("pref_tweaks_sleeptime", CaptureConstants.SLEEP_TIME));
        cleanupPeriod = Math.min(Long.parseLong(sharedPrefs.getString("pref_tweaks_tcpbuffer", String.valueOf(CaptureConstants.TCP_BUFFER))), Long.parseLong(sharedPrefs.getString("pref_tweaks_udpbuffer", String.valueOf(CaptureConstants.UDP_BUFFER))));

        cleanupTask = new TimerTask()
        {

            @Override
            public void run()
            {
                if (!exit)
                {
                    // clean up sockets, even if not all file descriptors are taken
                    forwarderManager.cleanUpExistingForwarder(true);
                    Log.d(TAG, "Executed periodic forwarder cleanup");
                }
            }
        };

    }

    public FileInputStream getTunOut()
    {
        return this.tunOut;
    }

    private void addPacketToTunIn(IPPacket packet)
    {
        captureCentral.getTunInQueue().offer(packet);
    }

    private void handleUnexpectedTCPPacket(IPPacket packet)
    {
        addPacketToTunIn(IPPacketFactory.createTCPFlagPacket(packet.getDstIP(), packet.getPayload().getDstPort(), packet.getSrcIP(), packet.getPayload().getSrcPort(), ((TCPPacket) packet.getPayload()).getAcknowledgmentNumber(), TCPForwarder.addIPPacketToField(((TCPPacket) packet.getPayload()).getSequenceNumber(), packet), 0, TCPPacket.Flag.ACK, TCPPacket.Flag.RST));
        if (DEBUG)
            Log.d(TAG, "Simulated TCP Packet (RST) for unexpected TCP Packet (Length: " + packet.getTotalLength() + ")");

    }

    private boolean checkLocalAddress(IPPacket packet)
    {
        short[] srcIP = packet.getSrcIP();
        if (!Arrays.equals(tunAddr, srcIP))
        {
            TransportLayerPacket.Protocol prot = packet.getPayload().getProtocol();

            if (DEBUG)
                Log.i(TAG, "Received packet (" + prot.toString() + ") with (wrong) source addr: " + Short.toString(srcIP[0]) + "." + Short.toString(srcIP[1]) + "." + Short.toString(srcIP[2]) + "." + Short.toString(srcIP[3]));

            switch (prot)
            {
                case TCP:
                    // Send RST to application
                    addPacketToTunIn(IPPacketFactory.createTCPFlagPacket(packet.getDstIP(), packet.getPayload().getDstPort(), srcIP, packet.getPayload().getSrcPort(), ((TCPPacket) packet.getPayload()).getAcknowledgmentNumber(), TCPForwarder.addIPPacketToField(((TCPPacket) packet.getPayload()).getSequenceNumber(), packet), 0, TCPPacket.Flag.ACK, TCPPacket.Flag.RST));
                    if (DEBUG)
                        Log.d(TAG, "Simulated TCP Packet (RST) for (wrong) source addr: " + Short.toString(srcIP[0]) + "." + Short.toString(srcIP[1]) + "." + Short.toString(srcIP[2]) + "." + Short.toString(srcIP[3]));
                    break;
                case UDP:
                    // send ICMP to application
                    int oldPosition = packet.getRawPacket().position();
                    packet.getRawPacket().position(0);
                    addPacketToTunIn(IPPacketFactory.createICMPDestUnreachablePacket(packet.getDstIP(), packet.getSrcIP(), packet.getRawPacket().getByteBuffer(), (packet.getIhl() * 4) + 8)); // old IP Header + 8 Byte of Transport Layer Header
                    packet.getRawPacket().position(oldPosition);
                    if (DEBUG)
                        Log.d(TAG, "Simulated ICMP Packet (Destination Unreachable) for (wrong) source addr: " + Short.toString(srcIP[0]) + "." + Short.toString(srcIP[1]) + "." + Short.toString(srcIP[2]) + "." + Short.toString(srcIP[3]));
                    break;
            }
            return false;
        }
        return true;
    }

    private Forwarder createNewForwarder(IPPacket packet)
    {
        Forwarder forwarder = null;
        TransportLayerPacket.Protocol protocol = packet.getPayload().getProtocol();

        if (!forwarderManager.checkForNewForwarder(protocol))
        {
            forwarderManager.cleanUpExistingForwarder(false);
            if (!forwarderManager.checkForNewForwarder(protocol))
                return null;
        }

        try
        {
            switch (protocol)
            {
                case TCP:
                    if (((TCPPacket) packet.getPayload()).isSYNFlagSet())
                    {
                        //Log.i(TAG, "Created TCPForwarder");
                        forwarder = new TCPForwarder(((TCPPacket) packet.getPayload()));
                    }
                    else if (((TCPPacket) packet.getPayload()).getPayloadLength() > 0)
                        handleUnexpectedTCPPacket(packet);
                    else if (DEBUG)
                        Log.i(TAG, "Dropped TCP packet (non-SYN) without existing forwarder");
                    break;
                case UDP:
                    //Log.i(TAG, "Created UDPForwarder");
                    forwarder = new UDPForwarder(((UDPPacket) packet.getPayload()));
                    break;
            }
        }
        catch (IOException e)
        {
            Log.w(TAG, "Could not create Forwarder:\n" + e);
        }
        return forwarder;
    }

    public void run()
    {
        // schedule cleanup timer at fixed interval
        Timer cleanupTimer = new Timer();
        cleanupTimer.scheduleAtFixedRate(cleanupTask, cleanupPeriod, cleanupPeriod);

        ByteBuffer packetBuffer = ByteBuffer.allocateDirect(65535);

        while (!exit)
        {
            packetBuffer.clear();

            // we received a new outgoing packet and have to
            //  a) match it to a forwarder    or
            //  b) create a new forwarder
            try
            {
                int length = getTunOut().read(packetBuffer.array());

                if (length > 0 && packetBuffer.get(0) != 0)
                {
                    packetBuffer.limit(length);

                    ByteBuffer outgoingBuffer = ByteBuffer.allocate(length);
                    System.arraycopy(packetBuffer.array(), 0, outgoingBuffer.array(), 0, length);

                    IPPacket packet = IPPacket.parse(new ByteBufferWrapper(outgoingBuffer));
                    packet.setCount(IPPacket.getPacketCount());
                    packet.setIncoming(false);
                    packet.setForeground(CaptureCentral.isScreenOn());
                    packet.setNetworkType(CaptureCentral.getNetworkType());

                    CaptureCentral.addPacketToPacketQueue(packet);
                    CaptureCentral.addPacketToPCAP(packet);

                    if (packet.getPayload() != null)
                    {
                        TransportLayerPacket.Protocol prot = packet.getPayload().getProtocol();
                        if (prot != null)
                        {
                            if (!checkLocalAddress(packet))
                                continue;

                            Forwarder forwarder = forwarderManager.getForwarder(prot, packet.getKey());
                            if (forwarder == null)
                            {
                                forwarder = createNewForwarder(packet);
                                if (forwarder != null)
                                    captureCentral.addForwarder(forwarder, AppLookup.getAppNameByPort(captureCentral.getContext(), packet.getPayload().getSrcPort(), forwarder.getProtocol()));
                            }
                            else
                            {
                                // enqueue the packet and AFTERWARDS register the forwarder
                                forwarder.getTunOutQueue().offer(packet.getPayload());
                                forwarder.registerForUpdate();
                            }
                        }
                        else
                        {
                            Log.w(TAG, "Protocol " + packet.getProtocol() + " not supported");
                            continue;
                        }
                    }
                }
                else
                {
                    try
                    {
                        Thread.sleep(sleepTime);
                    }
                    catch (InterruptedException e)
                    {
                        Log.w(TAG, "interrupted");
                        break;
                    }
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                break;
            }
        }
        try
        {
            getTunOut().close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        Log.i(TAG, "shut down");
    }


    public void exit()
    {
        this.exit = true;
    }
}
