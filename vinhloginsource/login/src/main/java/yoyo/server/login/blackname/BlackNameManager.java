package yoyo.server.login.blackname;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import yoyo.server.login.database.ConnPool;
import yoyo.server.login.tools.Log;

import javolution.util.FastList;

public class BlackNameManager
{
    private static final String SELECT_BLACKNAME_ACCOUNT = "SELECT * FROM account_black";

    private static final String DELETE_BLACKNAME_ACCOUNT = "DELETE FROM account_black WHERE account_id=?";

    private static final String SELECT_BLACKNAME_ROLE    = "SELECT * FROM role_black";

    private static final String DELETE_BLACKNAME_ROLE    = "DELETE FROM role_black WHERE user_id=?";

    private static final long   TIME_INTERVAL             = 5 * 1000 * 60;
    
    private FastList<String>     mobileList;

    private FastList<String>     usernameList;

    private FastList<Integer>    roleList;

    private Timer                timer;

    private static BlackNameManager instance;

    private BlackNameManager()
    {
        mobileList = new FastList<String>();
        usernameList = new FastList<String>();
        roleList = new FastList<Integer>();
        timer = new Timer();
        load();
        timer.schedule(new CheckTask(), TIME_INTERVAL,
                TIME_INTERVAL);
    	System.out.println("BlackNameManager init()");
    }

    public static BlackNameManager getInstance ()
    {
        if (null == instance)
        {
            instance = new BlackNameManager();
        }

        return instance;
    }

    public boolean isVaildMobile (String mobile)
    {
        return !mobileList.contains(mobile);
    }

    public boolean isVaildAccount (String username)
    {
        return !usernameList.contains(username);
    }

    public boolean isVaildRole (int roleID)
    {
        return !roleList.contains(roleID);
    }

    private void load ()
    {
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(SELECT_BLACKNAME_ACCOUNT);
            rs = pstm.executeQuery();

            int keepTime;
            String userName, mobile;
            Timestamp startTime;
            Timestamp endTime = new Timestamp(System.currentTimeMillis());
            int accountID;
            long taken;
            ArrayList<Integer> invalidateIDList = new ArrayList<Integer>();

            while (rs.next())
            {
                keepTime = rs.getShort("keep_time");
                userName = rs.getString("username");
                mobile = rs.getString("msisdn");
                accountID = rs.getInt("account_id");
                startTime = rs.getTimestamp("start_time");
                endTime = rs.getTimestamp("end_time");

                if (keepTime == 0)
                {
                    mobileList.add(mobile);
                    usernameList.add(userName);
                }
                else
                { 
                    taken = System.currentTimeMillis() - endTime.getTime();
                    
                    if (taken >= 0)
                    {
                        invalidateIDList.add(accountID);
                    }
                    else
                    {
                        mobileList.add(mobile);
                        usernameList.add(userName);
                    }
                }
            }

            if (invalidateIDList.size() > 0)
            {
                conn.setAutoCommit(false);
                pstm.close();
                pstm = conn.prepareStatement(DELETE_BLACKNAME_ACCOUNT);

                for (int id : invalidateIDList)
                {
                    pstm.setInt(1, id);
                    pstm.addBatch();
                }

                pstm.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            }

            pstm = conn.prepareStatement(SELECT_BLACKNAME_ROLE);
            rs = pstm.executeQuery();

            int userID;
            invalidateIDList.clear();

            while (rs.next())
            {
                keepTime = rs.getShort("keep_time");
                userID = rs.getInt("user_id");
                startTime = rs.getTimestamp("start_time");
                endTime = rs.getTimestamp("end_time");

                if (keepTime == 0)
                {
                    roleList.add(userID);
                }
                else
                { 
                    taken = System.currentTimeMillis()- endTime.getTime();
                    if (taken >= 0)
                    {
                        invalidateIDList.add(userID);
                    }
                    else
                    {
                        roleList.add(userID);
                    }
                }
            }

            if (invalidateIDList.size() > 0 )
            {
                conn.setAutoCommit(false);
                pstm.close();
                pstm = conn.prepareStatement(DELETE_BLACKNAME_ROLE);

                for (int id : invalidateIDList)
                {
                    pstm.setInt(1, id);
                    pstm.addBatch();
                }

                pstm.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
        }
        finally
        {
            try
            {
                if (null != rs)
                {
                    rs.close();
                    rs = null;
                }
                if (null != pstm)
                {
                    pstm.close();
                    pstm = null;
                }
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            }
        }
        System.out.println("BlackNameManager load()");
    }

    private void reload ()
    {  
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try
        {
            FastList<String> temporaryMsisdnList = new FastList<String>();
            FastList<String> temporaryUsernameList = new FastList<String>();
            FastList<Integer> temporaryRoleList = new FastList<Integer>();

            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(SELECT_BLACKNAME_ACCOUNT);
            rs = pstm.executeQuery();

            short keepTime;
            String userName, mobile;
            Timestamp startTime;
            Timestamp endTime = new Timestamp(System.currentTimeMillis());
            int accountID;
            long taken;
            ArrayList<Integer> invalidateIDList = new ArrayList<Integer>();

            while (rs.next())
            {
                keepTime = rs.getShort("keep_time");
                userName = rs.getString("username");
                mobile = rs.getString("msisdn");
                accountID = rs.getInt("account_id");
                startTime = rs.getTimestamp("start_time");
                endTime = rs.getTimestamp("end_time"); 
                if (keepTime == 0)
                {
                    temporaryMsisdnList.add(mobile);
                    temporaryUsernameList.add(userName);
                }
                else
                {
                    
                    taken = System.currentTimeMillis()
                            - endTime.getTime();
                    if (taken >= 0)
                    {
                        invalidateIDList.add(accountID);
                    }
                    else
                    {
                        temporaryMsisdnList.add(mobile);
                        temporaryUsernameList.add(userName);
                    }
                }
            }

            synchronized (mobileList)
            {
                mobileList.clear();
                mobileList = temporaryMsisdnList;
            }

            synchronized (usernameList)
            {
                usernameList.clear();
                usernameList = temporaryUsernameList;
            }

            if (invalidateIDList.size() > 0)
            {
                conn.setAutoCommit(false);
                pstm.close();
                pstm = conn.prepareStatement(DELETE_BLACKNAME_ACCOUNT);

                for (int id : invalidateIDList)
                {
                    pstm.setInt(1, id);
                    pstm.addBatch();
                }

                pstm.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            }

            pstm = conn.prepareStatement(SELECT_BLACKNAME_ROLE);
            rs = pstm.executeQuery();

            int userID;
            invalidateIDList.clear();

            while (rs.next())
            {
                keepTime = rs.getShort("keep_time");
                userID = rs.getInt("user_id");
                startTime = rs.getTimestamp("start_time");
                endTime = rs.getTimestamp("end_time");

                if (keepTime == 0)
                {
                    temporaryRoleList.add(userID);
                }
                else
                {
                    
                    taken = System.currentTimeMillis()
                            - endTime.getTime();
                    if (taken >= 0)
                    {
                        invalidateIDList.add(userID);
                    }
                    else
                    {
                        temporaryRoleList.add(userID);
                    }
                }
            }

            synchronized (roleList)
            {
                roleList.clear();
                roleList = temporaryRoleList;
            }

            if (invalidateIDList.size() > 0)
            {
                conn.setAutoCommit(false);
                pstm.close();
                pstm = conn.prepareStatement(DELETE_BLACKNAME_ROLE);

                for (int id : invalidateIDList)
                {
                    pstm.setInt(1, id);
                    pstm.addBatch();
                }

                pstm.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
        }
        finally
        {
            try
            {
                if (null != rs)
                {
                    rs.close();
                    rs = null;
                }
                if (null != pstm)
                {
                    pstm.close();
                    pstm = null;
                }
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            }
        }
        System.out.println("black account reload.... ");
    }

    private class CheckTask extends TimerTask
    {
        public void run ()
        {
            reload();
        }
    }
}
