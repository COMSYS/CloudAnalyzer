package de.rwth.comsys.capture_vpn.util;

import java.nio.ByteBuffer;

/**
 * util class for our IP/TCP/UDP implementations
 */

public class ByteBufferWrapper
{
    public static String TAG = "ByteBufferWrapper";

    private ByteBuffer byteBuffer;
    private int internalOffset;

    public ByteBufferWrapper(ByteBuffer byteBuffer)
    {
        this.byteBuffer = byteBuffer;
        this.internalOffset = 0;
    }

    private ByteBufferWrapper(ByteBuffer byteBuffer, int internalOffset)
    {
        this.byteBuffer = byteBuffer;
        this.internalOffset = internalOffset;
        this.byteBuffer.position(internalOffset);
    }

    private ByteBufferWrapper(ByteBuffer byteBuffer, int internalOffset, int length)
    {
        this(byteBuffer, internalOffset);
        this.byteBuffer.limit(internalOffset + length);
    }

    public void putIntoBuffer(ByteBuffer buffer)
    {
        buffer.put(byteBuffer);
    }

    public void putIntoBuffer(ByteBuffer buffer, int length)
    {
        int offset = internalOffset + byteBuffer.arrayOffset() + position();
        buffer.put(byteBuffer.array(), offset, length);
        position(position() + length);
    }

    public ByteBufferWrapper getSlice(int offset)
    {
        return new ByteBufferWrapper(ByteBuffer.wrap(byteBuffer.array()), offset);
    }

    public ByteBufferWrapper getSlice(int offset, int length)
    {
        return new ByteBufferWrapper(ByteBuffer.wrap(byteBuffer.array()), offset, length);
    }

    private int index(int i)
    {
        return i + internalOffset;
    }

    public short getUnsignedByte()
    {
        return ((short) (byteBuffer.get() & (short) 0xff));
    }

    public short getUnsignedByte(int offset)
    {
        return ((short) (byteBuffer.get(index(offset)) & (short) 0xff));
    }

    public void putUnsignedByte(short v)
    {
        byteBuffer.put((byte) (v & 0xff));
    }

    public void putUnsignedByte(short v, int offset)
    {
        byteBuffer.put(index(offset), (byte) (v & 0xff));
    }

    public int getUnsignedShort()
    {
        return (byteBuffer.getShort() & 0xffff);
    }

    public int getUnsignedShort(int offset)
    {
        return (byteBuffer.getShort(index(offset)) & 0xffff);
    }

    public void putUnsignedShort(int v)
    {
        byteBuffer.putShort((short) (v & 0xffff));
    }

    public void putUnsignedShort(int v, int offset)
    {
        byteBuffer.putShort(index(offset), (short) (v & 0xffff));
    }

    public long getUnsignedInt()
    {
        return ((long) byteBuffer.getInt() & 0xffffffffL);
    }

    public long getUnsignedInt(int offset)
    {
        return ((long) byteBuffer.getInt(index(offset)) & 0xffffffffL);
    }

    public void putUnsignedInt(long v)
    {
        byteBuffer.putInt((int) (v & 0xffffffffL));
    }

    public void putUnsignedInt(long v, int offset)
    {
        byteBuffer.putInt(index(offset), (int) (v & 0xffffffffL));
    }

    public ByteBufferWrapper compact()
    {
        return new ByteBufferWrapper(byteBuffer.compact());
    }

    public ByteBufferWrapper duplicate()
    {
        return new ByteBufferWrapper(byteBuffer.duplicate());
    }

    public ByteBuffer getByteBuffer()
    {
        return byteBuffer;
    }

    public byte get()
    {
        return byteBuffer.get();
    }

    public byte get(int i)
    {
        return byteBuffer.get(index(i));
    }

    public char getChar()
    {
        return byteBuffer.getChar();
    }

    public char getChar(int i)
    {
        return byteBuffer.getChar(index(i));
    }

    public double getDouble()
    {
        return byteBuffer.getDouble();
    }

    public double getDouble(int i)
    {
        return byteBuffer.getDouble(index(i));
    }

    public float getFloat()
    {
        return byteBuffer.getFloat();
    }

    public float getFloat(int i)
    {
        return byteBuffer.getFloat(index(i));
    }

    public int getInt()
    {
        return byteBuffer.getInt();
    }

    public int getInt(int i)
    {
        return byteBuffer.get(index(i));
    }

    public long getLong()
    {
        return byteBuffer.getLong();
    }

    public long getLong(int i)
    {
        return byteBuffer.getLong(index(i));
    }

    public short getShort()
    {
        return byteBuffer.getShort();
    }

    public short getShort(int i)
    {
        return byteBuffer.getShort(index(i));
    }

    public byte[] getNibbles()
    {
        byte[] nibbles = new byte[2];
        byte temp = byteBuffer.get();
        nibbles[0] = (byte) ((temp >> 4) & 0xF);
        nibbles[1] = (byte) (temp & 0xF);
        return nibbles;
    }

    public byte[] getNibbles(int i)
    {
        byte[] nibbles = new byte[2];
        nibbles[0] = (byte) ((byteBuffer.get(index(i)) >> 4) & 0xF);
        nibbles[1] = (byte) (byteBuffer.get(index(i)) & 0xF);
        return nibbles;
    }

    public byte getRightShiftedByte(int shift)
    {
        return (byte) (byteBuffer.get() >>> shift);
    }

    public byte getRightShiftedByte(int i, int shift)
    {
        return (byte) (getUnsignedByte(index(i)) >>> shift);
    }

    public boolean isDirect()
    {
        return byteBuffer.isDirect();
    }

    public boolean isReadOnly()
    {
        return byteBuffer.isReadOnly();
    }

    public ByteBufferWrapper put(byte b)
    {
        byteBuffer.put(b);
        return this;
    }

    public ByteBufferWrapper put(int i, byte b)
    {
        byteBuffer.put(index(i), b);
        return this;
    }

    public ByteBufferWrapper putChar(char c)
    {
        byteBuffer.putChar(c);
        return this;
    }

    public ByteBufferWrapper putChar(int i, char c)
    {
        byteBuffer.putChar(index(i), c);
        return this;
    }

    public ByteBufferWrapper putDouble(double v)
    {
        byteBuffer.putDouble(v);
        return this;
    }

    public ByteBufferWrapper putDouble(int i, double v)
    {
        byteBuffer.putDouble(index(i), v);
        return this;
    }

    public ByteBufferWrapper putFloat(float v)
    {
        byteBuffer.putFloat(v);
        return this;
    }

    public ByteBufferWrapper putFloat(int i, float v)
    {
        byteBuffer.putFloat(index(i), v);
        return this;
    }

    public ByteBufferWrapper putInt(int i)
    {
        byteBuffer.putInt(i);
        return this;
    }

    public ByteBufferWrapper putInt(int i, int i2)
    {
        byteBuffer.putInt(index(i), i2);
        return this;
    }

    public ByteBufferWrapper putLong(long l)
    {
        byteBuffer.putLong(l);
        return this;
    }

    public ByteBufferWrapper putLong(int i, long l)
    {
        byteBuffer.putLong(index(i), l);
        return this;
    }

    public ByteBufferWrapper putShort(short i)
    {
        byteBuffer.putShort(i);
        return this;
    }

    public ByteBufferWrapper putShort(int i, short i2)
    {
        byteBuffer.putShort(index(i), i2);
        return this;
    }

    public ByteBufferWrapper slice()
    {
        return new ByteBufferWrapper(byteBuffer.slice());
    }

    public ByteBufferWrapper flip()
    {
        limit(position());
        position(0);
        return this;
    }

    public int limit()
    {
        return byteBuffer.limit() - internalOffset;
    }

    public ByteBufferWrapper limit(int limit)
    {
        byteBuffer.limit(index(limit));
        return this;
    }

    public int position()
    {
        return byteBuffer.position() - internalOffset;
    }

    public ByteBufferWrapper position(int pos)
    {
        byteBuffer.position(index(pos));
        return this;
    }

    public boolean hasRemaining()
    {
        return byteBuffer.hasRemaining();
    }
}
