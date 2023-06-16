package yoyo.server.login.tools;

import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class AccountManager
{

    private static ArrayList<String[]> originalInfoList;

    private static ArrayList<String> invalidateInfoList;

    private static BufferedWriter invalidateWriter;
    
    private final static String SEPARATOR = "#";

    private final static String ENTER = "\n";

    private static SimpleDateFormat FORMATTER = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
    static
    {
        FORMATTER.applyPattern("yy-MM-dd HH:mm:ss");
    }

    public static void init ()
    {
    	
    }

    public static int check (String username, String password)
    {
        try
        {
            for (int i = 0; i < invalidateInfoList.size(); i++)
            {
                if (invalidateInfoList.get(i).equals(username))
                {
                    return 2;
                }
            }

            String[] accountInfo;

            for (int i = 0; i < originalInfoList.size(); i++)
            {
                accountInfo = originalInfoList.get(i);

                if (accountInfo[0].equals(username))
                { 
                    if (accountInfo[1].equals(password))
                    { 
                        invalidateWriter.write(username + SEPARATOR
                                + FORMATTER.format(new Date()) + ENTER);
                        invalidateWriter.flush();
                        invalidateInfoList.add(username);
                        originalInfoList.remove(i);

                        return 1;
                    }
                    else
                    {
                        return -1;
                    }
                }
            }

            return 0;
        }
        catch (Exception e)
        {
            return -2;
        }
    }

}
