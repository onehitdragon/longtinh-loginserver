package yoyo.server.login;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class AccountChecker
{
    private static final String    VALID = "1";

    private static final String    PARA_NAME_MOBILE       = "mid";

    private static final String    PARA_NAME_PWD          = "password";

    private static final String    PARA_NAME_GAMEID      = "gameid";

    private final static String    CONFIG_FILE               = "xj_login"
                                                                     + File.separator
                                                                     + "config"
                                                                     + File.separator
                                                                     + "account_verify_info.config";

    private final static String URL_NAME = "account_authen_url";

    private final static String CONFIG_GAMEID = "game_id";
    
    private String verifyAddress;

    private String gameID;

    private Timer timer;

    private CheckTask task;

    private static AccountChecker instance;

    private AccountChecker()
    {
        timer = new Timer();
        task = new CheckTask();
        timer.schedule(task, task.delay, task.period);
    }

    public static AccountChecker getInstance ()
    {
        if (null == instance)
        {
            instance = new AccountChecker();
            instance.initWithConfig();
        }

        return instance;
    }

    public boolean check (String mobile, String pwd)
    {
        StringBuffer urlStringBuffer = new StringBuffer(verifyAddress);

        urlStringBuffer.append("?");
        urlStringBuffer.append(PARA_NAME_GAMEID);
        urlStringBuffer.append("=");
        urlStringBuffer.append(gameID);
        urlStringBuffer.append("&");
        urlStringBuffer.append(PARA_NAME_MOBILE);
        urlStringBuffer.append("=");
        urlStringBuffer.append(mobile);
        urlStringBuffer.append("&");
        urlStringBuffer.append(PARA_NAME_PWD);
        urlStringBuffer.append("=");
        urlStringBuffer.append(pwd);

        HttpURLConnection connection = null;
        InputStream input = null;
        BufferedReader reader = null;

        try
        {
            connection = (HttpURLConnection) (new URL(urlStringBuffer
                    .toString()).openConnection());

            connection.setDoInput(true);
            connection.connect();

            if (HttpURLConnection.HTTP_OK == connection.getResponseCode())
            {
                input = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(input));

                String result = reader.readLine();

                if (null != result)
                {
                    result = result.substring(8, 9);

                    return result.equals(VALID) ? true : false;
                }
            }
        }
        catch (Exception e)
        {
        }
        finally
        {
            try
            {
                if (null != reader)
                    reader.close();

                if (null != input)
                    input.close();

                if (null != connection)
                    connection.disconnect();
            }
            catch (Exception e)
            {
            }
        }

        return false;
    }

    private void initWithConfig ()
    {
        FileInputStream fis = null;

        try
        {
            fis = new FileInputStream(CONFIG_FILE);
            Properties property = new Properties();
            property.load(fis);

            verifyAddress = property.getProperty(URL_NAME);
            gameID = property.getProperty(CONFIG_GAMEID);

            property.clear();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                    fis = null;
                }
                catch (Exception ioe)
                {

                }
            }
        }
    }

    private class CheckTask extends TimerTask
    {
        long delay = 10 * 1000 * 60;
        long period = 10 * 1000 * 60 ;
        public void run ()
        {
            initWithConfig();
        }
    }

}
