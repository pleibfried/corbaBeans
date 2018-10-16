package biz.ple.corba.util;

import java.nio.ByteBuffer;

/**
 * Contains static conversion methods for CORBA object ids (byte arrays).
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public class CorbaObjectId {

    public static byte[] fromShort(short s)
    {
        return ByteBuffer.allocate(2).putShort(s).array();
    }


    public static byte[] fromInt(int i)
    {
        return ByteBuffer.allocate(4).putInt(i).array();
    }


    public static byte[] fromLong(long l)
    {
        return ByteBuffer.allocate(8).putLong(l).array();
    }


    public static byte[] fromFloat(float f)
    {
        return ByteBuffer.allocate(4).putFloat(f).array();
    }


    public static byte[] fromDouble(double d)
    {
        return ByteBuffer.allocate(8).putDouble(d).array();
    }


    public static short toShort(byte[] objectId)
    {
        return ByteBuffer.wrap(objectId).getShort();
    }


    public static int toInt(byte[] objectId)
    {
        return ByteBuffer.wrap(objectId).getInt();
    }


    public static long toLong(byte[] objectId)
    {
        return ByteBuffer.wrap(objectId).getLong();
    }


    public static float toFloat(byte[] objectId)
    {
        return ByteBuffer.wrap(objectId).getFloat();
    }


    public static double toDouble(byte[] objectId)
    {
        return ByteBuffer.wrap(objectId).getDouble();
    }

}
