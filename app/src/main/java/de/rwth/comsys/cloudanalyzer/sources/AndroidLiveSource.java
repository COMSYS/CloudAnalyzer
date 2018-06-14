package de.rwth.comsys.cloudanalyzer.sources;

import android.net.ConnectivityManager;
import de.rwth.comsys.capture_vpn.CaptureCentral;
import de.rwth.comsys.capture_vpn.network.IPPacket;
import de.rwth.comsys.cloudanalyzer.MainHandler;
import de.rwth.comsys.cloudanalyzer.information.Information;
import de.rwth.comsys.cloudanalyzer.information.NetworkPacket;
import de.rwth.comsys.cloudanalyzer.network.AbstractPacket;
import de.rwth.comsys.cloudanalyzer.network.NetworkProtocol;
import de.rwth.comsys.cloudanalyzer.network.Packet;
import de.rwth.comsys.cloudanalyzer.network.trafficProperty.*;

import java.util.concurrent.LinkedBlockingQueue;


public class AndroidLiveSource implements NetworkPacketSource
{
    private final static String TAG = "NetworkPacketSource";

    private LinkedBlockingQueue<IPPacket> packetQueue;

    public AndroidLiveSource()
    {
        packetQueue = CaptureCentral.getInstance().getPacketQueue();
    }

    @Override
    public Information nextInformation()
    {
        IPPacket packet = packetQueue.poll();

        if (packet != null)
        {
            return new NetworkPacket(getPacket(packet));
        }

        return null;
    }

    @Override
    public boolean entireInformationProcessed()
    {
        return false;
    }

    @Override
    public void close()
    {
        //clean up capturing
    }

    @Override
    public Object getSyncObject()
    {
        return CaptureCentral.getSyncPacketSource();
    }

    @Override
    public boolean isLive()
    {
        return true;
    }

    public Packet getPacket(IPPacket packet)
    {
        return new AndroidPacket(packet);
    }

    private static class AndroidPacket extends AbstractPacket
    {
        private IPPacket packet;

        public AndroidPacket(IPPacket packet)
        {
            super(getTrafficProperty(packet));
            this.packet = packet;
        }

        private static TrafficProperty getTrafficProperty(IPPacket packet)
        {
            Link linkProperty;
            switch (packet.getNetworkType())
            {
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_MOBILE_DUN:
                    linkProperty = Link.CELLULAR;
                    break;
                case ConnectivityManager.TYPE_WIFI:
                    linkProperty = Link.WIFI;
                    break;

                default:
                    linkProperty = Link.AGGREGATED;
                    break;
            }
            return TrafficPropertiesManager.getProperty((packet.isIncoming() ? FlowDirection.IN : FlowDirection.OUT), (packet.isForeground() ? Importance.FOREGROUND : Importance.BACKGROUND), linkProperty, Protocol.AGGREGATED);
        }

        @Override
        public int getPacketWirelen()
        {
            return packet.getTotalLength();
        }

        @Override
        public int getCaptureLength()
        {
            return packet.getTotalLength();
        }

        @Override
        public long getOriginalFrameNumber()
        {
            return packet.getCount();
        }

        @Override
        public long getTimestampInMillis()
        {
            return packet.getTimestamp();
        }

        @Override
        public boolean hasHeader(NetworkProtocol prot)
        {
            switch (prot)
            {
                case IPv4:
                    return true;

                case IPv6:
                    break;

                case TCP:
                    if (packet.getProtocol() == 6)
                        return true;
                    break;

                case UDP:
                    if (packet.getProtocol() == 17)
                        return true;
                    break;

                case DNS:
                    if ((packet.getPayload().getSrcPort() == 53 || packet.getPayload().getDstPort() == 53) && packet.getProtocol() == 17)
                    {
                        return true;
                    }
                    break;

                default:
                    break;
            }
            return false;
        }

        @Override
        public boolean hasAnyHeader(NetworkProtocol... prots)
        {
            for (NetworkProtocol prot : prots)
            {
                if (hasHeader(prot))
                    return true;
            }

            return false;
        }

        @Override
        public int findHeaderIndex(NetworkProtocol prot)
        {
            switch (prot)
            {
                case IPv4:
                    return 0;

                case TCP:
                case UDP:
                    return 1;

                case DNS:
                    if (packet.getProtocol() == 17 && (packet.getPayload().getSrcPort() == 53 || packet.getPayload().getDstPort() == 53))
                        return 2;

            }

            return -1;
        }

        @Override
        public int findHeaderIndex(NetworkProtocol prot, int instance)
        {
            return findHeaderIndex(prot);
        }

        @Override
        public int getHeaderInstanceCount(NetworkProtocol prot)
        {
            if (hasHeader(prot))
                return 1;

            return 0;
        }

        @Override
        public int getHeaderOffsetByIndex(int index)
        {
            if (index == 1)
            {
                return packet.getPayload().getTransportHeaderOffset();
            }
            else if (index == 2)
            {
                return packet.getPayload().getTransportHeaderOffset() + packet.getPayload().getHeaderLength();
            }

            return 0;
        }

        @Override
        public int getHeaderLengthByIndex(int index)
        {
            if (index == 0)
            {
                return packet.getIhl() * 4;
            }
            else if (index == 1)
            {
                return packet.getPayload().getHeaderLength();
            }

            return 0;
        }

        @Override
        public int getUByte(int index)
        {
            return packet.getRawPacket().getUnsignedByte(index);
        }

        @Override
        public int getUShort(int index)
        {
            return packet.getRawPacket().getUnsignedShort(index);
        }

        @Override
        public long getUInt(int index)
        {
            return packet.getRawPacket().getUnsignedInt(index);
        }

        @Override
        public byte[] getByteArray(int index, int size)
        {
            byte[] temp = new byte[size];
            getByteArray(index, temp, 0, size);
            return temp;
        }

        @Override
        public byte[] getByteArray(int index, byte[] array, int offset, int length)
        {
            System.arraycopy(packet.getRawPacket().getByteBuffer().array(), index, array, offset, length);
            return array;
        }

        public StringBuilder getUTF8String(int index, StringBuilder buf, int length)
        {
            final int len = index + ((packet.getRawPacket().getByteBuffer().array().length < length) ? packet.getRawPacket().getByteBuffer().array().length : length);

            for (int i = index; i < len; i++)
            {
                char c = getUTF8Char(i);
                buf.append(c);
            }

            return buf;
        }

        @Override
        public String getString(int index, int length)
        {
            return getUTF8String(index, new StringBuilder(), length).toString();
        }

        public char getUTF8Char(int index)
        {
            return (char) packet.getRawPacket().getUnsignedByte(index);
        }

        @Override
        public int getApp()
        {
            String appName = CaptureCentral.getInstance().getAppNameManager().get(packet.getKey());
            if (appName == null)
            {
                appName = "Unknown.Application";
                //Log.w(TAG, packet.toString());
            }
            //Log.i(TAG, appName);

            return MainHandler.getAppId(appName);
        }
    }
}