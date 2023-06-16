package yoyo.server.login;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Properties;

import yoyo.server.login.tools.Log;

public class DAOThread
{
    private static DAOThread instance;

    private static final String  INSERT_ACCOUNT = "INSERT INTO T_GAME_ROLELOG(SOFTWAREID,CHANNELID,ACCOUNTID,MOBILE,ACCOUNTTIME)"
                                                               + " VALUES(?,?,?,?,sysdate)";
    private static final String  INSERT_ROLE    = "UPDATE T_GAME_ROLELOG SET USERID=?,NICKNAME=?,SERVERID=?,ROLETIME=sysdate"
                                                               + " WHERE ACCOUNTID=? AND USERID=0";
    private static final String  CONFIG_FILE           = System.getProperty("user.dir")
                                                               + File.separator
                                                               + "xj_login"
                                                               + File.separator
                                                               + File.separator
                                                               + "config"
                                                               + File.separator
                                                               + "total_platform.config";
    private Thread thread;
    private String dbDriver;
    private String dbURL;
    private String dbuName;
    private String dbPwd;
    private short gameID;
    private ArrayList<AccountInfo> lstAccountInfo;

    private DAOThread()
    {
        init();
        lstAccountInfo = new ArrayList<AccountInfo>();
        thread = new Thread(new Dao());
        thread.start();
    }

    private void init ()
    {
    	FileInputStream fis = null;
        try
        {
            Properties dbConfig = new Properties();
            fis = new FileInputStream(CONFIG_FILE);
            dbConfig.load(fis);

            dbURL = dbConfig.getProperty("db_url");
            dbuName = dbConfig.getProperty("db_user_name");
            dbPwd = dbConfig.getProperty("db_password");
            dbDriver = dbConfig.getProperty("db_driver");
            gameID = Short.parseShort(dbConfig.getProperty("game_id"));

            Class.forName(dbDriver);
            Log.println("账号统计已启动");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
        	if(fis != null)
        	{
        		try 
        		{
					fis.close();
				} 
        		catch (Exception e2) 
        		{			
				}
        	}
        }
    }
    
    public static DAOThread getInstance ()
    {
        if (null == instance)
        {
            instance = new DAOThread();
        }

        return instance;
    }

    public void createAccount (int channelID, int accountID, String mobile)
    {
        AccountInfo data = new AccountInfo();

        data.type = AccountInfo.ACCOUNT;
        data.channelID = channelID;
        data.accountID = accountID;
        data.mobile = mobile;

        lstAccountInfo.add(data);
    }

    public void createRole (int accountID, int roleUserID, String roleName,int roleServerID)
    {
        AccountInfo data = new AccountInfo();

        data.type = AccountInfo.ROLE;
        data.accountID = accountID;
        data.roleID = roleUserID;
        data.nickName = roleName;
        data.serverID = roleServerID;

        lstAccountInfo.add(data);
    }

    class Dao implements Runnable
    {
        public void run ()
        {
            try
            {
                Thread.sleep(5000);
            }
            catch (Exception e)
            {
            }

            while (true)
            {
                if (lstAccountInfo.size() > 0)
                {
                    Connection conn = null;
                    PreparedStatement pstm = null;

                    try
                    {
                        conn = DriverManager.getConnection(dbURL, dbuName, dbPwd);

                        AccountInfo data;

                        int size = lstAccountInfo.size();

                        while (size > 0)
                        {
                            try
                            {
                                data = lstAccountInfo.get(0);
                            }
                            catch (Exception e)
                            {
                                break;
                            }

                            try
                            {
                                if (data.type == AccountInfo.ACCOUNT)
                                {
                                    pstm = conn.prepareStatement(INSERT_ACCOUNT);
                                    pstm.setInt(1, gameID);
                                    pstm.setInt(2, data.channelID);
                                    pstm.setInt(3, data.accountID);
                                    pstm.setString(4, data.mobile);
                                    pstm.executeUpdate();

                                    lstAccountInfo.remove(0);
                                    size--;
                                }
                                else
                                {
                                    pstm = conn.prepareStatement(INSERT_ROLE);
                                    pstm.setInt(1, data.roleID);
                                    pstm.setString(2, data.nickName);
                                    pstm.setInt(3, data.serverID);
                                    pstm.setInt(4, data.accountID);
                                    pstm.executeUpdate();

                                    lstAccountInfo.remove(0);
                                    size--;
                                }
                            }
                            catch (Exception e)
                            {
                                break;
                            }

                            if (null != pstm)
                            {
                                pstm.close();
                                pstm = null;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        try
                        {
                            if (null != pstm)
                            {
                                pstm.close();
                                pstm = null;
                            }

                            if (null != conn)
                            {
                                conn.close();
                                conn = null;
                            }
                        }
                        catch (Exception e)
                        {

                        }
                    }
                }

                try
                {
                    Thread.sleep(5000);
                }
                catch (Exception e)
                {

                }
            }
        }
    }

    static class AccountInfo
    {
        byte        type;
        int         channelID;
        int         accountID;
        String      mobile;
        int         roleID;
        String      nickName;
        int         serverID;
        static final byte ACCOUNT = 1;
        static final byte ROLE    = 2;
    }
}
