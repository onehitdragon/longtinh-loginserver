package yoyo.server.login;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import yoyo.server.login.blackname.BlackNameManager;
import yoyo.server.login.channel.DCNService;
import yoyo.server.login.channel.Result;
import yoyo.server.login.constvalue.MsgConst;
import yoyo.server.login.constvalue.RequestConst;
import yoyo.server.login.constvalue.ResponseConst;
import yoyo.server.login.constvalue.TipConst;
import yoyo.server.login.database.ConnPool;
import yoyo.server.login.database.LoginDAO;
import yoyo.server.login.tools.Convertor;
import yoyo.server.login.tools.Decoder;
import yoyo.server.login.tools.AccountManager;
import yoyo.server.login.tools.HttpConnUtil;
import yoyo.server.login.tools.Log;
import yoyo.server.login.tools.YOYOInputStream;
import yoyo.server.login.tools.YOYOOutputStream;


public class LoginServer
{
    private static Logger log = Logger.getLogger(LoginServer.class);
    
    private static final Random RND = new Random();
    private static final byte PASSWORD_LENGTH      = 11;
    private static final String PSEUDO_MOBILE_PREFIX = "ABC";
    private static final String COLOR_LEFT = "#S3";
    private static final String COLOR_RIGHT = "#E";
    private static final int GEN_RANDOM_ACCOUNT_LENGTH = 8;
    private static final int GEN_RANDOM_PASSWORD_LENGTH = 8;
    private static final String[] RANDOM_STRING = {"0","1","2","3","4","5","6","7","8","9"};
    private static final String LOGIN_SUCCESS_PARAMS = "&s=login_";
    private static final String REGIST_SUCCESS_PARAMS = "&s=regist_";

    private static LoginServer instance;
    private AccountChecker accountChecker;
    private LoginCache loginCache;
    private boolean isInit;

    private Timer  reSendSZFResultTask;
    private Map<String,String[]> reSendSZFResultMap;

    private LoginServer()
    {
    	init();
    	isInit = true;
    }

    public static LoginServer getInstance ()
    {
        System.out.println("vinhhhhhhhhhhh init LoginServer");
        if (null == instance)
        {
            instance = new LoginServer();
        }

        return instance;
    }

    public void init ()
    {
        if (!isInit)
        {
            Log.init();
            ConnPool.init();
            LoginDAO.init();
            GameServerManager.getInstance().init();
            MsgConst.init();
            InvaildWordChecker.init();
            loginCache = LoginCache.getInstance();
            loginCache.init();
            BlackNameManager.getInstance();
            accountChecker = AccountChecker.getInstance();
            AccountManager.init();
            DAOThread.getInstance();

            reSendSZFResultTask = new Timer();
            reSendSZFResultTask.schedule(new ReSendSZFResultTask(),30000,10*60*1000);

            isInit = true;
        }
    }
    
    /**
     * 获取白名单后，更新账号的手机号和UA(机型)
     * @param accountID
     * @param msisdn
     * @param UA
     * @param _type login_12312 登录，  regist_123231 注册
     * @return 成功返回账号进行验证
     */
    public int setAccountMobileUA(int accountID,String mobile,String UA,String _type)
    {
        log.debug("setAccountMsisdnUA accountid="+accountID+",msisdn="+mobile+",UA="+UA+",type="+_type);
    	if(LoginDAO.updateMsisdnUA(accountID, mobile, UA,_type) == 1){
    		return accountID;
    	}
    	return 0;
    }

    /**
     * 短信计费回调
     * @param transID
     * @param result
     */
    public void smsCallBack(String transID,String result) throws GameServerShutdownException {
        log.info("短信计费回调... transid="+transID+", result="+result);
        /*int serverID = LoginDAO.getSmsFeeServerID(transID);
        GameService gameService = GameServiceListManager.getInstance().getGameService((short)serverID);
        if(gameService.isRunning()){
            gameService.smsCallBack(transID,result);
        }*/
    }

    /**
     * 神州付回调
     * @param result   1:支付成功   2:支付失败
     * @param orderID  唯一的订单号(乐神州付返回的是流水号)
     * @param type 1:自己的神州付   2:当乐神州付(返回的是流水号)
     * @return 1:成功    0:失败
     */
    public int szfFeeCallBack(String result,String orderID,String addp,String type) throws GameServerShutdownException {
        log.info("神州付回调 result="+result+",orderid="+orderID+",addpoint="+addp+",type="+type);
        orderID = orderID.replaceAll("\r\n", "");
        if(type == null) type="1";

        int resu = 0;

        int checkres = LoginDAO.checkSZFResult(orderID,Integer.parseInt(type));
        log.info("check szf is save = " + checkres);
        if(checkres == -1){//如果之前没保存过，则保存

            resu = updSZFResult(result,orderID,addp,type);

        }else if(checkres == 0) {//如果保存过但失败或之前没保存过
            byte szfResult = Byte.parseByte(result);
            if(szfResult == 1){//如果第二次返回成功
                resu = updSZFResult(result,orderID,addp,type);
            }
            if(szfResult == 0){
                //如果再次返回失败，再保存一次吧
                resu = updSZFResult(result,orderID,addp,type);
            }
        }else if(checkres == 1){
            //保存过且成功，则不处理
        }
        return resu;
    }

    /**
     * 更新神州付返回结果
     * @param result   1:支付成功   2:支付失败
     * @param orderID  唯一的订单号(乐神州付返回的是流水号)
     * @param type 1:自己的神州付   2:当乐神州付(返回的是流水号)
     * @return 1:成功    0:失败
     * @throws GameServerShutdownException
     */
    private int updSZFResult(String result,String orderID,String addp,String type) throws GameServerShutdownException{
        int resu = 0;
        String[] info = LoginDAO.getSZFChargeInfo(orderID,Integer.parseInt(type));
        log.info("get szf charge info="+info);

        if(info != null){
            int point = Integer.parseInt(addp);
            int userID = Integer.parseInt(info[0]);
            String transid = info[1];
            int serverID = Integer.parseInt(info[2]);
            String orderid = info[3];

            log.info("神州付信息 userid="+userID+",serverid="+serverID+",transid="+info[1]);
            GameServer gameService = GameServerManager.getInstance().getGameServer((short)serverID);
            if(gameService.isRunning()){
                int res = gameService.szfFeeCallBack(userID,info[1],Byte.parseByte(result),orderid,point);
                log.info("神州付回调给服务端 res="+res);
                if(res == 1){
                    log.info("开始更新数据库记录...");
                    resu = LoginDAO.updateSZFResultFlag(orderid,Byte.parseByte(result),1,point,transid,Integer.parseInt(type));
                    return resu;
                }
            }
            log.info("给服务端加点未成功，开始更新数据库记录...");
            resu = LoginDAO.updateSZFResultFlag(orderid,Byte.parseByte(result),0,point,transid,Integer.parseInt(type));
        }
        return resu;
    }

    /**
     * 对外踢下线接口
     * @param accountID
     * @throws GameServerShutdownException
     */
    public void resetPlayerStatus(int accountID) throws GameServerShutdownException{
        GameServerManager.getInstance().resetPlayerStatus(accountID);
    }
    
    /**
     * 游戏启动时也要获取一次手机号，发回来只保存手机号
     * @param msisdn
     */
    public void addMobile(String mobile)
    {
    	LoginDAO.addMbile(mobile);
    }

    public byte[] process (String request)
    {
        if (!isInit)
        {
            return MsgConst.SERVICENOTREADY;
        }

        byte[] response = null;
        YOYOInputStream yis = null;

        try
        {
            byte[] data = Decoder.deCode(request.getBytes("ISO8859-1"));
            yis = new YOYOInputStream(data);

            int length = yis.readShort();
            byte gameID = yis.readByte();
            int sessionID = yis.readInt();
            byte count = yis.readByte();

            for (int i = 0; i < count; i++)
            {
                short subDatagramLength = yis.readShort();
                byte[] subDatagram = new byte[subDatagramLength];
                yis.read(subDatagram);
                
                YOYOInputStream subInput = new YOYOInputStream(subDatagram);
                short requestType = subInput.readShort();
                
                switch (requestType)
                {
                    case RequestConst.REQUEST_QUICKLOGIN:
                    {
                        String mobile = subInput.readUTF();
                        String pwd = subInput.readUTF();
                        String clientVersion = subInput.readUTF();
                        int publisher = subInput.readInt();
                        String clientJarType = subInput.readUTF();
                        String agent = subInput.readUTF();
                        if (BlackNameManager.getInstance().isVaildMobile(mobile))
                        {
                            response = quickLogin(mobile, pwd,
                                    clientVersion, publisher, clientJarType,
                                    agent);
                        }
                        else
                        {
                            return tip(TipConst.BLACKMOBILE, 0);
                        }
                        break;
                    }
                    case RequestConst.REQUEST_QUICKLOGINBYPSEUDOMOBILE:
                    {
                        String mobile = subInput.readUTF();
                        String pseudoMsisdn = subInput.readUTF();
                        String pwd = subInput.readUTF();
                        String clientVersion = subInput.readUTF();
                        int publisher = subInput.readInt();
                        String clientJarType = subInput.readUTF();
                        String agent = subInput.readUTF();

                        if (BlackNameManager.getInstance().isVaildMobile(
                                mobile))
                        {
                            response = quickLoginByPseudoMobile(mobile,
                                    pseudoMsisdn, pwd, clientVersion,
                                    publisher, clientJarType, agent);
                        }
                        else
                        {
                            return tip(TipConst.BLACKMOBILE, 0);
                        }

                        break;
                    }
                    case RequestConst.REQUEST_ACCOUNTREGISTE:
                    {
                        String mobile = subInput.readUTF();
                        String userName = subInput.readUTF();
                        String pwd = subInput.readUTF();
                        String clientVersion = subInput.readUTF();
                        int publisher = subInput.readInt();
                        String clientJarType = subInput.readUTF();
                        String agent = subInput.readUTF();

                        response = registe(mobile, userName, pwd,
                                clientVersion, publisher, clientJarType, agent);

                        break;
                    }
                    case RequestConst.REQUEST_ACCOUNTLOGIN:
                    {
                        String userName = subInput.readUTF().toLowerCase();
                        String password = subInput.readUTF().toLowerCase();
                        String clientVersion = subInput.readUTF();
                        int publisher = subInput.readInt();
                        byte type = subInput.readByte();//1:正常输入用户名和密码进入  2:注册成功后进入 3：(渠道登录)当乐乐号登陆4：当乐用户名登陆

                        if(type == 3 || type == 4) {//3：当乐乐号登陆4：当乐用户名登陆
                        	response = channelLogin(userName, password, clientVersion, "3", publisher, type);
                        } else {//1:正常输入用户名和密码进入  2:注册成功后进入
                        	if (BlackNameManager.getInstance().isVaildAccount(userName)){
                        		response = login(userName, password,clientVersion,publisher,type);
                        	} else {
                        		return tip(TipConst.BLACKNAME, 0);
                        	}
                        }
                        break;
                    }
                    case RequestConst.REQUEST_ACCOUNTLOGINBYMOBILE:
                    {
                        String mobile = subInput.readUTF();
                        String userName = subInput.readUTF().toLowerCase();
                        String password = subInput.readUTF().toLowerCase();
                        String clientVersion = subInput.readUTF();
                        int publisher = subInput.readInt();
                        String clientJarType = subInput.readUTF();
                        String agent = subInput.readUTF();

                        if (BlackNameManager.getInstance().isVaildAccount(
                                userName))
                        {
                            response = loginByMobile(userName,
                                    password, mobile, clientVersion, publisher,
                                    clientJarType, agent);
                        }
                        else
                        {
                            return tip(TipConst.BLACKNAME, 0);
                        }

                        break;
                    }
                    case RequestConst.REQUEST_MODIFYPWD:
                    {
                        String username = subInput.readUTF();
                        String oldPwd = subInput.readUTF();
                        String newPwd = subInput.readUTF();
                        response = modifyPwd(username, oldPwd, newPwd);

                        break;
                    }
                    case RequestConst.REQUEST_SERVERLIST:
                    {
                        if (sessionID <= 0 || !loginCache.keepActivity(sessionID))
                        {
                            response = tip(TipConst.DISCONNECTION, 0);
                        }
                        else
                        {
                            response = serverList(sessionID);
                        }

                        break;
                    }
                    case RequestConst.REQUEST_SELTSERVER:
                    {
                        short serverID = subInput.readShort();
                        byte clientType = subInput.readByte();
                        
                        response = selectServer(sessionID, serverID, clientType);
                        break;
                    }
                    case RequestConst.REQUEST_CREATEROLE:
                    {
                        byte sequence = subInput.readByte();
                        String nickname = subInput.readUTF();
                        byte clan = subInput.readByte();
                        byte vocation = subInput.readByte();
                        byte sex = subInput.readByte();
                        byte clientType = subInput.readByte();

                        if (nickname.indexOf("\r") != -1 || nickname.indexOf("\n") != -1)
                        {
                            response = tip(TipConst.INVAILDNICKNAME, sessionID);
                        }
                        else
                        {
                            response = createRole(sessionID, sequence, clan,
                                    sex, vocation, nickname, clientType);
                        }

                        break;
                    }
                    case RequestConst.REQUEST_DEFAULTROLELIST:
                    {
                        short serverID = subInput.readShort();
                        log.info("#####　进行默认角色信息 serverID="+serverID+" ,sessionID= " + sessionID);
                        response = defaultRoleList(sessionID, serverID);
                        break;
                    }
                    case RequestConst.REQUEST_DELETEROLE:
                    {
                        int userID = subInput.readInt();

                        response = deleteRole(sessionID, userID);

                        break;
                    }
                    case RequestConst.REQUEST_ENTERGAME:
                    {
                        int userID = subInput.readInt();

                        if (BlackNameManager.getInstance().isVaildRole(userID))
                        {
                            response = enterGame(sessionID, userID);
                        }
                        else
                        {
                            return tip(TipConst.BLACKROLE, 0);
                        }

                        break;
                    }
                    case RequestConst.REQUEST_HEARTJUMP:
                    {
                        short publisherTag = 0;

                        try
                        {
                            publisherTag = Short.parseShort(subInput.readUTF());
                        }
                        catch (Exception e)
                        {
                            publisherTag = 0;
                        }

                        response = heartJump(sessionID, publisherTag);

                        break;
                    }
                    case RequestConst.REQUEST_BACKROLELIST:
                    {
                        int accountID = subInput.readInt();
                        short serverID = subInput.readShort();
                        String clientVersion = subInput.readUTF();
                        String msisdn = subInput.readUTF();

                        response = backRoleList(accountID, serverID,
                                clientVersion, msisdn);

                        break;
                    }
                    case RequestConst.REQUEST_PHONEURL:
                    {
                    	response = getPhoneURL();
                    	break;
                    }
                    case RequestConst.NEW_PLAYER_LOGIN_STEP:
                    {
                    	
                    	String id = subInput.readUTF();
                    	byte step = subInput.readByte();
                    	response = newPlayerLoginStep(id,step);
                    	break;
                    }
                    case RequestConst.REQUEST_DOWNLOAD_NG_GMAE_URL:
                    {
                        String mobileUserID = subInput.readUTF();
                        log.info("请求网游游戏下载地址: moblieuserid="+mobileUserID);
                        response = getNgGameDownloadUrl(mobileUserID);

                        break;
                    }
                    case RequestConst.REQUEST_SAVE_DOWNLOAD_NG_GAME_FLAG:
                    {
                        String mobileUserID = subInput.readUTF();
                        String flag = subInput.readUTF();
                        log.info("更新下载网游游戏记录, mobiliuserid="+mobileUserID+",flag="+flag);
                        response = saveMobileUserIDDownloadNgGameLog(mobileUserID,flag);

                        break;
                    }
                    case RequestConst.REQUEST_BIND_MSISDN:
                    {
                        log.info("绑定手机号....");
                        String username = subInput.readUTF();
                        String pwd = subInput.readUTF();
                        String msisdn = subInput.readUTF();

                        response = bindMobile(username,pwd,msisdn);

                        break;
                    }
                    case RequestConst.REQUEST_PWD_BACK:
                    {
                        log.info("密码找回...");
                        String username = subInput.readUTF();

                        response = pwdBack(username);

                        break;
                    }
                    case RequestConst.REQUEST_SHORT_REGIST:
                    {
                        log.info("快速注册...");
                        String _clietVersion = subInput.readUTF();
                        int _publisher = subInput.readInt();
                        String clientJarType = subInput.readUTF();
                        response = quickRegist(_clietVersion,_publisher,clientJarType);
                        break;
                    }
                    case RequestConst.REQUEST_LOADING_TIPS:
                    {
                        log.info("请求Loading 提示列表");
                        response = loadingTips();
                        break;
                    }
                    default:
                    {
                        break;
                    }
                }
                try 
                {
                	if(subInput!=null)
                	{
                		subInput.close();
                	}
				}
                catch (Exception e) 
				{				
				}
            }
        }
        catch (Exception e)
        {
            log.error("process error: ", e);
            response = null;
        }
        finally
        {
            try
            {
                if (null != yis)
                {
                    yis.close();
                    yis = null;
                }
            }
            catch (Exception e)
            {
                Log.error(this, e);
                e.printStackTrace();
                response = null;
            }
        }
        return response;
    }

    private byte[] loadingTips()
    {
        byte[] ret = null;
        YOYOOutputStream yos = new YOYOOutputStream();
        try
        {
            List<String> loadingtiplist = LoginDAO.getTipList();
            yos.writeShort(ResponseConst.RESPONSE_LOADINGTIPS);
            yos.writeByte(GameServerManager.getInstance().getRoll_lady_time());
            yos.writeShort(loadingtiplist.size());
            for (String content : loadingtiplist)
            {
                yos.writeUTF(content);
            }
            yos.flush();

            byte[] bytes = yos.getBytes();

            ret = OutputFormat.getInstance().format(GameServer.GAME_ID,0,bytes);

        }catch (IOException e) 
        {
			e.printStackTrace();
		}
        finally
        {
        	try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();

                ret = null;
            }
        }
        return ret;
    }

    /**
     * 密码找回，下发短信
     * @param username
     * @return
     */
    private byte[] pwdBack(String username){
    	
        String mobile = LoginDAO.getBindMobileByUsername(username);
        if(mobile == null){
            return tip(TipConst.EMPTY_BIND_MOBILE,0);
        }
        String pwd = LoginDAO.getPwdByUsername(username);
        if(pwd == null){
            return tip(TipConst.GET_PWD_FAIL,0);
        }
        String content = "[仙镜]找回密码通知，您的密码是："+pwd;
        int res = sendSMS(mobile,content,1);
        if(res == 0){
            return tip(TipConst.SEND_PWD_SMS,0);
        }else if(res == 3) {
            return tip(TipConst.SEND_PWD_OVER_TIMES,0);
        }else {
            return tip(TipConst.SEND_PWD_SMS_FAIL,0);
        }
    }

    private int sendSMS(String msisdn,String content,int flag)
    {
        String smsUrl = GameServerManager.getInstance().getSMSURL();
        return HttpConnUtil.sendPwdBackSMS(smsUrl,msisdn,content);
    }

    private byte[] bindMobile(String username,String pwd,String mobile)
    {
        try{

            int checkres = LoginDAO.login(username,pwd);
            if(checkres <=0 )
            {
                return tip(TipConst.NAME_OR_PWD_ERROR,0);
            }

            int res = LoginDAO.bindMobile(checkres,mobile);

            if(res == 1)
            {
                return tip(TipConst.BIND_MOBILE_SUCCES,0);
            }

            return tip(TipConst.BIND_MOBILE_FAIL,0);

        }
        catch (Exception e)
        {
            log.error("绑定手机号 error : ",e);
        }
        return null;
    }

    private byte[] saveMobileUserIDDownloadNgGameLog(String mobileUserID,String flag)
    {
        byte[] ret = null;
        YOYOOutputStream yos = new YOYOOutputStream();
        try{

            LoginDAO.saveOrUpdMobileUserIDDownloadLog(mobileUserID,flag);

            yos.writeShort(ResponseConst.RESPONSE_DONWLOAD_NG_GAME_URL);
            yos.writeByte(0);
            yos.flush();
            byte[] bytes = yos.getBytes();
            ret = OutputFormat.getInstance().format(GameServer.GAME_ID,0,bytes);
        }
        catch (IOException e) 
        {
			e.printStackTrace();
		}
        finally
        {
        	try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                ret = null;
            }
        }
        return ret;
    }

    private byte[] getNgGameDownloadUrl(String moblieUserID)
    {
        byte[] ret = null;
        YOYOOutputStream yos = new YOYOOutputStream();
        try
        {
            yos.writeShort(ResponseConst.RESPONSE_DONWLOAD_NG_GAME_URL);
            //请求判断此 moblie user id 是否下载过两个网游游戏
            String download = LoginDAO.checkMoblieUserIDDownloadNg(moblieUserID);
            if(download != null && download.indexOf(",")>0){//完全下载的flag="hero,jiutian"或"jiutian,hero"
                yos.writeByte(0);//不需要再次下载
            }else {
                yos.writeByte(1); //需要下载
                String[][] downloadUrls = HttpConnUtil.getNgGameDownloadUrl(download);
                if(downloadUrls != null){
                    yos.writeByte(downloadUrls.length);

                    yos.writeUTF(GameServerManager.getInstance().getDownloadAgent());
                    yos.writeInt(GameServerManager.getInstance().getDownloadSize());

                    for (int i=0; i<downloadUrls.length; i++){
                        yos.writeUTF(downloadUrls[i][0]);
                        yos.writeUTF(downloadUrls[i][1]);
                        log.info(downloadUrls[i][0]+" > "+ downloadUrls[i][1]);
                    }
                }else {
                    yos.writeByte(0);
                }
            }

            yos.flush();

            byte[] bytes = yos.getBytes();

            ret = OutputFormat.getInstance().format(GameServer.GAME_ID,0,bytes);
        }catch (IOException e) {
			e.printStackTrace();
		}
        finally
        {
        	try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();

                ret = null;
            }
        }
        return ret;
    }
    

    private byte[] newPlayerLoginStep(String id,byte step)
    {
    	byte[] ret = null;
        YOYOOutputStream yos = new YOYOOutputStream();
        try {
        	
        	byte res = LoginDAO.saveNewPlayerLoginStep(id,step);
        	
        	yos.writeShort(ResponseConst.RESPONSE_STEP);
			yos.writeByte(res);
			
			yos.flush();

            byte[] serverListMsg = yos.getBytes();

            ret = OutputFormat.getInstance().format(GameServer.GAME_ID,0,serverListMsg);
		} 
        catch (IOException e)
        {
			e.printStackTrace();
		}
        finally
        {
        	try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();

                ret = null;
            }
        }
        return ret;
    }
    
    private byte[] getPhoneURL(){
    	byte[] ret = null;
        YOYOOutputStream yos = new YOYOOutputStream();
        try {
        	yos.writeShort(ResponseConst.RESPONSE_GET_PHONE_URL);
			yos.writeUTF(GameServerManager.getInstance().getPhoneURL());
            String[] moblie_login_urls = GameServerManager.getInstance().getMobileLoginUrl();
            yos.writeByte(moblie_login_urls.length);
            for (int i=0; i<moblie_login_urls.length; i++)
            {
                yos.writeUTF(moblie_login_urls[i]);
            }
			
			yos.flush();

            byte[] serverListMsg = yos.getBytes();

            ret = OutputFormat.getInstance().format(GameServer.GAME_ID,0,serverListMsg);
		} 
        catch (IOException e) 
        {
			e.printStackTrace();
		}
        finally
        {
        	try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();

                ret = null;
            }
        }
        return ret;
    }

    private byte[] quickRegist(String clientVersion,int publisher,String clientJarType)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;
        try
        {
            String username = createRndString(GEN_RANDOM_ACCOUNT_LENGTH);
            String pwd = createRndString(GEN_RANDOM_PASSWORD_LENGTH);

            int res = LoginDAO.validateUserName(username);
            if(res == LoginDAO.OPERATE_OF_INVALID_DATA)
            {
                return tip(TipConst.GEN_RANDOM_USERNAME_FAIL,0);
            }
            else 
            {
                int accountID = HttpConnUtil.getAccountID();
                if(accountID == -1)
                {
                    return tip(TipConst.GEN_RANDOM_USERNAME_FAIL,0);
                }
                res = LoginDAO.registe(accountID,LoginDAO.ACCOUNT_TYPE_OF_REGIST_RANDOM,"",username,pwd,
                         publisher,clientJarType,clientVersion,"");
                if(res == 1)
                {
                    int sessionID = loginCache.addLoginInfo(accountID,
                            clientVersion, username);
                    log.info("##### short　registe success accountID = " + accountID+",sessionID= " + sessionID);
                    HttpConnUtil.registeToUserCenter(accountID, username, pwd, publisher);

                    byte versionCheck = checkVersion(clientVersion);
                    yos = new YOYOOutputStream();
                    if (TipConst.TYPE_NEWVERSION_EXISTS == versionCheck)
                    {                      
                        yos.writeShort(ResponseConst.RESPONSE_REGIST_TIP);
                        yos.writeUTF(username);
                        yos.writeUTF(pwd);
                        yos.writeUTF("您的账号:"+COLOR_LEFT+username+COLOR_RIGHT+",密码:"+COLOR_LEFT+pwd+COLOR_RIGHT+"#E,请牢记账号密码，并且进行手机绑定确保账号安全！祝您游戏愉快！");

                        yos.flush();

                        byte[] serverListMsg = yos.getBytes();

                        ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                                sessionID, MsgConst.NEWCLIENTEXISTS,
                                serverListMsg);
                    }
                    else
                    {
                        yos.writeShort(ResponseConst.RESPONSE_REGIST_TIP);
                        yos.writeUTF(username);
                        yos.writeUTF(pwd);
                        yos.writeUTF("您的账号:"+COLOR_LEFT+username+COLOR_RIGHT+",密码:"+COLOR_LEFT+pwd+COLOR_RIGHT+",请牢记账号密码，并且进行手机绑定确保账号安全！祝您游戏愉快！");

                        yos.flush();

                        byte[] serverListMsg = yos.getBytes();

                        ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                                sessionID, serverListMsg, null);
                    }
                }

            }

        }catch (Exception e){
            log.error("快速注册 error:",e);
        }
        finally
        {
        	try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
            }
        }
        return ret;
    }

    private byte[] quickLogin (String mobile, String pwd,String clientVersion, int publisher, String clientJarType,String agent)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;

        try
        {
            byte versionCheck = checkVersion(clientVersion);

            if (TipConst.TYPE_MUST_UPGRADE_CLIENT == versionCheck)
            {
                return OutputFormat.getInstance().format(GameServer.GAME_ID, 0,MsgConst.NEWCLIENTEXISTS);
            }
            else
            {
                if (GameServerManager.getInstance()
                        .getRunningServerCount() == 0)
                {
                    return tip(TipConst.EMPTY_GAME_SERVER, 0);
                }

                if (loginCache.getActConnectCount() == LoginCache.MAX_CONNECTION_COUNT)
                {
                    return tip(TipConst.LOGIN_FULL, 0);
                }

                int accountID = 0;
                
                boolean isNewAccount = false;
                int res = -1;

                if (isPseudoMobile(mobile))
                {
                    if (accountChecker.check(mobile, pwd))
                    {
                    	accountID = HttpConnUtil.getAccountID();
                        res = LoginDAO.registe(accountID,
                                LoginDAO.ACCOUNT_TYPE_OF_MPHONE_NUMBER,
                                mobile, mobile, pwd, publisher,
                                clientJarType, clientVersion, agent);

                        if (res == LoginDAO.OPERATE_OF_FAILER)
                        {
                            return tip(TipConst.REGISTRY_FAILER, 0);
                        }
                    }
                    else
                    {
                        return tip(TipConst.INVALID_ACCOUNT, 0);
                    }

                    isNewAccount = true;
                }
                else
                {
                    accountID = LoginDAO.login(mobile, pwd);
                    if (accountID == LoginDAO.OPERATE_OF_INVALID_DATA)
                    {
                        if (accountChecker.check(mobile, pwd))
                        {
                        	accountID = HttpConnUtil.getAccountID();
                        	
                            res = LoginDAO.registe(accountID,
                                    LoginDAO.ACCOUNT_TYPE_OF_MPHONE_NUMBER,
                                    mobile, mobile, pwd, publisher,
                                    clientJarType, clientVersion, agent);

                            if (res == LoginDAO.OPERATE_OF_FAILER)
                            {
                                return tip(TipConst.REGISTRY_FAILER, 0);
                            }
                            if (res == LoginDAO.OPERATE_OF_INVALID_DATA)
                            {
                                return tip(TipConst.USER_NAME_EXISTS, 0);
                            }

                            isNewAccount = true;
                            
                            HttpConnUtil.registeToUserCenter(accountID, mobile, pwd, publisher);
                        }
                        else
                        {
                            return tip(TipConst.INVALID_ACCOUNT, 0);
                        }
                    }
                    else if (accountID == LoginDAO.OPERATE_OF_FAILER)
                    {
                        return tip(TipConst.SERVER_ERROR, 0);
                    }
                }

                if (!isNewAccount)
                {
                    log.debug("upd curr publisher ..");
                    int ures = LoginDAO.updateAccountCurrPublisher(accountID,publisher);
                    log.debug("upd curr publisher .. res ="+ures);

                    GameServerManager.getInstance().resetPlayerStatus(accountID);
                }

                int sessionID = loginCache.addLoginInfo(accountID,clientVersion, mobile);            
                yos = new YOYOOutputStream();
                if (TipConst.TYPE_NEWVERSION_EXISTS == versionCheck)
                {
                    yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
                    yos.writeInt(accountID);

                    GameServerManager.getInstance().serverListInfo(yos);
                    yos.flush();

                    byte[] serverListMsg = yos.getBytes();

                    ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                            sessionID, MsgConst.NEWCLIENTEXISTS,
                            serverListMsg);
                }
                else
                {
                    yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
                    yos.writeInt(accountID);

                    GameServerManager.getInstance().serverListInfo(yos);
                    yos.flush();

                    byte[] serverListMsg = yos.getBytes();

                    ret = OutputFormat.getInstance().format(GameServer.GAME_ID,sessionID, serverListMsg, null);
                }

                LoginDAO.saveAccountLoginLog(accountID);
            }
        }
        catch (Exception e)
        {
            Log.error(instance, e);
            e.printStackTrace();
            ret = null;
        }
        finally
        {
            try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();

                ret = null;
            }
        }

        return ret;
    }
    
    private static String createRndString(int length)
    {
        StringBuffer sf = new StringBuffer();

        for(int i=0; i<length; i++)
        {
            sf.append(RANDOM_STRING[RND.nextInt(length)]);
        }
        return sf.toString();
    }

    private byte[] quickLoginByPseudoMobile (String mobile,
            String pseudoMobile, String pwd, String clientVersion,
            int publisher, String clientJarType, String agent)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;

        try
        {
            byte versionCheck = checkVersion(clientVersion);

            if (TipConst.TYPE_MUST_UPGRADE_CLIENT == versionCheck)
            {
                return OutputFormat.getInstance().format(GameServer.GAME_ID, 0,MsgConst.NEWCLIENTEXISTS);
            }
            else
            {
                if (GameServerManager.getInstance()
                        .getRunningServerCount() == 0)
                {
                    return tip(TipConst.EMPTY_GAME_SERVER, 0);
                }

                if (loginCache.getActConnectCount() == LoginCache.MAX_CONNECTION_COUNT)
                {
                    return tip(TipConst.LOGIN_FULL, 0);
                }

                int accountID = HttpConnUtil.getAccountID();
                
                boolean isNewAccount = false;
                int res = -1;

                if (accountChecker.check(mobile, pwd))
                {
                    res = LoginDAO.modifyMobile(mobile, pseudoMobile,
                            pwd);

                    if (res == LoginDAO.OPERATE_OF_FAILER)
                    {
                        return tip(TipConst.GET_ACCOUNT_ERROR, 0);
                    }
                    else if (res == LoginDAO.OPERATE_OF_INVALID_DATA)
                    {
                        res = LoginDAO.registe(accountID,
                                LoginDAO.ACCOUNT_TYPE_OF_MPHONE_NUMBER,
                                mobile, mobile, pwd, publisher,
                                clientJarType, clientVersion, agent);

                        if (res == LoginDAO.OPERATE_OF_FAILER)
                        {
                            return tip(TipConst.REGISTRY_FAILER, 0);
                        }
                        if (res == LoginDAO.OPERATE_OF_INVALID_DATA)
                        {
                            return tip(TipConst.USER_NAME_EXISTS, 0);
                        }

                        isNewAccount = true;
                        HttpConnUtil.registeToUserCenter(accountID, pseudoMobile, pwd, publisher);
                    }
                }
                else
                {
                    return tip(TipConst.LOGIN_FAILER, 0);
                }

                if (!isNewAccount)
                {
                    log.debug("upd curr publisher ..");
                    int ures = LoginDAO.updateAccountCurrPublisher(accountID,publisher);
                    log.debug("upd curr publisher .. res ="+ures);

                    GameServerManager.getInstance().resetPlayerStatus(accountID);
                }

                int sessionID = loginCache.addLoginInfo(accountID,clientVersion, mobile);
                 
                yos = new YOYOOutputStream();
                if (TipConst.TYPE_NEWVERSION_EXISTS == versionCheck)
                {
                    yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
                    yos.writeInt(accountID);

                    GameServerManager.getInstance().serverListInfo(yos);

                    yos.flush();

                    byte[] serverListMsg = yos.getBytes();

                    ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                            sessionID, MsgConst.NEWCLIENTEXISTS,
                            serverListMsg);
                }
                else
                {
                    yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
                    yos.writeInt(accountID);

                    GameServerManager.getInstance().serverListInfo(yos);

                    yos.flush();

                    byte[] serverListMsg = yos.getBytes();

                    ret = OutputFormat.getInstance().format(GameServer.GAME_ID,sessionID, serverListMsg, null);
                }
                LoginDAO.saveAccountLoginLog(accountID);
            }

        }
        catch (Exception e)
        {
            Log.error(instance, e);

            ret = null;
        }
        finally
        {
            try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();

                ret = null;
            }
        }

        return ret;
    }

    private byte[] registe (String mobile, String userName,
            String pwd, String clientVersion, int publisher,
            String clientJarType, String agent)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;
        System.out.println("vinh --- register " + userName + ":::" + pwd);
        try
        {
            byte versionCheck = checkVersion(clientVersion);

            if (TipConst.TYPE_MUST_UPGRADE_CLIENT == versionCheck)
            {
                return OutputFormat.getInstance().format(GameServer.GAME_ID, 0,MsgConst.NEWCLIENTEXISTS);
            }
            else
            {
                if (GameServerManager.getInstance().getRunningServerCount() <= 0)
                {
                    return tip(TipConst.EMPTY_GAME_SERVER, 0);
                }

                if (loginCache.getActConnectCount() == LoginCache.MAX_CONNECTION_COUNT)
                {
                    return tip(TipConst.LOGIN_FULL, 0);
                }

                int accountID = 1;
                
                // vinh noteeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee
                // if (accountID == LoginDAO.OPERATE_OF_FAILER)
                // {
                //     return tip(TipConst.GET_ACCOUNT_ERROR, 0);
                // }
                
                int res = LoginDAO.registe(accountID,
                        LoginDAO.ACCOUNT_TYPE_OF_REGIST, mobile, userName,
                        pwd, publisher, clientJarType, clientVersion,agent);

                log.info(Log.formatLog("创建帐号:",res==1,"res="+res,userName, pwd, mobile, ""+publisher, clientJarType,
                		clientVersion, "暂无", "暂无", "暂无", "0"));

                
                if (res == LoginDAO.OPERATE_OF_INVALID_DATA)
                {
                    return tip(TipConst.USER_NAME_EXISTS, 0);
                }
                else if(res == LoginDAO.OPERATE_OF_FAILER)
                {
                    return tip(TipConst.SERVER_ERROR,0);
                }
                else
                {
                    int sessionID = loginCache.addLoginInfo(accountID,clientVersion, userName);
                    log.info("#####　registe success accountID = " + accountID+",sessionID= " + sessionID);

                    HttpConnUtil.registeToUserCenter(accountID, userName, pwd, publisher);
                    yos = new YOYOOutputStream();
                    if (TipConst.TYPE_NEWVERSION_EXISTS == versionCheck)
                    {
                        yos.writeShort(ResponseConst.RESPONSE_REGIST_TIP);
                        yos.writeUTF(userName);
                        yos.writeUTF(pwd);
                        yos.writeUTF("您的账号:"+COLOR_LEFT+userName+COLOR_RIGHT+",密码:"+COLOR_LEFT+pwd+COLOR_RIGHT+",请牢记账号密码，并且进行手机绑定确保账号安全！祝您游戏愉快！");

                        yos.flush();

                        byte[] serverListMsg = yos.getBytes();

                        ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                                sessionID, MsgConst.NEWCLIENTEXISTS,
                                serverListMsg);
                    }
                    else
                    {                   
                        yos.writeShort(ResponseConst.RESPONSE_REGIST_TIP);
                        yos.writeUTF(userName);
                        yos.writeUTF(pwd);
                        yos.writeUTF("您的账号:"+COLOR_LEFT+userName+COLOR_RIGHT+",密码:"+COLOR_LEFT+pwd+COLOR_RIGHT+",请牢记账号密码，并且进行手机绑定确保账号安全！祝您游戏愉快！");

                        yos.flush();

                        byte[] serverListMsg = yos.getBytes();

                        ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                                sessionID, serverListMsg, null);
                    }
                }
            }
        }
        catch (Exception e)
        {
        	e.printStackTrace();
            Log.error(null, e);
        }
        finally
        {
        	if(yos != null)
        	{
        		try 
        		{
					yos.close();
				} 
        		catch (Exception e2) 
        		{			
				}
        	}
        }

        return ret;
    }
    
    private byte[] newVersionUrl(int publisher) throws Exception
    {
    	YOYOOutputStream yos = null;
    	byte[] ret = null;
    	try {
        	String downloadUrl = VersionInfo.downloadUrlMap.get(publisher);
            if(downloadUrl == null)
            {
                downloadUrl = VersionInfo.downloadUrlMap.get(VersionInfo.XJ_DOWNLOAD_ID);
            }
            yos = new YOYOOutputStream();
        	yos.writeShort(ResponseConst.RESPONSE_NEWVERSIONMOTIFY);
        	yos.writeUTF(VersionInfo.curVersion);
        	yos.writeUTF(VersionInfo.compVersion);
        	yos.writeUTF(downloadUrl);
        	yos.flush();
        	ret = yos.getBytes();
		} 
    	catch (Exception e) 
		{
    		e.printStackTrace();
		}
    	finally
    	{
    		if(yos != null)
    		{
    			try 
    			{
					yos.close();
				}
    			catch (Exception e2) 
				{
					
				}
    		}
    	}
        return ret;
    
    }
    
    /**
     * 渠道登录
     * @author rlj
     * @param _userName 用户名
     * @param _password 密码
     * @param _clientVersion 客户端版本号
     * @param type 1:正常输入用户名和密码进入
     * @return
     */
    private byte[] channelLogin(String _userName, String _password, String _clientVersion, String _clientJarType, int publisher,byte type) {
        byte[] result = null;
        YOYOOutputStream output = null;
        try {
        	log.info("开始登陆. username="+_userName+",pwd="+_password);
            byte versionCheck = checkVersion(_clientVersion);

            if (TipConst.TYPE_MUST_UPGRADE_CLIENT == versionCheck) {
                return OutputFormat.getInstance().format(GameServer.GAME_ID, 0,
                		newVersionUrl(publisher));
            } else {
                if (GameServerManager.getInstance().getRunningServerCount() == 0) {
                    return tip(TipConst.EMPTY_GAME_SERVER, 0);
                }
                if (loginCache.getActConnectCount() == LoginCache.MAX_CONNECTION_COUNT) {
                    return tip(TipConst.LOGIN_FULL, 0);
                }
                Result result1;
                if(type == 3) {
                	result1 = DCNService.login(_userName, "", _password);
                } else {
                	result1 = DCNService.login("", _userName, _password);
                }
                if(!result1.isResult()) {
                	return tip(TipConst.NAME_OR_PWD_ERROR, 0);
                } else {
                	String username = DCNService.CHANNEL_ID + "#" + result1.getReList();//用的当乐的乐号作为用户名
                	log.debug("渠道用户的用户名：" + username);
                	int accountID = LoginDAO.getAccountID(username);
                	log.debug("在我方：获取账号" + accountID);
                	if (accountID == LoginDAO.OPERATE_OF_FAILER) {
                		return tip(TipConst.SERVER_ERROR, 0);
                	} else if (accountID == LoginDAO.OPERATE_OF_INVALID_DATA) {
                		accountID = channelRegister(username, _password, _clientVersion, publisher, _clientJarType);
                		log.debug("在我方：生成的账号" + accountID);
                	}
                	if(accountID == -1) {
                		return tip(TipConst.SERVER_ERROR, 0);
                	}
            		int sessionID = loginCache.addLoginInfo(accountID, _clientVersion, _userName);
            		log.info("生成sessionID" + sessionID);
            		GameServerManager.getInstance().resetPlayerStatus(accountID);
            		if (TipConst.TYPE_NEWVERSION_EXISTS == versionCheck) {
            			output = new YOYOOutputStream();
            			output.writeShort(ResponseConst.RESPONSE_SERVERLIST);
            			output.writeInt(accountID);
            			if(type == 1) {
            				output.writeUTF(LOGIN_SUCCESS_PARAMS+System.currentTimeMillis());
            			} else if(type == 2) {
            				output.writeUTF(REGIST_SUCCESS_PARAMS+System.currentTimeMillis());
            			}
            			GameServerManager.getInstance().serverListInfo(output);
            			output.flush();
            			byte[] serverListMsg = output.getBytes();
            			result = OutputFormat.getInstance().format(GameServer.GAME_ID,
            					sessionID, MsgConst.NEWCLIENTEXISTS,
            					serverListMsg);
            		} else {
            			output = new YOYOOutputStream();
            			output.writeShort(ResponseConst.RESPONSE_SERVERLIST);
            			output.writeInt(accountID);
            			if(type == 2) {
            				output.writeUTF(REGIST_SUCCESS_PARAMS+System.currentTimeMillis());
            			} else  {//type为1,3,4
            				output.writeUTF(LOGIN_SUCCESS_PARAMS+System.currentTimeMillis());
            			}
            			GameServerManager.getInstance().serverListInfo(output);
            			output.flush();
            			byte[] serverListMsg = output.getBytes();
            			result = OutputFormat.getInstance().format(GameServer.GAME_ID,sessionID, serverListMsg);
            		}
            		//记录账号登录时间
            		LoginDAO.saveAccountLoginLog(accountID);
                }
            }
            log.debug("登陆过程正常");
        } catch (Exception e) {
            log.error("渠道登陆过程发生异常:",e);
            result = null;
        } finally {
            try {
                if (output != null) {
                    output.close();
                    output = null;
                }
            } catch (Exception e) {
            	log.error("渠道登陆过程关闭流对象的时候发生异常:",e);
                result = null;
            }
        }
        return result;
    }
    
    private static int channelRegister(String userName, String pwd, String clientVersion, int publisher, String clientJarType) {
        int accountID = HttpConnUtil.getAccountID();
        if (accountID == LoginDAO.OPERATE_OF_FAILER) 
        {
        	return -1;
        }
        int res = LoginDAO.registe(accountID,LoginDAO.ACCOUNT_TYPE_OF_REGIST, "", userName,pwd, publisher, clientJarType, clientVersion,"");
        log.info(Log.formatLog("创建帐号:",res==1,"res="+res,userName, pwd, "", ""+publisher, clientJarType,clientVersion, "暂无", "暂无", "暂无", "0"));
        if (res == LoginDAO.OPERATE_OF_INVALID_DATA || res == LoginDAO.OPERATE_OF_FAILER) 
        {
        	return -1;
        } 
        else 
        {
        	return accountID;
        }
    }

    private byte[] loginByMobile (String userName, String password,
            String mobile, String clientVersion, int publisher,
            String clientJarType, String agent)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;

        try
        {
            byte versionCheck = checkVersion(clientVersion);

            if (TipConst.TYPE_MUST_UPGRADE_CLIENT == versionCheck)
            {
                return OutputFormat.getInstance().format(GameServer.GAME_ID, 0,MsgConst.NEWCLIENTEXISTS);
            }
            else
            {
                if (GameServerManager.getInstance().getRunningServerCount() == 0)
                {
                    return tip(TipConst.EMPTY_GAME_SERVER, 0);
                }
                if (loginCache.getActConnectCount() == LoginCache.MAX_CONNECTION_COUNT)
                {
                    return tip(TipConst.LOGIN_FULL, 0);
                }
                int accountID = HttpConnUtil.getAccountID();       
                int res = LoginDAO.loginByMobile(accountID, userName, password,
                        mobile, clientVersion, publisher, clientJarType,agent);

                if (res == LoginDAO.OPERATE_OF_FAILER)
                {
                    return tip(TipConst.SERVER_ERROR, 0);
                }
                else if (res == LoginDAO.OPERATE_OF_INVALID_DATA)
                {
                    return tip(TipConst.NAME_OR_PWD_ERROR, 0);
                }
                else
                {
                    int ures = LoginDAO.updateAccountCurrPublisher(accountID,publisher);

                    int sessionID = loginCache.addLoginInfo(accountID,clientVersion, userName);

                    GameServerManager.getInstance().resetPlayerStatus(accountID);
                    yos = new YOYOOutputStream();
                    if (TipConst.TYPE_NEWVERSION_EXISTS == versionCheck)
                    {     
                        yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
                        yos.writeInt(accountID);
                        yos.writeUTF(LOGIN_SUCCESS_PARAMS+System.currentTimeMillis());

                        GameServerManager.getInstance().serverListInfo(yos);

                        yos.flush();

                        byte[] serverListMsg = yos.getBytes();

                        ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                                sessionID, MsgConst.NEWCLIENTEXISTS,
                                serverListMsg);
                    }
                    else
                    {
                        yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
                        yos.writeInt(accountID);
                        yos.writeUTF(LOGIN_SUCCESS_PARAMS+System.currentTimeMillis());

                        GameServerManager.getInstance().serverListInfo(yos);

                        yos.flush();

                        byte[] serverListMsg = yos.getBytes();

                        ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                                sessionID, serverListMsg);
                    }
                    LoginDAO.saveAccountLoginLog(accountID);
                }
            }
        }
        catch (Exception e)
        {
            Log.error(instance, e);

            ret = null;
        }
        finally
        {
            try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                ret = null;
            }
        }

        return ret;
    }

    private byte[] serverList (int sessionID)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;
        try
        {
            yos = new YOYOOutputStream();
            yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
            yos.writeInt(loginCache.getAccountID(sessionID));
            yos.writeUTF("");
            GameServerManager.getInstance().serverListInfo(yos);

            yos.flush();

            ret = OutputFormat.getInstance().format(GameServer.GAME_ID, sessionID,yos.getBytes());
        }
        catch (Exception e)
        {
            Log.error(null, e);
            e.printStackTrace();
        }
        finally
        {
        	if(yos!=null)
        	{
        		try 
        		{
        			yos.close();
				} 
        		catch (Exception e2) 
        		{
				}
        	}
        }
        return ret;
    }
    
    private byte[] defaultRoleList (int _sessionID, short _serverID)
    {
        YOYOOutputStream yos = null;
        byte[] ret = null;

        try
        {
            GameServer gameService = GameServerManager.getInstance().getGameServer(_serverID);
            if(gameService.isRunning())
            {
                yos = new YOYOOutputStream();
                yos.writeShort(ResponseConst.RESPONSE_DEFAULTROLELIST);
                byte[] rmiResponse = gameService.getDefaultRoleList();
                if (rmiResponse != null)
                {
                    yos.writeBytes(rmiResponse);
                }
                else
                {
                    ret = tip(TipConst.ROLE_LIST_ERROR, 0);
                    return ret;
                }
                yos.flush();
                ret = OutputFormat.getInstance().format(GameServer.GAME_ID,_sessionID, yos.getBytes());

            }
            else
            {
                return tip(TipConst.SERVER_SHUTDOWN, _sessionID);
            }


        }
        catch (Exception e)
        {
            Log.error(null, e);
        	e.printStackTrace();
        }
        finally
        {
        	if(yos != null)
        	{
        		try 
        		{
                    yos.close();
                    yos = null;
				} 
        		catch (Exception e2) 
        		{
				}
        	}
        }

        return ret;
    }
    
    private byte[] login (String userName, String passwrd,String clientVersion,int publisher,byte type)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;

        try
        {
            byte versionCheck = checkVersion(clientVersion);

            if (TipConst.TYPE_MUST_UPGRADE_CLIENT == versionCheck)
            {
                return OutputFormat.getInstance().format(GameServer.GAME_ID, 0,newVersionUrl(publisher));
            }
            else
            {
                if (GameServerManager.getInstance().getRunningServerCount() == 0)
                {
                    return tip(TipConst.EMPTY_GAME_SERVER, 0);
                }

                if (loginCache.getActConnectCount() == LoginCache.MAX_CONNECTION_COUNT)
                {
                    return tip(TipConst.LOGIN_FULL, 0);
                }

                int accountID = LoginDAO.login(userName, passwrd);

                if (accountID == LoginDAO.OPERATE_OF_FAILER)
                {
                    return tip(TipConst.SERVER_ERROR, 0);
                }
                else if (accountID == LoginDAO.OPERATE_OF_INVALID_DATA)
                {
                    return tip(TipConst.NAME_OR_PWD_ERROR, 0);
                }
                else
                {
                    log.debug("upd curr publisher ..");
                    int ures = LoginDAO.updateAccountCurrPublisher(accountID,publisher);
                    log.debug("upd curr publisher .. res ="+ures);

                    int sessionID = loginCache.addLoginInfo(accountID,clientVersion, userName);

                    GameServerManager.getInstance().resetPlayerStatus(accountID);
                    yos = new YOYOOutputStream();
                    if (TipConst.TYPE_NEWVERSION_EXISTS == versionCheck)
                    {
                        yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
                        yos.writeInt(accountID);
                        if(type == 1)
                        {
                            yos.writeUTF(LOGIN_SUCCESS_PARAMS+System.currentTimeMillis());
                        }
                        else if(type == 2)
                        {
                            yos.writeUTF(REGIST_SUCCESS_PARAMS+System.currentTimeMillis());
                        }
                        GameServerManager.getInstance().serverListInfo(yos);

                        yos.flush();

                        byte[] serverListMsg = yos.getBytes();

                        ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                                sessionID, MsgConst.NEWCLIENTEXISTS,
                                serverListMsg);
                    }
                    else
                    {
                        yos.writeShort(ResponseConst.RESPONSE_SERVERLIST);
                        yos.writeInt(accountID);
                        if(type == 1)
                        {
                            yos.writeUTF(LOGIN_SUCCESS_PARAMS+System.currentTimeMillis());
                        }
                        else if(type == 2)
                        {
                            yos.writeUTF(REGIST_SUCCESS_PARAMS+System.currentTimeMillis());
                        }

                        GameServerManager.getInstance().serverListInfo(yos);

                        yos.flush();

                        byte[] serverListMsg = yos.getBytes();

                        ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                                sessionID, serverListMsg);
                    }
                    LoginDAO.saveAccountLoginLog(accountID);
                }
            }
        }
        catch (Exception e)
        {
            Log.error(instance, e);
            e.printStackTrace();
            ret = null;
        }
        finally
        {
            try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
            	e.printStackTrace();
                ret = null;
            }
        }

        return ret;
    }
    
    private byte[] selectServer (int sessionID, short serverID,byte clientType)
    {
        byte[] ret = null;

        try
        {
            int accountID = loginCache.getAccountID(sessionID);
            if (accountID == -1)
            {
                return MsgConst.CLIENTCLOSE;
            }
            else
            {
                GameServer gameServer = GameServerManager.getInstance().getGameServer(serverID);

                if (null != gameServer && gameServer.isRunning())
                {
                    if (gameServer.canLogin())
                    {
                        ArrayList<int[]> lstUID = LoginDAO.getUserIDListByAccountID(accountID, serverID);
                        if (lstUID == null)
                        {
                            return tip(TipConst.SERVER_ERROR, 0);
                        }
                        else
                        {
                            ret = roleList(sessionID, serverID,lstUID, gameServer);
                            loginCache.setServerID(sessionID, serverID);
                        }
                    }
                    else
                    {
                        return tip(TipConst.SERVER_FULL, sessionID);
                    }
                }
                else
                {
                    return tip(TipConst.SERVER_SHUTDOWN, sessionID);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.error(null, e);
            ret = null;
        }

        return ret;
    }

    private byte[] modifyPwd (String username, String oldPwd,String newPwd)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;
        try
        {
            if (0 == newPwd.length() || PASSWORD_LENGTH < newPwd.length())
            {
                return tip(TipConst.NEWPWD_LENGTH_INVALID, 0);
            }

            int checkres = LoginDAO.login(username,oldPwd);
            if(checkres <=0 )
            {
                return tip(TipConst.NAME_OR_PWD_ERROR,0);
            }

            String[] accountInfo = LoginDAO.modifyPassword(username,oldPwd, newPwd);
            if (null != accountInfo)
            {
            	int accountID = LoginDAO.getAccountID(username);
                if(accountID > 0){
                	int res = HttpConnUtil.updatePwdToUserCenter(accountID, oldPwd, newPwd);
                	if(res != 0)
                	{
                		return tip(TipConst.MODIFY_PWD_FAILER, 0);
                	}
                }
                
                yos = new YOYOOutputStream();
                yos.writeShort(ResponseConst.RESPONSE_MODIFYPWD);
                yos.writeUTF(accountInfo[0]);
                yos.writeUTF(accountInfo[1]);
                yos.flush();

                ret = OutputFormat.getInstance().format(GameServer.GAME_ID, 0,yos.getBytes());

                return tip(TipConst.MODIFY_PWD_SUCCESS,0);
 
            }
            else
            {
                return tip(TipConst.NAME_OR_PWD_ERROR, 0);
            }
        }
        catch (Exception e)
        {
            Log.error(instance, e);

            ret = null;
        }
        finally
        {
            try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                ret = null;
            }
        }

        return ret;
    }
    
    private byte[] roleList (int _sessionID, short _serverID,
            ArrayList<int[]> _userIDList, GameServer _rmiInstance)
    {
        YOYOOutputStream yos = null;
        byte[] ret = null;

        try
        {
            if (null != _userIDList)
            {
                yos = new YOYOOutputStream();
                yos.writeShort(ResponseConst.RESPONSE_ROLELIST);
                yos.writeShort(_serverID);
                yos.writeByte(_userIDList.size());
                if (_userIDList.size() > 0)
                {
                    int userIDArray[] = new int[_userIDList.size()];

                    for (int i = 0; i < _userIDList.size(); i++)
                    {
                        yos.writeByte(_userIDList.get(i)[0]);
                        userIDArray[i] = _userIDList.get(i)[1];
                    }

                    byte[] rmiResponse = _rmiInstance.getRoleList(userIDArray);

                    if (rmiResponse != null)
                    {

                        yos.writeBytes(rmiResponse);
                    }
                    else
                    {
                        ret = tip(TipConst.ROLE_LIST_ERROR, 0);

                        return ret;
                    }
                }

                yos.flush();
                ret = OutputFormat.getInstance().format(GameServer.GAME_ID,
                        _sessionID, yos.getBytes());
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
        }
        finally
        {
        	if(yos != null)
        	{
	        	try 
	        	{
                    yos.close();
                    yos = null;
				} 
	        	catch (Exception e2) 
	        	{
				}
        	}
        }

        return ret;
    }
    
    private byte[] createRole (int sessionID, byte seq, byte clan,
            byte sex, byte vocation, String nickname, byte clientType)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;
        try
        {
            int accountID = loginCache.getAccountID(sessionID);
            if (accountID == -1)
            {
                return MsgConst.CLIENTCLOSE;
            }
            else
            {
                short serverID = loginCache.getServerID(sessionID);
                int nameAvailability = NickNameChecker.check(nickname, serverID);
                switch (nameAvailability)
                {
                    case NickNameChecker.NAMEINVAILD:
                    {
                        return tip(TipConst.INVAILDNICKNAME, sessionID);
                    }
                    case NickNameChecker.NAMEEXISTS:
                    {
                        return tip(TipConst.NICKNAME_EXISTS, sessionID);
                    }
                    case NickNameChecker.NAMEHASSPACE:
                    {
                        return tip(TipConst.NICKNAME_HAS_SPACE, sessionID);
                    }
                    default:
                        break;
                }

                GameServer gameServer = GameServerManager.getInstance().getGameServer(serverID);

                if (null != gameServer && gameServer.isRunning())
                {
                    String[] registryInfo = new String[]{
                            String.valueOf(nickname), String.valueOf(clan),
                            String.valueOf(vocation), String.valueOf(sex),
                            String.valueOf(clientType) };

                    byte[] serverResponse = null;
                    int userID = LoginDAO.getUseableUserID();
                    try
                    {
                        serverResponse = gameServer.createRole(accountID,
                                userID, registryInfo);
                    }
                    catch (GameServerShutdownException ex)
                    {
                        return tip(TipConst.SERVER_SHUTDOWN, 0);
                    }
                    if (serverResponse != null)
                    {
                        int returnUserID = Convertor.bytes2Int(serverResponse, 0, 4);
                        if (userID == returnUserID)
                        {
                            if (LoginDAO.createRole(accountID, userID,
                                    seq, nickname, serverID))
                            {
                                yos = new YOYOOutputStream();
                                yos.writeShort(ResponseConst.RESPONSE_NEWROLE);
                                yos.writeByte(seq);
                                yos.writeBytes(serverResponse);
                                yos.flush();
                                ret = OutputFormat.getInstance().format(GameServer.GAME_ID, sessionID, yos.getBytes());
                            }
                            else
                            {
                            	log.error("LoginDAO.createRole fail...");
                                return tip(TipConst.CREATE_ROLE_ERROR, 0);
                            }
                        }
                        else
                        {
                        	log.error("userID != returnUserID fail...");
                            return tip(TipConst.CREATE_ROLE_ERROR, 0);
                        }
                    }
                    else
                    {
                    	log.error("serverResponse == null fail...");
                        return tip(TipConst.CREATE_ROLE_ERROR, 0);
                    }
                }
                else
                {
                    return tip(TipConst.SERVER_SHUTDOWN, 0);
                }
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
            e.printStackTrace();
            ret = null;
        }
        finally
        {
            try
            {
                if (yos != null)
                {
                    yos.flush();
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
            	e.printStackTrace();
                ret = null;
            }
        }

        return ret;
    }

    private byte[] deleteRole (int sessionID, int userID)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;
        try
        {
            short serverID = loginCache.getServerID(sessionID);
            if (serverID == -1)
            {
                return MsgConst.CLIENTCLOSE;
            }
            else
            {
                GameServer gameServer = GameServerManager.getInstance().getGameServer(serverID);

                if (gameServer.isRunning())
                {
                    int deleteRoleResult = -1;
                    try
                    {
                        deleteRoleResult = gameServer.deleteRole(userID);
                    }
                    catch (GameServerShutdownException e)
                    {
                        return tip(TipConst.SERVER_SHUTDOWN, 0);
                    }

                    if (GameServer.DELETE_ROLE_SUCCESS == deleteRoleResult)
                    {
                        if (LoginDAO.deleteRole(userID, serverID))
                        {
                            yos = new YOYOOutputStream();
                            yos.writeShort(ResponseConst.RESPONSE_DELETEROLE);
                            yos.writeInt(userID);
                            ret = OutputFormat.getInstance().format( GameServer.GAME_ID, sessionID, yos.getBytes());
                        }
                    }
                    else
                    {
                        return tip(TipConst.DELETE_ROLE_FAILER, sessionID);
                    }
                }
                else
                {
                    return tip(TipConst.SERVER_SHUTDOWN, 0);
                }
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
            ret = null;
        }
        finally
        {
            try
            {
                if (yos != null)
                {
                    yos.flush();
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                ret = null;
            }
        }

        return ret;
    }

    private byte[] backRoleList (int accountID, short serverID,String clientVersion, String mobile)
    {
        byte[] ret = null;

        try
        {
            int sessionID = loginCache.addLoginInfo(accountID,clientVersion, mobile);

            if (accountID == -1)
            {
                return MsgConst.CLIENTCLOSE;
            }
            else
            {
                GameServer gameService = GameServerManager.getInstance().getGameServer(serverID);

                if (null != gameService && gameService.isRunning())
                {
                    gameService.resetPlayerStatus(accountID);

                    ArrayList<int[]> userIDList = LoginDAO
                            .getUserIDListByAccountID(accountID, serverID);

                    if (userIDList == null)
                    {
                        return tip(TipConst.SERVER_ERROR, 0);
                    }
                    else
                    {
                        ret = roleList(sessionID, serverID, userIDList,gameService);
                        loginCache.setServerID(sessionID, serverID);
                    }
                }
                else
                {
                    return tip(TipConst.SERVER_SHUTDOWN, sessionID);
                }
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
            ret = null;
        }

        return ret;
    }

    private byte[] tip(String tip, int sessionID)
    {
        byte[] ret = null;
        YOYOOutputStream dos = null;
        try
        {
        	dos = new YOYOOutputStream();
            dos.writeShort(ResponseConst.RESPONSE_TIP);
            dos.writeUTF(tip);
            dos.flush();
            ret = OutputFormat.getInstance().format(GameServer.GAME_ID, sessionID,dos.getBytes());
        }
        catch (Exception e)
        {
            Log.error(null, e);
            ret = null;
        }
        finally
        {
        	if(dos != null)
        	{
        		try 
        		{
                    dos.close();
                    dos = null;
				} 
        		catch (Exception e2) 
				{
				}
        	}
        }
        return ret;
    }
    
    private byte[] heartJump (int sessionID, short publisherTag)
    {
        byte[] ret = null;
        try
        {
            if (loginCache.keepActivity(sessionID))
            {
                return MsgConst.HEARTJUMP;
            }
            else
            {
                return MsgConst.CLIENTCLOSE;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ret = null;
        }

        return ret;
    }
    
    private byte[] enterGame(int sessionID, int userID)
    {
        byte[] ret = null;
        YOYOOutputStream yos = null;

        try
        {
            short serverID = loginCache.getServerID(sessionID);
            if (-1 == serverID)
            {
                return MsgConst.CLIENTCLOSE;
            }
            else
            {
                GameServer rmiInstance = GameServerManager.getInstance().getGameServer(serverID);

                if (null != rmiInstance)
                {
                    if (rmiInstance.isRunning())
                    {
                        int accountID = loginCache
                                .getAccountID(sessionID);
                        int sID = rmiInstance.createSession(userID,accountID);

                        if (-1 != sID)
                        {
                            yos = new YOYOOutputStream();
                            yos.writeShort(ResponseConst.RESPONSE_GAMESERVERURL);
                            yos.writeInt(sID);
                            yos.writeUTF(rmiInstance.getHttpURLAddr());
                            yos.writeUTF(rmiInstance.getHttpURLContext());
                            yos.writeUTF(rmiInstance.getTcpURL());

                            ret = OutputFormat.getInstance().format(GameServer.GAME_ID, sessionID, yos.getBytes());

                            loginCache.clean(sessionID);
                        }
                        else
                        {
                            return tip(TipConst.SESSION_ERROR, 0);
                        }
                    }
                    else
                    {
                        return tip(TipConst.SERVER_SHUTDOWN, 0);
                    }
                }
                else
                {
                    return tip(TipConst.DISCONNECTION, 0);
                }
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
            e.printStackTrace();
            ret = null;
        }
        finally
        {
            try
            {
                if (yos != null)
                {
                    yos.close();
                    yos = null;
                }
            }
            catch (Exception e)
            {
                ret = null;
            }
        }

        return ret;
    }
    
    private boolean isPseudoMobile (String mobile)
    {
        return mobile.startsWith(PSEUDO_MOBILE_PREFIX);
    }

    private byte checkVersion (String clientVersion)
    {
    	if (clientVersion.compareTo(VersionInfo.curVersion) < 0)
        {
            return TipConst.TYPE_NEWVERSION_EXISTS;
        }
    	else if (clientVersion.compareTo(VersionInfo.compVersion) < 0)
        {
            return TipConst.TYPE_MUST_UPGRADE_CLIENT;
        }
        else
        {
            return -1;
        }
    }

    class ReSendSZFResultTask extends TimerTask
    {
        @Override
        public void run() 
        {
            reSendSZFResultMap = LoginDAO.getNotToServerSZFCharge();

            for (Iterator<String> it = reSendSZFResultMap.keySet().iterator(); it.hasNext();)
            {
                String[] info = reSendSZFResultMap.get(it.next());
                int userID = Integer.parseInt(info[0]);
                String transID = info[1];
                int serverID = Integer.parseInt(info[2]);
                String orderID = info[3];
                byte result = Byte.parseByte(info[4]);
                int point = Integer.parseInt(info[5]);
                int type = Integer.parseInt(info[6]);
                GameServer gameService = GameServerManager.getInstance().getGameServer((short)serverID);
                if(gameService.isRunning()){
                    try {
                        int res = gameService.szfFeeCallBack(userID,transID,result,orderID,point);
                        if(res == 1){
                            LoginDAO.updateSZFResultFlag(orderID,result,1,point,transID,type);

                            it.remove();
                        }

                    } catch (GameServerShutdownException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}


