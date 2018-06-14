package de.rwth.comsys.capture_vpn.util;

import de.rwth.comsys.capture_vpn.network.IPPacket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * writes packets to file system (if desired)
 */

public class PCAPFileWriter
{
    private final Lock lock = new ReentrantLock();

    private FileChannel fileChannel;
    private FileOutputStream fileStream;

    public PCAPFileWriter(String path)
    {
        File pcapFile = new File(path);

        try
        {
            pcapFile.createNewFile();
            fileStream = new FileOutputStream(pcapFile);
            fileChannel = fileStream.getChannel();

            writeGlobalHeader();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void writeGlobalHeader() throws IOException
    {
        if (fileChannel == null)
            return;
        ByteBufferWrapper globalHeader = new ByteBufferWrapper(ByteBuffer.allocate(24));
        //magic_number
        globalHeader.putUnsignedInt(0xa1b2c3d4);
        //version_major
        globalHeader.putUnsignedShort(2);
        //version_minor
        globalHeader.putUnsignedShort(4);
        //thiszone
        globalHeader.putInt(0);
        //sigfigs
        globalHeader.putUnsignedInt(0);
        //snaplen
        globalHeader.putUnsignedInt(65535);
        //de.rwth.comsys.cloudanalyzer.capture_vpn.network
        globalHeader.putUnsignedInt(101);

        globalHeader.flip();
        fileChannel.write(globalHeader.getByteBuffer());
    }

    private void writePacketHeader(long timestamp, int packetLength) throws IOException
    {
        if (fileChannel == null)
            return;
        ByteBufferWrapper packetHeader = new ByteBufferWrapper(ByteBuffer.allocate(16));
        //ts_sec
        packetHeader.putUnsignedInt(timestamp / 1000);
        //ts_usec
        packetHeader.putUnsignedInt(timestamp % 1000);
        //incl_len
        packetHeader.putUnsignedInt(packetLength);
        //orig_len
        packetHeader.putUnsignedInt(packetLength);

        packetHeader.flip();
        fileChannel.write(packetHeader.getByteBuffer());
    }


    public void addPacket(ByteBuffer byteBuffer)
    {
        if (fileChannel == null)
            return;
        lock.lock();
        try
        {
            writePacketHeader(System.currentTimeMillis(), byteBuffer.limit());
            fileChannel.write(byteBuffer);

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        lock.unlock();
    }


    public void addPacket(IPPacket ipPacket, boolean fullPacket)
    {
        if (fileChannel == null)
            return;
        lock.lock();
        try
        {
            if (fullPacket)
            {
                writePacketHeader(ipPacket.getTimestamp(), ipPacket.getTotalLength());
                fileChannel.write(ipPacket.getRawPacket().getByteBuffer());
            }
            else
            {
                writePacketHeader(ipPacket.getTimestamp(), ipPacket.getPayload().getPayloadStart());
                fileChannel.write(ipPacket.getRawPacket().getSlice(0, ipPacket.getPayload().getPayloadStart()).getByteBuffer());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        lock.unlock();
    }

    public void close()
    {
        lock.lock();
        try
        {
            fileStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        lock.unlock();
    }
}
