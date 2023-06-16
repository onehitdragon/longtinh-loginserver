package yoyo.server.login.database;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.admin.SnapshotIF;
import org.apache.log4j.PropertyConfigurator;

import yoyo.server.login.tools.Log;


public class ConnPool
{
    private static String  NAME;

    private static boolean FLAG_INIT;
    
    private static final String CONFIG_FILE = System.getProperty("user.dir")
    + File.separator + "xj_login"
    + File.separator
    + File.separator + "config"
    + File.separator
    + "db.config";

    private ConnPool()
    {
    }


    public static void init ()
    {
        try
        {
            if (false == FLAG_INIT)
            {
                Properties dbConfig = new Properties();
                dbConfig.load(new FileInputStream(CONFIG_FILE));
                PropertyConfigurator.configure(System.getProperty("user.dir")
                        + File.separator + "xj_login" + File.separator + "config"
                        + File.separator + "proxool_log4j.config");
                NAME = "proxool."
                        + dbConfig.getProperty("proxool_url_head");

                Class.forName(dbConfig.getProperty("proxool_driver"));
                Class.forName(dbConfig.getProperty("db_driver"));

                Properties proxoolProperty = new Properties();

                proxoolProperty.setProperty("proxool.maximum-connection-count",
                        dbConfig.getProperty("maximum_connection_count"));
                proxoolProperty.setProperty("proxool.minimum-connection-count",
                        dbConfig.getProperty("minimum_connection_count"));
                proxoolProperty.setProperty("maximum-active-time", dbConfig
                        .getProperty("maximum_active_time"));
                proxoolProperty.setProperty("proxool.house-keeping-test-sql",
                        dbConfig.getProperty("house_keeping_test_sql"));
                proxoolProperty.setProperty("test-before-use", dbConfig
                        .getProperty("test_before_use"));
                proxoolProperty.setProperty("user", dbConfig
                        .getProperty("db_user"));
                proxoolProperty.setProperty("password", dbConfig
                        .getProperty("db_password"));

                String proxoolURL = new StringBuffer(NAME).append(":")
                        .append(dbConfig.getProperty("db_driver")).append(":")
                        .append(dbConfig.getProperty("db_url")).toString();

                ProxoolFacade.registerConnectionPool(proxoolURL,
                        proxoolProperty);

                Log.println(ConnPool.class.getName()
                        + " complete init.");

                FLAG_INIT = true;
            }
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public synchronized static Connection getConnection ()
    {
        try
        {
            return DriverManager.getConnection(NAME);
        }
        catch (SQLException sqle)
        {
            Log.error(null, sqle);

            return null;
        }
    }

    public static String showPoolInfo ()
    {
        try
        {
            SnapshotIF snapShot = ProxoolFacade.getSnapshot(NAME);
            StringBuffer buffer = new StringBuffer("DB Pool Statistics : \n");

            buffer.append(
                    "         Active Connection Count: "
                            + snapShot.getActiveConnectionCount()).append("\n");
            buffer.append(
                    "         Avaliable Connection Count: "
                            + snapShot.getAvailableConnectionCount()).append(
                    "\n");
            buffer.append(
                    "         Connection Count: "
                            + snapShot.getConnectionCount()).append("\n");
            buffer.append(
                    "         Maxximum Connection Count: "
                            + snapShot.getMaximumConnectionCount())
                    .append("\n");
            buffer.append(
                    "         Offline Connection Count: "
                            + snapShot.getOfflineConnectionCount())
                    .append("\n");
            buffer.append(
                    "         Refuse  Count: " + snapShot.getRefusedCount())
                    .append("\n");
            buffer.append(
                    "         Served  Count: " + snapShot.getServedCount())
                    .append("\n");

            return buffer.toString();
        }
        catch (ProxoolException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}