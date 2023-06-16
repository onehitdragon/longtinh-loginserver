package yoyo.server.login;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;


public class LoginCache
{
    private final static short MAX_SESSION_ID = 32700;
    private static LoginCache instance;
    public static short MAX_CONNECTION_COUNT;
    private static int SESSION_TIME_OUT;
    private static int CHECK_INTERVAL;
    
    private boolean isInit;
    private LoginInfo[]infoList;
    private CheckTimeoutTask timeOutTask;
    private Timer timeOutTimer;
    private short actConnectCount;
    private short freeSessionID = 10;
    
    private static String CONFIG_FILE = System.getProperty("user.dir")
    + File.separator + "xj_login" + File.separator
    + File.separator + "config" + File.separator
    + "login.config";

    public static LoginCache getInstance ()
    {
        if (null == instance)
        {
            instance = new LoginCache();
        }

        return instance;
    }

    private LoginCache()
    {
    }
    
    public short getActConnectCount ()
    {
        return actConnectCount;
    }

    public void init ()
    {
        if (!isInit)
        {
            Properties config = new Properties();

            try
            {
                config.load(new FileInputStream(CONFIG_FILE));

                MAX_CONNECTION_COUNT = Short.parseShort(config.getProperty("max_connection_number"));
                SESSION_TIME_OUT = Integer.parseInt(config.getProperty("session_timeout"));
                CHECK_INTERVAL = Integer.parseInt(config.getProperty("inverval_of_session_checking"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            config.clear();
            config = null;

            infoList = new LoginInfo[MAX_CONNECTION_COUNT];

            for (int i = 0; i < MAX_CONNECTION_COUNT; i++)
            {
                infoList[i] = new LoginInfo();
            }

            timeOutTimer = new Timer();
            timeOutTask = new CheckTimeoutTask();
            timeOutTimer.schedule(timeOutTask,CHECK_INTERVAL,CHECK_INTERVAL);

            isInit = true;
        }
    }

    public synchronized int addLoginInfo (int accountID, String clientVersion,String mobile)
    {
        if (actConnectCount == MAX_CONNECTION_COUNT)
        { 
        	return -1; 
        }

        if (freeSessionID >= MAX_SESSION_ID)
        {
            freeSessionID = 10;
        }

        for (short i = 0; i < MAX_CONNECTION_COUNT; i++)
        {
            if (infoList[i].accountID != accountID)
            {
            	if (infoList[i].sessionID == 0)
                {
                    infoList[i].sessionID = freeSessionID++;
                    infoList[i].accountID = accountID;
                    infoList[i].lastTime = System.currentTimeMillis();
                    infoList[i].clientVersion = clientVersion;
                    infoList[i].mobile = mobile;

                    actConnectCount++;

                    for (int j = i + 1; j < MAX_CONNECTION_COUNT; j++)
                    {
                        if (infoList[j].accountID == accountID)
                        {
                            infoList[j].clean();

                            actConnectCount--;

                            break;
                        }
                    }

                    return infoList[i].sessionID;
                }
            }
            else
            {
                infoList[i].sessionID = freeSessionID++;
                infoList[i].lastTime = System.currentTimeMillis();
                infoList[i].clientVersion = clientVersion;
                infoList[i].mobile = mobile;

                return infoList[i].sessionID;
            }
        }

        return -1;
    }

    private synchronized void cleanTimeOutLoginInfo ()
    {
        for (LoginInfo info : infoList)
        {
            if (info.sessionID != 0)
            {
            	long taken = System.currentTimeMillis() - info.lastTime;
                if (taken >= SESSION_TIME_OUT)
                {
                    info.clean();
                    actConnectCount--;
                }
            }
        }
    }

    public void clean (int sessionID)
    {
        for (short i = 0; i < MAX_CONNECTION_COUNT; i++)
        {
            if (infoList[i].sessionID == sessionID)
            {
                infoList[i].clean();

                actConnectCount--;
                break;
            }
        }
    }

    public String getClientVersion (int sessionID)
    {
        for (LoginInfo info : infoList)
        {
            if (info.sessionID == sessionID) 
            { 
            	return info.clientVersion; 
            }
        }

        return null;
    }

    public boolean isValidSession (int sessionID)
    {
        for (LoginInfo info : infoList)
        {
            if (info.sessionID == sessionID) 
            { 
            	return true; 
            }
        }

        return false;
    }
    
    public String getMobile (int sessionID)
    {
        for (LoginInfo info : infoList)
        {
            if (info.sessionID == sessionID) 
            { 
            	return info.mobile; 
            }
        }

        return null;
    }

    public void setServerID (int sessionID, short serverID)
    {
        for (LoginInfo info : infoList)
        {
            if (info.sessionID == sessionID)
            {
                info.serverID = serverID;

                return;
            }
        }
    }

    public int getAccountID (int sessionID)
    {
        for (LoginInfo info : infoList)
        {
            if (info.sessionID == sessionID) 
            { 
            	return info.accountID;
            }
        }

        return -1;
    }

    public short getServerID (int sessionID)
    {
        if (sessionID > 0)
        {
            for (LoginInfo info : infoList)
            {
                if (info.sessionID == sessionID) 
                { 
                	return info.serverID; 
                }
            }
        }

        return -1;
    }
    
    public boolean keepActivity (int sessionID)
    {
        for (LoginInfo info : infoList)
        {
            if (info.sessionID == sessionID)
            {
                info.lastTime = System.currentTimeMillis();

                return true;
            }
        }

        return false;
    }
    
    private class CheckTimeoutTask extends TimerTask
    {
        public void run ()
        {
            cleanTimeOutLoginInfo();
        }
    }

    private class LoginInfo
    {
        private int    sessionID;
        private int    accountID;
        private short  serverID;
        private long   lastTime;
        private String clientVersion;
        private String mobile;

        private void clean ()
        {
            sessionID = 0;
            accountID = 0;
            serverID = 0;
            lastTime = 0;
            clientVersion = null;
            mobile = null;
        }
    }
}
