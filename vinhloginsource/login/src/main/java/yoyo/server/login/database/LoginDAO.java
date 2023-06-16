package yoyo.server.login.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.log4j.Logger;

import yoyo.server.login.DAOThread;
import yoyo.server.login.tools.AccountManager;
import yoyo.server.login.tools.Log;

public class LoginDAO
{
    private static Logger log = Logger.getLogger(LoginDAO.class);

    private static int         VAILD_ACCOUNT_ID            = 0;

    private static int         VAILD_USER_ID;

    public static final byte   ACCOUNT_TYPE_OF_MPHONE_NUMBER = 1;

    public static final byte   ACCOUNT_TYPE_OF_REGIST        = 2;

    public static final byte   ACCOUNT_TYPE_OF_REGIST_RANDOM        = 3;

    public static final String DEFAULT_MOBILE           = "none";

    private static final String SELECT_LOADING_TIPS = "select t.content from loading_tip t";

    private static final String SELECT_PWD_BY_USERAME = "select password from account where username=?";

    private static final String SELECT_BIND_MSISDN = "select bind_msisdn from account where username=?";

    private static final String UPDATE_BIND_MSISDN = "update account set bind_msisdn=? where account_id=?";

    private static final String SELECT_SMS_FEE_SERVER_ID_INFO = "select server_id from sms_fee_ini where trans_id=?";

    private static final String UPDATE_MOBILE_USERID_DOWNLOAD_LOG = "update moblie_user_download_log set download_flag=?  where mobile_user_id=?";

    private static final String SAVE_MOBLIE_USERID_DOWNLOAD_LOG = "insert into moblie_user_download_log(mobile_user_id,download_flag) values(?,?)";

    private static final String CHECK_MOBILE_USERID_HAD_DOWNLOAD_NG = "select * from moblie_user_download_log where mobile_user_id=?";

    private static final String SELECT_SZF_CHARGE_INFO_NOT_TO_SERVER = "select * from szf_chargeup t where t.flag=0";

    private static final String UPDATE_SZF_CHARGE_INFO = "update szf_chargeup t set t.result=?,t.flag=?,t.point=?,t.type=? where t.order_id=? and trans_id=?";
    
    /**
     * 记录账号每次登录时间日志
     */
    private static final String INSERT_ACCOUNT_LOGIN_LOG = "insert into account_login_log(account_id) values(?)";
    
    /**
     * 插入新玩家在进入游戏过程中的步骤编号
     */
    private static final String INSERT_NEW_PLAYER_LOGIN_STEP_SQL = "insert into new_player_login_log(id,step) values(?,?)";
    
    /**
     * 更新新玩家在进入游戏过程中的步骤编号
     */
    private static final String UPDATE_NEW_PLAYER_LOGIN_STEP_SQL = "update new_player_login_log t set t.step=? where t.id=?";
    
    /**
     * 查询新玩家在进入游戏步骤中的步骤编号
     */
    private static final String SELECT_NEW_PLAYER_LOGIN_STEP_SQL = "select step from new_player_login_log where id=?";
    
    /**
     * 游戏刚启动时获取的手机号保存
     */
    private static final String INSERT_MSISDN = "insert into login_phone(phone) values(?)";
    
    /**
     * 获取白名单后，更新账号的手机号和UA(机型)
     */
    private static final String UPDATE_MSISDN_UA = "update account set msisdn=?,agent=? where account_id=?";
    /**
     * 注册账号时的手机号和机型
     */
    private static final String UPDATE_REGIST_MSISDN_UA = "update account set regist_msisdn=?,regist_agent=? where account_id=?";
    /**
     * 账号每登录时的手机号和机型
     */
    private static final String ACCOUNT_LOGIN_PHONE = "insert into account_login_phone(account_id,phone,agent) values(?,?,?)";

    /**
     * 创建角色SQL脚本
     */
    private static final String CREATE_ROLE_SQL                = "insert into role("
                                                                       + "account_id,user_id,sequence,nickname,"
                                                                       + "server_id) values(?,?,?,?,?)";

    /**
     * 删除角色SQL脚本
     */
    private static final String DELETE_ROLE_SQL                = "delete from role where user_id=? and server_id=? limit 1";

    /**
     * 验证用户名是否存在
     */
    private static final String VERIFY_USER_NAME               = "select account_id from account where username=? limit 1";

    /**
     * 插入帐号信息SQL脚本
     */
    private static final String INSERT_ACCOUNT_SQL             = "insert into account(account_id,username,"
                                                                       + "password,type,publisher,client_jar_type,"
                                                                       + "client_version,msisdn,agent,update_time,curr_publisher)"
                                                                       + " values(?,?,?,?,?,?,?,?,?,?,?)";
    /**
     * 更新当前登录的渠道号
     */
    private static final String UPDATE_ACCOUNT_LOGIN_CURR_PUBLISHER = "update account set curr_publisher=? where account_id=?";

    /**
     * 替换伪手机号码SQL脚本
     */
    private static final String REPLACE_PSEUDO_MSISDN_SQL      = "UPDATE account SET username = ?,update_time=? "
                                                                       + "WHERE username = ? AND password = ? LIMIT 1";

    /**
     * 使用用户名、秘密登陆SQL脚本
     */
    private static final String LOGIN_BY_ACCOUNT_SQL           = "select account_id from account where "
                                                                       + "username=? and password=? limit 1";
    
    /**
     * 使用用户名、密码获取渠道号
     */
    private static final String GET_PUBLISHER_BY_NAME           = "select publisher from account where "
                                                                       + "username=? and password=? limit 1";
    /**
     * 修改密码
     */
    private static final String MODIFY_PASSWORD_SQL            = "update account set password=?,update_time=? where username=? AND password = ? limit 1";

    /**
     * 根据帐号编号查询角色编号列表
     */
    private static final String SELECT_USER_ID_LIST_SQL        = "select user_id,sequence from role where account_id=? and server_id =? order by user_id asc limit 3";

    /**
     * 验证昵称是否重复
     */
    private static final String CHECK_NICK_NAME_SQL            = "select account_id from role where server_id =? and nickname like ?";

    /**
     * 帐号ID列名
     */
    private static final String COLUMN_NAME_OF_ACCOUNT_ID      = "account_id";

    /**
     * userID列名
     */
    private static final String COLUMN_NAME_OF_USER_ID         = "user_id";

    /**
     * 操作失败，因为异常
     */
    public static final int     OPERATE_OF_FAILER              = -1;

    /**
     * 操作成功
     */
    public static final int     OPERATE_OF_SUCCESSFUL          = 1;

    /**
     * 因某数据冲突或无效
     */
    public static final int     OPERATE_OF_INVALID_DATA        = 0;

    /**
     * 所有编号的初始值
     */
    private static final int    MIN_USER_ID                    = 1000;
    
    private LoginDAO()
    {
    }

    public static final void init ()
    {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
System.out.println("初始化1（帐号编号）" + VAILD_ACCOUNT_ID);
        try
        {
            conn = ConnPool.getConnection();
            ps = conn.prepareStatement("select max(user_id) as max_id from role");
            rs = ps.executeQuery();

            if (rs.next())
            {
                int maxUserID = rs.getInt("max_id");
System.out.println("maxUserID = " + maxUserID);
                if (maxUserID >= MIN_USER_ID)
                {
System.out.println("maxUserID >= MIN_USER_ID");
                    VAILD_USER_ID = ++maxUserID;
System.out.println("maxUserID >= MIN_USER_ID USEABLE_USER_ID = " + VAILD_USER_ID);
                }
                else
                {
                    VAILD_USER_ID = MIN_USER_ID;
System.out.println(" @@@ USEABLE_USER_ID = MIN_USER_ID = " + VAILD_USER_ID);                         
                }
            }
            else
            {
                VAILD_USER_ID = MIN_USER_ID;
System.out.println("USEABLE_USER_ID = MIN_USER_ID  " + VAILD_USER_ID);    
            }
System.out.println("end ... USEABLE_USER_ID = " + VAILD_USER_ID);
        }
        catch (Exception exception)
        {
            Log.println("Error in " + LoginDAO.class.getName()
                    + "::init()\r\n" + exception);
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
                if (null != ps)
                {
                    ps.close();
                    ps = null;
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

    public static List<String> getTipList(){
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<String> loadingtiplist = new ArrayList<String>();
        try{
            conn = ConnPool.getConnection();
            ps = conn.prepareStatement(SELECT_LOADING_TIPS);
            rs = ps.executeQuery();
            while (rs.next()){
                loadingtiplist.add(rs.getString("content"));
            }
            rs.close();
            ps.close();
        }catch (Exception e){
            Log.error("加载Loaing提示列表 error：",e);
        }finally
        {
            try
            {
                if (null != ps)
                {
                    ps.close();
                    ps = null;
                }

                if (null != conn)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return loadingtiplist;
    }

    public static int updateAccountCurrPublisher(int accountID,int currPublisher){

        Connection conn = null;
        PreparedStatement ps = null;
        int res = 0;
        try{
            conn = ConnPool.getConnection();
            ps = conn.prepareStatement(UPDATE_ACCOUNT_LOGIN_CURR_PUBLISHER);
            ps.setInt(1,currPublisher);
            ps.setInt(2,accountID);

            res = ps.executeUpdate();

            ps.close();
        }catch (Exception e){
            Log.error("账号:”" + accountID + "“已存在,该次操作失败",e);
        }finally
        {
            try
            {
                if (null != ps)
                {
                    ps.close();
                    ps = null;
                }

                if (null != conn)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	res = OPERATE_OF_FAILER;
            }
        }
        return res;
    }

    public static final synchronized int registe (int accountID,byte accountType,
            String mobile, String userName, String passwrd, int publisher,
            String clientJarType, String clientVersion, String agent)
    {
        int res = -1;
        Connection conn = null;
        PreparedStatement ps = null;
        try
        {
            conn = ConnPool.getConnection();
            if(conn == null){
            	System.out.println("conn 为NULL");
            	return res;
            }
            
            res = validateUserName(userName);
            if(res == 0){
            	return res;
            }

            ps = conn.prepareStatement(INSERT_ACCOUNT_SQL);

            ps.setInt(1, accountID);
            ps.setString(2, userName.toLowerCase());
            ps.setString(3, passwrd.toLowerCase());
            ps.setByte(4, accountType);
            ps.setInt(5, publisher);
            ps.setString(6, clientJarType);
            ps.setString(7, clientVersion);
            ps.setString(8, mobile);
            ps.setString(9, agent);
            ps.setString(10, LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd hh:mm:ss")));
            ps.setInt(11,publisher);
            res = ps.executeUpdate();
            if (res == 1)
            {
                DAOThread.getInstance().createAccount(
                        publisher, VAILD_ACCOUNT_ID,
                        mobile);
            }
            else
            {
                res = OPERATE_OF_INVALID_DATA;
            }
        }
        catch (Exception ex)
        {
        	res = OPERATE_OF_FAILER;
            ex.printStackTrace();
            //edit by zhengl ; date: 2010-12-15 ; note:用户名已存在的插入将不再写入异常信息,仅仅记录一下
            Log.println("账号:”" + userName + "“已存在,该次操作失败");
        }
        finally
        {
            try
            {
                if (null != ps)
                {
                    ps.close();
                    ps = null;
                }

                if (null != conn)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	res = OPERATE_OF_FAILER;
            }
        }

        return res;
    }

    public static final synchronized int modifyMobile (String mobile,
            String pseudoMobile, String passwrd)
    {
        int accountID;
        Connection conn = null;
        PreparedStatement ps = null;

        try
        {
            conn = ConnPool.getConnection();

            ps = conn.prepareStatement(REPLACE_PSEUDO_MSISDN_SQL);

            ps.setString(1, mobile.toLowerCase());
            ps.setTimestamp(2,new Timestamp(System.currentTimeMillis()));
            ps.setString(3, pseudoMobile.toLowerCase());
            ps.setString(4, passwrd.toLowerCase());

            if (ps.executeUpdate() == 1)
            {
                accountID = VAILD_ACCOUNT_ID++;
            }
            else
            {
                accountID = OPERATE_OF_INVALID_DATA;
            }
        }
        catch (Exception ex)
        {
            accountID = OPERATE_OF_FAILER;

            Log.error(null, ex);
        }
        finally
        {
            try
            {
                if (null != ps)
                {
                    ps.close();
                    ps = null;
                }

                if (null != conn)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
                accountID = OPERATE_OF_FAILER;
            }
        }

        return accountID;
    }
    
    /**
     * 获取账号渠道号
     * @param name
     * @param pwd
     * @return
     */
    public static final int getPublisherByName(String name,String pwd){
    	Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        int result = 1;
        try{
        	conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(GET_PUBLISHER_BY_NAME);
            pstm.setString(1, name);
            pstm.setString(2, pwd);
            rs = pstm.executeQuery();
            if(rs.next()){
            	result = rs.getInt("publisher");
            }else{
            	result = OPERATE_OF_INVALID_DATA;
            }
            rs.close();
            pstm.close();
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.error("获取账号渠道号:", e);
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
                e.printStackTrace();
            }
        }
        return result;
    }
    
    /**
     * 保存新玩家进入游戏步骤进度
     * @param id
     * @param step
     */
    public static final byte saveNewPlayerLoginStep(String id,byte step){
    	Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        byte currStep = 0;
        byte res = 0;
        try{
        	conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(SELECT_NEW_PLAYER_LOGIN_STEP_SQL);
            pstm.setString(1, id);
            rs = pstm.executeQuery();
            if(rs.next()){
            	currStep = rs.getByte("step");
            	
            	rs.close();
                pstm.close();
                
                if(step > currStep){//如果新的step 比之前的step 大则更新
                	pstm = conn.prepareStatement(UPDATE_NEW_PLAYER_LOGIN_STEP_SQL);
                	pstm.setByte(1, step);
                	pstm.setString(2, id);
                	
                	res = (byte)pstm.executeUpdate();
                }
                
            }else{
            	pstm = conn.prepareStatement(INSERT_NEW_PLAYER_LOGIN_STEP_SQL);
            	pstm.setString(1, id);
            	pstm.setByte(2, step);
            	
            	res = (byte)pstm.executeUpdate();
            }
            pstm.close();
            
        }catch (Exception e)
        {
            e.printStackTrace();
            Log.error("保存新玩家进入游戏步骤进度:", e);
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
                e.printStackTrace();
            }
        }
        return res;
    }


    public final static int login (String _userName, String _password)
    {
        int accountID = OPERATE_OF_FAILER;
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(LOGIN_BY_ACCOUNT_SQL);
            pstm.setString(1, _userName.toLowerCase());
            pstm.setString(2, _password.toLowerCase());

            rs = pstm.executeQuery();
            
//            pstm = conn.createStatement();
//            rs = pstm.executeQuery("select account_id from account where username='"+_userName.toLowerCase()+"' and password='"+_password.toLowerCase()+"' limit 1");
           
System.out.println("rs size = " + rs.getFetchSize());
            if (rs.next())
            {
            	
                accountID = rs.getInt(COLUMN_NAME_OF_ACCOUNT_ID);
System.out.println("rs accountID= " + accountID);
            }
            else
            {
System.out.println("accountID = OPERATE_OF_INVALID_DATA;");
                accountID = OPERATE_OF_INVALID_DATA;
            }
        }
        catch (Exception e)
        {
            accountID = OPERATE_OF_FAILER;
            e.printStackTrace();
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
                accountID = OPERATE_OF_FAILER;
            }
        }

        return accountID;
    }

    public final static int loginByMobile (int accountID,String userName,
            String password, String mobile, String clientVersion,
            int publisher, String clientJarType, String agent)
    {
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try
        {
            int authResult = AccountManager.check(userName, password);

            switch (authResult)
            {
                case 2:
                {
                    conn = ConnPool.getConnection();
                    pstm = conn.prepareStatement(LOGIN_BY_ACCOUNT_SQL);
                    pstm.setString(1, userName.toLowerCase());
                    pstm.setString(2, password.toLowerCase());
                    rs = pstm.executeQuery();

                    if (rs.next())
                    {
                        accountID = rs.getInt(COLUMN_NAME_OF_ACCOUNT_ID);
                    }
                    else
                    {
                        accountID = OPERATE_OF_INVALID_DATA;
                    }
                    break;
                }
                case 1:
                {
                    conn = ConnPool.getConnection();
                    pstm = conn.prepareStatement(INSERT_ACCOUNT_SQL);
                    pstm.setInt(1, accountID);
                    pstm.setString(2, userName.toLowerCase());
                    pstm.setString(3, password.toLowerCase());
                    pstm.setByte(4, ACCOUNT_TYPE_OF_MPHONE_NUMBER);
                    pstm.setInt(5, publisher);
                    pstm.setString(6, clientJarType);
                    pstm.setString(7, clientVersion);
                    pstm.setString(8, mobile);
                    pstm.setString(9, agent);
                    pstm.setTimestamp(10,new Timestamp(System.currentTimeMillis()));
                    pstm.setInt(11,publisher);
                    if (pstm.executeUpdate() == 1)
                    {
                        DAOThread.getInstance().createAccount(
                                publisher,
                                VAILD_ACCOUNT_ID, mobile);

                    }
                    else
                    {
                        accountID = OPERATE_OF_INVALID_DATA;
                    }

                    break;

                }
                case 0:
                {
                    conn = ConnPool.getConnection();
                    pstm = conn.prepareStatement(LOGIN_BY_ACCOUNT_SQL);
                    pstm.setString(1, userName.toLowerCase());
                    pstm.setString(2, password.toLowerCase());
                    rs = pstm.executeQuery();

                    if (rs.next())
                    {
                        accountID = rs.getInt(COLUMN_NAME_OF_ACCOUNT_ID);
                    }
                    else
                    {
                        accountID = OPERATE_OF_INVALID_DATA;
                    }
                    break;
                }
                case -1:
                {
                    accountID = OPERATE_OF_INVALID_DATA;

                    break;

                }
                case -2:
                {
                    break;
                }
            }
        }
        catch (Exception e)
        {
            accountID = OPERATE_OF_FAILER;
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
                accountID = OPERATE_OF_FAILER;
            }
        }
        return accountID;
    }

    public final static String[] modifyPassword (String _username,
            String _oldPassword, String _newPassword) throws Exception
    {
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(MODIFY_PASSWORD_SQL);
            pstm.setString(1, _newPassword);
            pstm.setTimestamp(2,new Timestamp(System.currentTimeMillis()));
            pstm.setString(3, _username);
            pstm.setString(4, _oldPassword);
            if (pstm.executeUpdate() == 1)
            {
                return new String[]{_username, _newPassword };
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
            throw e;
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

    public synchronized final static int getUseableUserID ()
    {
        return VAILD_USER_ID++;
    }

    public final static ArrayList<int[]> getUserIDListByAccountID (
            int _accountID, short _serverID)
    {
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        ArrayList<int[]> userIDList = null;
        log.debug("##### getUserIDListByAccountID="+_accountID);
        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(SELECT_USER_ID_LIST_SQL);
            pstm.setInt(1, _accountID);
            pstm.setShort(2, _serverID);
            rs = pstm.executeQuery();

            userIDList = new ArrayList<int[]>();


            Map<Integer,Integer> userSequence = new HashMap<Integer,Integer>();
            while (rs.next())
            {
                int userID = rs.getInt(COLUMN_NAME_OF_USER_ID);
                if(userSequence.get(_accountID) == null){
                    userIDList.add(new int[]{0,userID});
                    userSequence.put(_accountID,0);
                    log.info("####### accountID="+_accountID+",userID="+userID+",sequence=0"+",mapsize="+userSequence.size());
                }else {
                    int sequence = userSequence.get(_accountID)+1;
                    userIDList.add(new int[]{sequence, userID});
                    userSequence.put(_accountID,sequence);
                    log.info("###@@@@ accountID="+_accountID+",userID="+userID+",sequence="+sequence+",mapsize="+userSequence.size());
                }

            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
            userIDList = null;
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
                userIDList = null;
            }
        }

        return userIDList;
    }

    public final static boolean createRole (int _accountID, int _userID,
            byte _sequence, String _nickname, short _serverID)
    {
        Connection conn = null;
        PreparedStatement pstm = null;
        boolean success = false;
        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(CREATE_ROLE_SQL);
            pstm.setInt(1, _accountID);
            pstm.setInt(2, _userID);
            pstm.setShort(3, _sequence);
            pstm.setString(4, _nickname);
            pstm.setShort(5, _serverID);
            System.out.println("也插入role表:_accountID="+_accountID
            		+";_userID="+_userID+";_sequence="+_sequence+";_nickname="+_nickname
            		+";_serverID="+_serverID);
            pstm.execute();
            success = true;
            DAOThread.getInstance().createRole(_accountID, _userID,
                    _nickname, _serverID);
        }
        catch (Exception e)
        {
            Log.error(null, e);

            try
            {
                conn.rollback();
            }
            catch (SQLException sqle)
            {

            }

            success = false;
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
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	//关闭数据库对象失败,但不一定角色创建就失败了,直接输出这个关闭异常的LOG即可
            	//edit by zhengl ; date: 2010-12-15 ; note: 添加日志输出
//                success = false;
            	Log.error(null, e);
            }
        }

        return success;
    }

    public final static boolean deleteRole (int _userID, short _serverID)
    {
        Connection conn = ConnPool.getConnection();
        PreparedStatement pstm = null;
        boolean success = false;
        try
        {
            pstm = conn.prepareStatement(DELETE_ROLE_SQL);
            pstm.setInt(1, _userID);
            pstm.setShort(2, _serverID);

            pstm.execute();
            success = true;
        }
        catch (Exception e)
        {
            try
            {
                conn.rollback();
            }
            catch (SQLException sqle)
            {

            }

            Log.error(null, e);

            success = false;
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
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
                success = false;
            }
        }

        return success;
    }
    
    public final static int getAccountID(String _username){
    	Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(VERIFY_USER_NAME);
            pstm.setString(1, _username);
            rs = pstm.executeQuery();

            if (rs.next())
            {
                return rs.getInt(1);
            }
            else
            {
                return 0;
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);

            return OPERATE_OF_FAILER;
        }
        finally
        {
            try
            {
                if (rs != null)
                {
                    rs.close();
                    rs = null;
                }
                if (pstm != null)
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
    }

    public final static int validateUserName (String _username)
    {
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(VERIFY_USER_NAME);
            pstm.setString(1, _username);
            rs = pstm.executeQuery();

            if (rs.next())
            {
                return OPERATE_OF_INVALID_DATA;
            }
            else
            {
                return OPERATE_OF_SUCCESSFUL;
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);

            return OPERATE_OF_FAILER;
        }
        finally
        {
            try
            {
                if (rs != null)
                {
                    rs.close();
                    rs = null;
                }
                if (pstm != null)
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
    }

    public final static boolean nicknameCanBeUse (String _userName,
            short _serverID)
    {
        boolean success = false;
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(CHECK_NICK_NAME_SQL);
            pstm.setShort(1, _serverID);
            pstm.setString(2, _userName);
            rs = pstm.executeQuery();

            if (rs.next())
            {
                success = false;
            }
            else
            {
                success = true;
            }
        }
        catch (Exception e)
        {
            success = false;
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
                success = false;
            }
        }

        return success;
    }

    public static void addMbile(String mobile){
    	Connection conn = null;
        PreparedStatement pstm = null;
        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(INSERT_MSISDN);
            pstm.setString(1, mobile);
            pstm.execute();
        }catch (Exception e)
        {
            try
            {
                conn.rollback();
            }
            catch (SQLException sqle)
            {
            	Log.error("游戏刚启动时获取手机号码保存 rollback error: ", sqle);
            }
            Log.error("游戏刚启动时获取手机号码保存 error: ", e);
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
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	Log.error("游戏刚启动时获取手机号码保存 error: ", e);
            }
        }
    }
    
    /**
     * 获取白名单后，更新账号的手机号和UA(机型)
     * @param accountID
     * @param msisdn 手机号
     * @param UA 机型
     * @return
     */
    public static int updateMsisdnUA(int accountID, String msisdn, String UA,String type){
    	Connection conn = null;
        PreparedStatement pstm = null;
        try
        {
            conn = ConnPool.getConnection();

            if(type.startsWith("login")){
                pstm = conn.prepareStatement(UPDATE_MSISDN_UA);
                pstm.setString(1, msisdn);
                pstm.setString(2, UA);
                pstm.setInt(3, accountID);

                pstm.executeUpdate();

                pstm.close();

                pstm = conn.prepareStatement(ACCOUNT_LOGIN_PHONE);
                pstm.setInt(1,accountID);
                pstm.setString(2,msisdn);
                pstm.setString(3,UA);

                pstm.executeUpdate();

                pstm.close();
            }
            if(type.startsWith("regist")){//注册时的手机号和机型
                pstm = conn.prepareStatement(UPDATE_REGIST_MSISDN_UA);
                pstm.setString(1, msisdn);
                pstm.setString(2, UA);
                pstm.setInt(3, accountID);

                pstm.executeUpdate();
                pstm.close();

                pstm = conn.prepareStatement(UPDATE_MSISDN_UA);
                pstm.setString(1, msisdn);
                pstm.setString(2, UA);
                pstm.setInt(3, accountID);

                pstm.executeUpdate();

                pstm.close();

                pstm = conn.prepareStatement(ACCOUNT_LOGIN_PHONE);
                pstm.setInt(1,accountID);
                pstm.setString(2,msisdn);
                pstm.setString(3,UA);

                pstm.executeUpdate();

                pstm.close();
            }

            return 1;
        }
        catch (Exception e)
        {
            try
            {
                conn.rollback();
            }
            catch (SQLException sqle)
            {
            	Log.error("获取白名单后，更新账号的手机号和UA(机型)出错后 rollback error: ", sqle);
            }
            Log.error("获取白名单后，更新账号的手机号和UA(机型) error: ", e);
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
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	Log.error("获取白名单后，更新账号的手机号和UA(机型) error: ", e);
            }
        }

        return 0;
    }
    
    /**
     *  记录账号每次登录时间日志
     * @param accountID
     */
    public static void saveAccountLoginLog(int accountID){
    	Connection conn = null;
        PreparedStatement pstm = null;
        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(INSERT_ACCOUNT_LOGIN_LOG);
            pstm.setInt(1, accountID);

            pstm.executeUpdate();
        }
        catch (Exception e)
        {
            try
            {
                conn.rollback();
            }
            catch (SQLException sqle)
            {
            	Log.error(" 记录账号每次登录时间日志 rollback error: ", sqle);
            }
            Log.error(" 记录账号每次登录时间日志 error: ", e);
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
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	Log.error(" 记录账号每次登录时间日志 error: ", e);
            }
        }
    }

    /**
     * 检查此订单ID或流水号，是否已经保存过异步结果
     * @param orderID
     * @param type
     * @return  res -1:没保存过   1:保存过且成功   0:保存过但失败
     */
    public static int checkSZFResult(String orderID,int type){
        Connection conn = null;
//        PreparedStatement pstm = null;
        Statement st = null;
        ResultSet rs = null;
        int res = -1;//默认为没有保存过
        try{
            conn = ConnPool.getConnection();
//            log.debug("conn = " + conn);
//            pstm = conn.prepareStatement(SELECT_SZF_CHARGE_INFO);
//            pstm = conn.prepareStatement("select * from szf_chargeup t where t.order_id='"+orderID+"'");
//            pstm.setString(1,orderID);
//            log.debug("select * from szf_chargeup t where t.order_id='"+orderID+"'");
//            rs = pstm.executeQuery();
            String sql = "select t.result from szf_chargeup t where t.order_id='"+orderID+"' limit 1";
            if(type == 2){
                sql = "select t.result from szf_chargeup t where t.trans_id='"+orderID+"' limit 1";
            }
            st = conn.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()){
                res = rs.getInt("result");
            }
            log.debug("checkSZFResult = " + res);
            rs.close();
//            pstm.close();
            st.close();
        }catch (Exception e)
        {

            log.error(" 检查此订单ID或流水号，是否已经保存过异步结果 error: ", e);
        }
        finally
        {
            try
            {
                if (null != st)
                {
                    st.close();
                    st = null;
                }
                if(rs != null){
                    rs.close();
                    rs = null;
                }
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	log.error(" 检查此订单ID或流水号，是否已经保存过异步结果 errors: ", e);
            }
        }
        return res;
    }

    /**
     * 获取神州付充值信息
     * @param orderID  唯一订单号(当乐神州付返回的是流水号)
     * @param type 1:自己的神州付   2:当乐神州付(
     * @return  [0]=user_id [1]=trans_id  [2]=server_id [3] order_id
     */
    public static String[] getSZFChargeInfo(String orderID,int type){
        log.debug("获取神州付充值信息 orderid="+orderID);
        Connection conn = null;
//        PreparedStatement pstm = null;
        Statement st = null;
        ResultSet rs = null;
        String[] res = null;
        try{
            conn = ConnPool.getConnection();
//            log.debug("conn = " + conn);
//            pstm = conn.prepareStatement(SELECT_SZF_CHARGE_INFO);
//            pstm = conn.prepareStatement("select * from szf_chargeup t where t.order_id='"+orderID+"'");
//            pstm.setString(1,orderID);
//            log.debug("select * from szf_chargeup t where t.order_id='"+orderID+"'");
//            rs = pstm.executeQuery();
            String sql = "select * from szf_chargeup t where t.order_id='"+orderID+"'";
            if(type == 2){
                sql = "select * from szf_chargeup t where t.trans_id='"+orderID+"'";
            }
            st = conn.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()){
                res = new String[4];
                res[0] = rs.getInt("role_id")+"";
                res[1] = rs.getString("trans_id");
                res[2] = rs.getInt("server_id")+"";
                res[3] = rs.getString("order_id");
                log.debug("roleid="+res[0]+",transid="+res[1]+",server_id="+res[2]+",orderid="+res[3]);
            }
            rs.close();
//            pstm.close();
            st.close();
        }catch (Exception e)
        {

            log.error(" 获取神州付充值信息 error: ", e);
        }
        finally
        {
            try
            {
                if (null != st)
                {
                    st.close();
                    st = null;
                }
                if(rs != null){
                    rs.close();
                    rs = null;
                }
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	log.error(" 获取神州付充值信息 errors: ", e);
            }
        }
        return res;
    }

    /**
     * 更新神州付返回的结果
     * @param orderID
     * @param result
     * @param flag 1:已经返回 0:未返回   是否给服务器返回 如果因为某种原因没有返回，则以后还要返回
     */
    public static int updateSZFResultFlag(String orderID,byte result,int flag,int point,String transID,int type){
    	Connection conn = null;
        PreparedStatement pstm = null;
        int res = 0;
        try
        {
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(UPDATE_SZF_CHARGE_INFO);
            pstm.setInt(1, result);
            pstm.setInt(2,flag);
            pstm.setInt(3,point);
            pstm.setInt(4,type);
            pstm.setString(5,orderID);
            pstm.setString(6,transID);

            res = pstm.executeUpdate();
        }
        catch (Exception e)
        {
            try
            {
                conn.rollback();
            }
            catch (SQLException sqle)
            {
            	Log.error(" 更新神州付结果 rollback error: ", sqle);
            }
            Log.error(" 更新神州付结果 error: ", e);
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
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	Log.error(" 更新神州付结果 error: ", e);
            }
        }
        return res;
    }

    /**
     * 获取没有给服务器返回充值结果的充值记录
     * @return
     */
    public static Map<String,String[]> getNotToServerSZFCharge(){
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        String[] res = null;
        Map<String,String[]> resultMap = new HashMap<String,String[]>();
        try{
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(SELECT_SZF_CHARGE_INFO_NOT_TO_SERVER);
            rs = pstm.executeQuery();
            while (rs.next()){
                res = new String[7];
                res[0] = rs.getInt("role_id")+"";
                res[1] = rs.getString("trans_id");
                res[2] = rs.getInt("server_id")+"";
                res[3] = rs.getString("order_id");
                res[4] = rs.getInt("result")+"";
                res[5] = rs.getInt("point")+"";
                res[6] = rs.getInt("type")+"";

                resultMap.put(res[3],res);
            }
            rs.close();
            pstm.close();
        }catch (Exception e)
        {

            Log.error(" 获取神州付充值信息 error: ", e);
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
                if(rs != null){
                    rs.close();
                    rs = null;
                }
                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	Log.error(" 获取神州付充值信息 error: ", e);
            }
        }
        return resultMap;
    }


    /**
     * 检查 moblie user id 是否下载过两个网游
     * @param mobileUserID
     * @return
     */
    public static String checkMoblieUserIDDownloadNg(String mobileUserID){
        String flag = null;
        Connection conn = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try{
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(CHECK_MOBILE_USERID_HAD_DOWNLOAD_NG);
            pstm.setString(1,mobileUserID);
            rs = pstm.executeQuery();
            if(rs.next()){
                flag = rs.getString("download_flag");
            }
            rs.close();
            pstm.close();
            conn.close();
        }catch (Exception e)
        {
            Log.error(" 检查 moblie user id 是否下载过两个网游 error: ", e);
        }
        finally
        {
            try
            {
                if(rs != null){
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
            	Log.error(" 检查 moblie user id 是否下载过两个网游  error: ", e);
            }
        }
        return flag;
    }

    /**
     * 保存或更新下载标志
     * @param mobileUserID
     * @param flag
     */
    public static void saveOrUpdMobileUserIDDownloadLog(String mobileUserID,String flag){
        String hasflag = checkMoblieUserIDDownloadNg(mobileUserID);
        log.info("save or update moblieuserid before hasflag="+hasflag);
        if(hasflag == null){
            saveMoblieUserIDDownloadLog(mobileUserID,flag);
        }else {
            if(hasflag.trim().length()>0 && !flag.equals(hasflag)){
                updMoblieUserIDDownloadFlag(mobileUserID,hasflag+","+flag);
            }
        }
    }

    /**
     * 保存moblie user id 下载log
     * @param moblieUserID
     * @param flag
     */
    private static int saveMoblieUserIDDownloadLog(String moblieUserID,String flag){
        log.info("save mobileuserid down flag="+flag);
        Connection conn = null;
        PreparedStatement pstm = null;
        int res = 0;
        try{
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(SAVE_MOBLIE_USERID_DOWNLOAD_LOG);
            pstm.setString(1,moblieUserID);
            pstm.setString(2,flag);

            res = pstm.executeUpdate();
        }catch (Exception e)
        {
            log.error("保存moblie user id 下载log error: ", e);
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

                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	log.error(" 保存moblie user id 下载log  error: ", e);
            }
        }
        return res;
    }

    /**
     * 更新moblieuserid的下载状态
     * @param mobileUserID
     * @return  hero只下载英雄成功  jiutian只下载九天成功  hero,jiutian
     */
    public static int updMoblieUserIDDownloadFlag(String mobileUserID,String flag){
        log.info("update mobileuserid down flag="+flag);
        int res = 0;
        Connection conn = null;
        PreparedStatement pstm = null;
        try{
            conn = ConnPool.getConnection();
            pstm = conn.prepareStatement(UPDATE_MOBILE_USERID_DOWNLOAD_LOG);
            pstm.setString(1,flag);
            pstm.setString(2,mobileUserID);

            res = pstm.executeUpdate();

            pstm.close();
            conn.close();
        }catch (Exception e)
        {
            log.error(" 更新moblieuserid的下载状态 error: ", e);
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

                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	log.error("更新moblieuserid的下载状态  error: ", e);
            }
        }
        return res;
    }

    /**
     * 获取短信计费的服务器ID
     * @param transID
     * @return
     */
    public static int getSmsFeeServerID(String transID){
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int serverID = 0;
        try{
            conn = ConnPool.getConnection();
            ps = conn.prepareStatement(SELECT_SMS_FEE_SERVER_ID_INFO);
            ps.setString(1,transID);

            rs = ps.executeQuery();
            if(rs.next()){
                serverID = rs.getInt("server_id");
            }

            rs.close();
            ps.close();
            conn.close();
        }catch (Exception e)
        {
            log.error(" 获取短信计费的服务器ID error: ", e);
        }
        finally
        {
            try
            {
                if(null != rs){
                    rs.close();;
                    rs = null;
                }
                if (null != ps)
                {
                    ps.close();
                    ps = null;
                }

                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	log.error("更新moblieuserid的下载状态  error: ", e);
            }
        }
        return serverID;
    }

    public static int bindMobile(int accountID,String mobile){
        Connection conn = null;
        PreparedStatement ps = null;
        int res = 0;
        try{
            conn = ConnPool.getConnection();
            ps = conn.prepareStatement(UPDATE_BIND_MSISDN);
            ps.setString(1,mobile);
            ps.setInt(2,accountID);

            res = ps.executeUpdate();

            ps.close();
            conn.close();
        }catch (Exception e)
        {
            log.error(" 绑定手机号 error: ", e);
        }
        finally
        {
            try
            {
                if (null != ps)
                {
                    ps.close();
                    ps = null;
                }

                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	log.error("绑定手机号  error: ", e);
            }
        }
        return res;
    }

    public static String getBindMobileByUsername(String username){
        String bindmsisdn = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try{
            conn = ConnPool.getConnection();
            ps = conn.prepareStatement(SELECT_BIND_MSISDN);
            ps.setString(1,username);

            rs = ps.executeQuery();
            if(rs.next()){
                bindmsisdn = rs.getString("bind_msisdn");
            }

            rs.close();
            ps.close();
            conn.close();
        }catch (Exception e)
        {
            Log.error(" 获取绑定的手机号 error: ", e);
        }
        finally
        {
            try
            {
                if(rs != null){
                    rs.close();
                    rs = null;
                }
                if (null != ps)
                {
                    ps.close();
                    ps = null;
                }

                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	Log.error("获取绑定的手机号  error: ", e);
            }
        }
        return bindmsisdn;
    }

    public static String getPwdByUsername(String username){
        String pwd = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try{
            conn = ConnPool.getConnection();
            ps = conn.prepareStatement(SELECT_PWD_BY_USERAME);
            ps.setString(1,username);

            rs = ps.executeQuery();
            if(rs.next()){
                pwd = rs.getString("password");
            }

            rs.close();
            ps.close();
            conn.close();
        }catch (Exception e)
        {
            Log.error(" 根据账号获取密码 error: ", e);
        }
        finally
        {
            try
            {
                if(rs != null){
                    rs.close();
                    rs = null;
                }
                if (null != ps)
                {
                    ps.close();
                    ps = null;
                }

                if (conn != null)
                {
                    conn.close();
                    conn = null;
                }
            }
            catch (Exception e)
            {
            	Log.error("根据账号获取密码  error: ", e);
            }
        }
        return pwd;
    }


}
