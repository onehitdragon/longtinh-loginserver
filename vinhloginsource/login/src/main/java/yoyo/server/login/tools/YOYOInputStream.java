package yoyo.server.login.tools;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;


public class YOYOInputStream
{

    private DataInputStream dis;

    private int size;


    public YOYOInputStream(byte[] bytes)
    {
        if (null != bytes)
        {
        	ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            dis = new DataInputStream(bais);
            size = bytes.length;
        }
    }

    public byte readByte () throws IOException
    {
        return dis.readByte();
    }

    public int readInt () throws IOException
    {
        return dis.readInt();
    }

    public short readShort () throws IOException
    {
        return dis.readShort();
    }

    public String readUTF () throws IOException
    {
        return dis.readUTF();
    }

    public void read (byte[] bytes) throws IOException
    {
        dis.read(bytes);
    }

    public long readLong () throws IOException
    {
        return dis.readLong();
    }

    public float readFloat () throws IOException
    {
        return dis.readFloat();
    }

    public double readDouble () throws IOException
    {
        return dis.readDouble();
    }

    public int size ()
    {
        return size;
    }

    public long skip (long length) throws IOException
    {
        return dis.skip(length);
    }

    public void mark (int pos) throws IOException
    {
        dis.mark(pos);
    }

    public void reset () throws IOException
    {
        dis.reset();
    }

    public void close () throws IOException
    {
        dis.close();
    }
}
