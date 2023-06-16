package yoyo.server.login.config;

import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.io.File;


public class RMIConfig
{
    private final static String CONFIG_FILE = "xj_login" + File.separator+ "config" + File.separator+ "game_server.config";
    private static RMIConfig instance;

    private HashMap<String, String> configMap = new HashMap<String, String>();

    private RMIConfig()
    {
    }

    public static RMIConfig getInstance ()
    {
        if (instance == null)
        {
            instance = new RMIConfig();
            instance.load();
        }

        return instance;
    }

    public void reload ()
    {
        load();
    }

    private void load ()
    {
        FileInputStream fis = null;

        try
        {
            fis = new FileInputStream(CONFIG_FILE);
            Properties property = new Properties();
            property.load(fis);

            Enumeration<Object> enu = property.keys();

            while (enu.hasMoreElements())
            {
                String key = (String) enu.nextElement();
                String value = property.getProperty(key);
                configMap.put(key, value);
            }

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

    public String getValue (String key)
    {
        return configMap.get(key);
    }
}
