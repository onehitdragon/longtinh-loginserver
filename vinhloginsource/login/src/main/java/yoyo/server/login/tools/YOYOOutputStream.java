package yoyo.server.login.tools;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class YOYOOutputStream
{
    DataOutputStream dos;

    ByteArrayOutputStream baos;

    public YOYOOutputStream()
    {
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
    }

    public void writeInt (int value) throws IOException
    {
        dos.writeInt(value);
    }


    public void writeUTF (String str) throws IOException
    {
        dos.writeUTF(str);
    }

    public void writeShort (short value) throws IOException
    {
        dos.writeShort(value);
    }

    public void writeShort (int value) throws IOException
    {
        dos.writeShort(value);
    }

    public void writeByte (byte value) throws IOException
    {
        dos.writeByte(value);
    }

    public void writeByte (int value) throws IOException
    {
        dos.writeByte(value);
    }

    public void writeBytes (byte[] bytes) throws IOException
    {
        dos.write(bytes);
    }

    public void write2DBytes (byte[][] bytes2d) throws IOException
    {
        for (int i = 0; i < bytes2d.length; i++)
        {
            dos.write(bytes2d[i]);
        }
    }

    public void write2DUnequleBytes (byte[][] bytes2d) throws IOException
    {
        dos.writeShort(bytes2d.length);

        for (int i = 0; i < bytes2d.length; i++)
        {
            dos.writeShort(bytes2d[i].length);
            dos.write(bytes2d[i]);
        }
    }

    public void writeChar (char value) throws IOException
    {
        dos.writeChar(value);
    }

    public void writeChars (char[] chars) throws IOException
    {
        dos.writeChars(String.valueOf(chars));
    }

    public void flush () throws IOException
    {
        dos.flush();
    }

    public void reset ()
    {
        baos.reset();
    }

    public int size ()
    {
        return baos.size();
    }

    public byte[] getBytes ()
    {
        return baos.toByteArray();
    }

    public void close () throws IOException
    {
        baos.close();
        dos.close();
    }
}
