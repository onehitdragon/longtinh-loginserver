package yoyo.server.login.tools;

public class Convertor
{
    public static byte[] int2Bytes (int num)
    {
        byte[] ret = new byte[4];

        for (int i = 0; (i < 4); i++)
        {
        	ret[i] = (byte) (num >> 8 * i & 0xFF);
        }

        return ret;
    }

    public static int bytes2Int (byte[] bytes)
    {
        int ret = 0;
        byte btmp;

        for (int i = 0; i < bytes.length; i++)
        {
        	btmp = bytes[i];
            ret += (btmp & 0xFF) << (8 * (3 - i));
        }

        return ret;
    }

    public static int bytes2Int (byte[] bytes, int startIndex, int length)
    {
        int ret = 0;
        byte btmp;
        int count = 0;

        for (int i = startIndex; i < (startIndex + length); i++)
        {
        	btmp = bytes[i];
            ret += (btmp & 0xFF) << (8 * (3 - count));
            count++;
        }

        return ret;
    }

    public static byte[] short2Bytes (short num)
    {
        byte[] ret = new byte[2];

        for (int i = 0; (i < 2); i++)
        {
            ret[i] = (byte) (num >> 8 * i & 0xFF);
        }

        return ret;
    }

    public static short bytes2Short (byte[] bytes)
    {
        short ret = 0;
        byte btmp;

        for (int i = 0; i < bytes.length; i++)
        {
        	btmp = bytes[i];
            ret += (btmp & 0xFF) << (8 * i);
        }

        return ret;
    }

    public static byte[] char2Bytes (char ch)
    {
        int temp = (int) ch;
        byte[] ret = new byte[2];

        for (int i = ret.length - 1; i > -1; i--)
        {
        	ret[i] = (byte) (temp & 0xff);
            temp = temp >> 8;
        }

        return ret;
    }

    public static char bytes2Char (byte[] bytes)
    {
        int ret = 0;
        int num = 256;
        if (bytes[0] > 0)
        {
        	ret += bytes[0];
        }
        else
        {
        	ret += num + bytes[0];
        }

        ret *= num;

        if (bytes[1] > 0)
        {
        	ret += bytes[1];
        }
        else
        {
        	ret += num + bytes[1];
        }

        return (char) ret;
    }

    public static byte[] double2Bytes (double num)
    {
        byte[] ret = new byte[8];
        long l = Double.doubleToLongBits(num);

        for (int i = 0; i < ret.length; i++)
        {
        	ret[i] = new Long(l).byteValue();
            l = l >> 8;
        }

        return ret;
    }

    public static double bytes2Double (byte[] bytes)
    {
        long ret = bytes[0];
        ret &= 0xff;
        ret |= ((long) bytes[1] << 8);
        ret &= 0xffff;
        ret |= ((long) bytes[2] << 16);
        ret &= 0xffffff;
        ret |= ((long) bytes[3] << 24);
        ret &= 0xffffffffl;
        ret |= ((long) bytes[4] << 32);
        ret &= 0xffffffffffl;
        ret |= ((long) bytes[5] << 40);
        ret &= 0xffffffffffffl;
        ret |= ((long) bytes[6] << 48);
        ret |= ((long) bytes[7] << 56);

        return Double.longBitsToDouble(ret);
    }
}
