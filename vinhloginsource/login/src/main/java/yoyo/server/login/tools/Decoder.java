package yoyo.server.login.tools;


public class Decoder
{
	private final static byte hex[] = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70 };

    private Decoder()
    {
    }


    public static byte[] deCode (byte[] bytes)
    {
        int len = bytes.length;

        if (len % 2 == 0)
        {
            len = len / 2;
        }
        else
        {
            len = len / 2 + 1;
        }

        byte[] result = new byte[len];

        for (int i = 0, j = 0; i < bytes.length; i += 2, j++)
        {
            int hight = bytes[i];
            int low = 0;

            if (i + 1 != bytes.length)
            {
                low = bytes[i + 1];
            }

            result[j] = (byte) (indexOf((byte) hight) * 16 + indexOf((byte) low));
        }

        return result;
    }

    private static int indexOf (byte value)
    {
        for (int i = 0; i < hex.length; i++)
        {
            if (hex[i] == value) 
            { 
            	return i; 
            }
        }

        return -1;
    }
    

}
