package yoyo.server.login.tools;


public class Encoder
{
	private final static byte hex[] = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70 };

    private Encoder()
    {
    }

    public static byte[] encode (byte[] bytes)
    {
    	int num = 16;
        int len = bytes.length;
        byte[] result = new byte[len * 2];

        for (int i = 0; i < len; i++)
        {
            int j = bytes[i];

            if (j < 0)
            {
                j = num*num + j;
            }

            int k = j / num;
            int l = j % num;
            result[i * 2] = hex[k];
            result[i * 2 + 1] = hex[l];
        }

        return result;
    }
    

}
