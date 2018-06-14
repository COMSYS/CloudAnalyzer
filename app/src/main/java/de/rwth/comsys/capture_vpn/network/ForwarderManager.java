package de.rwth.comsys.capture_vpn.network;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.util.CaptureConstants;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * handles all created forwarders (for TCP and UDP)
 */

public class ForwarderManager
{
    private static final String TAG = "ForwarderManager";

    private static ForwarderManager forwarderManager = new ForwarderManager();
    private ConcurrentHashMap<TransportLayerPacket.Protocol, ForwarderHandler> protocolMap;
    private int tcpForwarder;
    private int udpForwarder;
    private long tcpBuffer;
    private long udpBuffer;


    public ForwarderManager()
    {
        this.protocolMap = new ConcurrentHashMap<>();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(CaptureCentral.getInstance().getContext());
        tcpForwarder = (int) Math.floor(CaptureConstants.USABLE_FORWARDER * Integer.parseInt(sharedPrefs.getString("pref_tweaks_tcpforwarder", String.valueOf(CaptureConstants.TCP_FORWARDER))) / 100);
        udpForwarder = (int) Math.floor(CaptureConstants.USABLE_FORWARDER * Integer.parseInt(sharedPrefs.getString("pref_tweaks_udpforwarder", String.valueOf(CaptureConstants.UDP_FORWARDER))) / 100);
        tcpBuffer = Long.parseLong(sharedPrefs.getString("pref_tweaks_tcpbuffer", String.valueOf(CaptureConstants.TCP_BUFFER)));
        udpBuffer = Long.parseLong(sharedPrefs.getString("pref_tweaks_udpbuffer", String.valueOf(CaptureConstants.UDP_BUFFER)));

    }

    public static ForwarderManager getInstance()
    {
        return forwarderManager;
    }

    public void resetForwarderManager()
    {
        for (ForwarderHandler handler : protocolMap.values())
        {
            handler.resetForwarderInformation();
        }
    }

    public void addForwarder(Forwarder forwarder)
    {
        TransportLayerPacket.Protocol protocol = forwarder.getProtocol();
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
        {
            handler = new ForwarderHandler();
            protocolMap.put(protocol, handler);
        }

        handler.addForwarder(forwarder);
    }

    public Forwarder getForwarder(TransportLayerPacket.Protocol protocol, IPPacket.IPKey key)
    {
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
        {
            return null;
        }
        return handler.getForwarder(key);
    }

    public void accessedForwarder(Forwarder forwarder)
    {
        TransportLayerPacket.Protocol protocol = forwarder.getProtocol();
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null || !handler.contains(forwarder))
        {
            Log.w(TAG, "Forwarder not found, not updating, possibly removed");
            return;
        }

        handler.accessedForwarder(forwarder.getKey());
    }

    public void removeForwarder(Forwarder forwarder)
    {
        TransportLayerPacket.Protocol protocol = forwarder.getProtocol();
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null || !handler.contains(forwarder))
        {
            Log.w(TAG, "Forwarder not found, already removed?");
            return;
        }

        handler.removeForwarder(forwarder.getKey());
    }

    public ConcurrentHashMap<IPPacket.IPKey, Forwarder> getForwarder()
    {
        ConcurrentHashMap<IPPacket.IPKey, Forwarder> forwarderMap = new ConcurrentHashMap<>();
        for (ForwarderHandler handler : protocolMap.values())
        {
            forwarderMap.putAll(handler.getForwarderMap());
        }
        return forwarderMap;
    }

    public int getNumberOfForwarder()
    {
        int count = 0;
        for (ForwarderHandler handler : protocolMap.values())
        {
            count += handler.size();
        }
        return count;
    }

    public int getNumberOfForwarder(TransportLayerPacket.Protocol protocol)
    {
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
            return 0;
        return handler.size();
    }

    public long getData_incoming()
    {
        int count = 0;
        for (ForwarderHandler handler : protocolMap.values())
        {
            count += handler.getData_incoming();
        }
        return count;
    }

    public long getData_incoming(TransportLayerPacket.Protocol protocol)
    {
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
            return 0;
        return handler.getData_incoming();
    }

    public long getData_outgoing()
    {
        int count = 0;
        for (ForwarderHandler handler : protocolMap.values())
        {
            count += handler.getData_outgoing();
        }
        return count;
    }

    public long getData_outgoing(TransportLayerPacket.Protocol protocol)
    {
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
            return 0;
        return handler.getData_outgoing();
    }

    public long getPackets_incoming()
    {
        int count = 0;
        for (ForwarderHandler handler : protocolMap.values())
        {
            count += handler.getPackets_incoming();
        }
        return count;
    }

    public long getPackets_incoming(TransportLayerPacket.Protocol protocol)
    {
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
            return 0;
        return handler.getPackets_incoming();
    }

    public long getPackets_outgoing()
    {
        int count = 0;
        for (ForwarderHandler handler : protocolMap.values())
        {
            count += handler.getPackets_outgoing();
        }
        return count;
    }

    public long getPackets_outgoing(TransportLayerPacket.Protocol protocol)
    {
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
            return 0;
        return handler.getPackets_outgoing();
    }

    public void addPacketInformation(IPPacket packet)
    {
        TransportLayerPacket.Protocol protocol = TransportLayerPacket.Protocol.fromProtocolID(packet.getProtocol());
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
        {
            Log.w(TAG, "Forwarder not found, not updating information");
            return;
        }
        handler.addPacketInformation(packet);
    }

    public boolean checkForNewForwarder(TransportLayerPacket.Protocol protocol)
    {
        if (getNumberOfForwarder() >= CaptureConstants.USABLE_FORWARDER)
            return false;
        switch (protocol)
        {
            case TCP:
                if (getNumberOfForwarder(protocol) >= tcpForwarder)
                {
                    Log.i(TAG, "Too many existing TCP forwarder");
                    return false;
                }
                break;
            case UDP:
                if (getNumberOfForwarder(protocol) >= udpForwarder)
                {
                    Log.i(TAG, "Too many existing UDP forwarder");
                    return false;
                }
                break;
        }
        return true;

    }

    public void cleanUpExistingForwarder(TransportLayerPacket.Protocol protocol, boolean safe, long buffer)
    {
        ForwarderHandler handler = protocolMap.get(protocol);
        if (handler == null)
        {
            //Log.v(TAG, "Could not clean up forwarder for protocol " + protocol.toString());
            return;
        }

        if (safe)
        {
            handler.removeClosedForwarder();
        }
        else
        {
            handler.removeOldForwarder(buffer);
        }
    }

    public void cleanUpExistingForwarder(boolean force)
    {
        if (force || getNumberOfForwarder(TransportLayerPacket.Protocol.TCP) >= tcpForwarder)
        {
            //cleanUpExistingForwarder(TransportLayerPacket.Protocol.TCP, true, 0);
            //if (force || getNumberOfForwarder(TransportLayerPacket.Protocol.TCP) >= tcpForwarder)
            cleanUpExistingForwarder(TransportLayerPacket.Protocol.TCP, false, tcpBuffer);
        }
        if (force || getNumberOfForwarder(TransportLayerPacket.Protocol.UDP) >= udpForwarder)
        {
            //cleanUpExistingForwarder(TransportLayerPacket.Protocol.UDP, true, 0);
            //if (force || getNumberOfForwarder(TransportLayerPacket.Protocol.UDP) >= udpForwarder)
            cleanUpExistingForwarder(TransportLayerPacket.Protocol.UDP, false, udpBuffer);
        }
    }

    public void cleanUpExistingForwarder(TransportLayerPacket.Protocol protocol, long buffer)
    {
        cleanUpExistingForwarder(protocol, false, buffer);
    }


    public class ForwarderHandler
    {
        private ConcurrentHashMap<IPPacket.IPKey, ForwarderConnectionInfo> forwarderMap;
        private long data_incoming;
        private long data_outgoing;
        private long packets_incoming;
        private long packets_outgoing;

        public ForwarderHandler()
        {
            this.forwarderMap = new ConcurrentHashMap<>();

            this.data_incoming = 0;
            this.data_outgoing = 0;
            this.packets_incoming = 0;
            this.packets_outgoing = 0;
        }

        public boolean contains(Forwarder forwarder)
        {
            return this.contains(forwarder.getKey());
        }

        public boolean contains(IPPacket.IPKey key)
        {
            return this.forwarderMap.containsKey(key);
        }

        public int size()
        {
            return this.forwarderMap.size();
        }

        public long getData_incoming()
        {
            return this.data_incoming;
        }

        public long getData_outgoing()
        {
            return this.data_outgoing;
        }

        public long getPackets_incoming()
        {
            return this.packets_incoming;
        }

        public long getPackets_outgoing()
        {
            return this.packets_outgoing;
        }

        public void addForwarder(Forwarder forwarder)
        {
            this.forwarderMap.put(forwarder.getKey(), new ForwarderConnectionInfo(forwarder));
        }

        public Forwarder getForwarder(IPPacket.IPKey key)
        {
            ForwarderConnectionInfo connInfo = this.forwarderMap.get(key);
            if (connInfo == null)
            {
                return null;
            }
            return connInfo.getForwarder();
        }

        public void accessedForwarder(IPPacket.IPKey key)
        {
            ForwarderConnectionInfo connInfo = this.forwarderMap.get(key);
            if (connInfo == null)
                Log.v(TAG, "Forwarder access updated failed");
            else
                connInfo.updateAccess();
        }

        public void removeForwarder(IPPacket.IPKey key)
        {
            ForwarderConnectionInfo connInfo = this.forwarderMap.get(key);
            if (connInfo == null)
                Log.v(TAG, "Forwarder already removed?");
            else
                connInfo.remove();
            this.forwarderMap.remove(key);
        }

        public void addPacketInformation(IPPacket packet)
        {
            if (packet.isIncoming())
            {
                data_incoming += packet.getTotalLength();
                packets_incoming++;
            }
            else
            {
                data_outgoing += packet.getTotalLength();
                packets_outgoing++;
            }
        }

        public void removeClosedForwarder()
        {
            for (IPPacket.IPKey key : forwarderMap.keySet())
            {
                ForwarderConnectionInfo connInfo = forwarderMap.get(key);
                if (connInfo == null || connInfo.removeIfPossible())
                    forwarderMap.remove(key);
            }
        }

        public void removeOldForwarder(long buffer)
        {
            for (IPPacket.IPKey key : forwarderMap.keySet())
            {
                ForwarderConnectionInfo connInfo = forwarderMap.get(key);
                if (connInfo == null)
                {
                    forwarderMap.remove(key);
                    continue;
                }
                if (!connInfo.checkValid(buffer))
                {
                    Log.i(TAG, "Removing old " + connInfo.getForwarder().protocol.toString() + " forwarder");
                    connInfo.closeAndRemove();
                    //forwarderMap.remove(key);
                }
            }
        }


        public void resetForwarderInformation()
        {
            for (ForwarderConnectionInfo connInfo : forwarderMap.values())
            {
                connInfo.closeAndRemove();
            }
            this.forwarderMap.clear();

            this.data_incoming = 0;
            this.data_outgoing = 0;
            this.packets_incoming = 0;
            this.packets_outgoing = 0;
        }


        public HashMap<IPPacket.IPKey, Forwarder> getForwarderMap()
        {
            HashMap<IPPacket.IPKey, Forwarder> forwarderMergedMap = new HashMap<>();
            for (ForwarderConnectionInfo connInfo : forwarderMap.values())
            {
                Forwarder forwarder = connInfo.getForwarder();
                forwarderMergedMap.put(forwarder.getKey(), forwarder);
            }
            return forwarderMergedMap;
        }
    }

    public class ForwarderConnectionInfo
    {
        private Forwarder forwarder;
        private volatile long lastAccess;

        public ForwarderConnectionInfo(Forwarder forwarder)
        {
            this.forwarder = forwarder;
            this.lastAccess = System.currentTimeMillis();
        }

        public void updateAccess()
        {
            this.lastAccess = System.currentTimeMillis();
        }

        public boolean checkValid(long puffer)
        {
            return (this.lastAccess > System.currentTimeMillis() - puffer);
        }

        public Forwarder getForwarder()
        {
            return this.forwarder;
        }

        public void remove()
        {
        }

        public void closeAndRemove()
        {
            forwarder.close();
        }

        public boolean removeIfPossible()
        {
            if (forwarder.isClosed())
            {
                closeAndRemove();
                return true;
            }
            return false;
        }
    }
}
