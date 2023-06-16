package yoyo.server.login.tools;


public class LetterNumberChecker
{
    public static boolean check (String str)
    {
        for (short i = 0; i < str.length(); i++)
        {
            int v = Character.valueOf(str.charAt(i)).hashCode();

            if (v > 'z' || v < '0'
                    || (v > '9' && v < 'A')
                    || (v > 'Z' && v < 'a'))
            { 
            	return false; 
            }
        }

        return true;
    }
}
