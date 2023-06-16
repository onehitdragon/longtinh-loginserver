package yoyo.server.login.tools;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.Random;

import org.apache.log4j.Logger;

import yoyo.server.login.GameServerManager;


public class HttpConnUtil {
	private static Logger log = Logger.getLogger(HttpConnUtil.class);
	/**
	 * 获取账号ID  URL
	 * 返回 帐号id (6位起)
	 */
	private static String GET_ACCOUNT_ID_URL = "http://112.25.14.24:80/usercenter/createpassportid.php";
	/**
	 * 注册信息录入接口
	 * [post]参数：
		userid 帐号id
		username 用户名
		userpass 密码
		pid 渠道id
		成功 返回 0 失败返回非0值
	 */
	private static String REGISTE_TO_USER_CENTER_URL = "http://112.25.14.24:80/usercenter/register.php";

    static Random random = new Random(1000000);

	/**
	 * 密码修改接口
	 * [post]参数：
	 *  userid 帐号id
	 *  userpass 旧密码
	 *  newpassword 新密码
	 *  成功 返回 0 失败返回非0值
	 */
	private static String UPDATE_PASSWORD_TO_USER_CENTER_URL = "http://112.25.14.24:80/usercenter/changepass.php";

	/**
	 * 获取账号ID  URL
	 * 返回 帐号id (6位起)
	 * 失败返回 -1
	 * @return
	 */
	public static  int getAccountID(){
		int accountID = -1;
		try {
			GET_ACCOUNT_ID_URL = GameServerManager.getInstance().getGET_ACCOUNT_ID_URL();
//			URL url = new URL(GameServiceListManager.getInstance().getGET_ACCOUNT_ID_URL());
//            int ran = random.nextInt();
//            System.out.println("rn == " + ran);
//			URL url = new URL(GET_ACCOUNT_ID_URL+"?r="+System.currentTimeMillis()+ran);
            URL url = new URL(GET_ACCOUNT_ID_URL+"?r="+System.currentTimeMillis());
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			log.info("GET_ACCOUNT_ID_URL code = " + conn.getResponseCode());
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while((line = reader.readLine()) != null){//因为这里只会返回一个值，所以用的 if ，而没有 while
				System.out.println("line = " + line);
				accountID = Integer.parseInt(line);
			}
			log.info("Httpconnect getaccountID = " + accountID);
			reader.close();
			conn.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			log.error("获取账号ID error: ",e);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("获取账号ID error: ",e);
			e.printStackTrace();
		} catch(Exception e){
			log.error("获取账号ID error: ",e);
			e.printStackTrace();
		}

		return accountID;
	}
	
	/**
	 * 注册信息录入接口
	 * 给王林就那边做信息同步.
	 * [post]参数：
		userid 帐号id
		username 用户名
		userpass 密码
		pid 渠道id
		成功 返回 0 失败返回非0值
	 * @return
	 */
	public static int registeToUserCenter(int accountID, String username, String pwd,int pid){
		int res = -1;
		StringBuffer sf = new StringBuffer();
		sf.append("userid=").append(accountID)
			.append("&")
			.append("username=").append(username)
			.append("&")
			.append("userpass=").append(pwd)
			.append("&")
			.append("pid=").append(pid);
		try {
			REGISTE_TO_USER_CENTER_URL = GameServerManager.getInstance().getREGISTE_TO_USER_CENTER_URL();
//			URL url = new URL(GameServiceListManager.getInstance().getREGISTE_TO_USER_CENTER_URL());
			URL url = new URL(REGISTE_TO_USER_CENTER_URL);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Charset", "UTF-8");
			
			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
			writer.write(sf.toString());
			writer.flush();
			
			log.info("REGISTE_TO_USER_CENTER_URL code = " + conn.getResponseCode());
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			if((line = reader.readLine()) != null){//因为这里只会返回一个值，所以用的 if ，而没有 while
//				System.out.println("res line = " + line);
				res = Integer.parseInt(line);
			}
			log.info("REGISTE_TO_USER_CENTER_URL res = " + res);
			reader.close();
			conn.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			log.error("注册信息录入接口 error: ",e);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("注册信息录入接口 error: ",e);
			e.printStackTrace();
		}
		return res;
	}
	/**
	 * 密码修改接口
	 * 给王林就那边做信息同步.
	 * [post]参数：
	 *  userid 帐号id
	 *  userpass 旧密码
	 *  newpassword 新密码
	 *  成功 返回 0 失败返回非0值
	 */
	public static int updatePwdToUserCenter(int accountID,String oldPass,String newPass){
		int res = -1;
		StringBuffer sf = new StringBuffer();
		sf.append("userid=").append(accountID)
			.append("&")
			.append("userpass=").append(oldPass)
			.append("&")
			.append("newpassword=").append(newPass);
		try {
//			UPDATE_PASSWORD_TO_USER_CENTER_URL = GameServiceListManager.getInstance().getUPDATE_PASSWORD_TO_USER_CENTER_URL();
//			URL url = new URL(GameServiceListManager.getInstance().getUPDATE_PASSWORD_TO_USER_CENTER_URL());
			URL url = new URL(UPDATE_PASSWORD_TO_USER_CENTER_URL);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Charset", "UTF-8");
			
			
			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
			writer.write(sf.toString());
			writer.flush();
			
			System.out.println("UPDATE_PASSWORD_TO_USER_CENTER_URL code = " + conn.getResponseCode());
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			if((line = reader.readLine()) != null){//因为这里只会返回一个值，所以用的 if ，而没有 while
//				System.out.println("res line = " + line);
				res = Integer.parseInt(line);
			}
			log.info("updatePwdToUserCenter res = " + res);
			reader.close();
			conn.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch blocks
			System.out.println("密码修改接口 error");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("密码修改接口 error");
			e.printStackTrace();
		}
		return res;
	}

    /**
     * 密码短信找回接口
     * 账号绑定手机号可以找回密码
     * @param smsurl 接口url
     * @param msisdn   绑定的手机号
     * @param content   发送内容
     * @return  同步返回：状态码#状态信息（0成功；非0失败）2#内容不合法  3#超过请求次数
     */
    public static int sendPwdBackSMS(String smsurl,String msisdn,String content){
        int res = 1;
        StringBuffer sf = new StringBuffer();
        sf.append("areaid=").append(1)
            .append("&")
            .append("mobile=").append(msisdn)
            .append("&")
            .append("message=").append(content);
        log.debug("send pwd sms = "+sf.toString());
        try{
//            smsurl = "http://112.25.14.24/usercenter/getpassbysms.php";
            log.debug("pwd back sms url = "+smsurl);
            URL url = new URL(smsurl);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", "UTF-8");

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
			writer.write(sf.toString());
			writer.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"));
			String line = reader.readLine();
            log.info("密码短信找回,发送短信同步结果："+line);
            System.out.println("密码短信找回,发送短信同步结果："+line);
            if(line.indexOf("#")>0){
                String[] ls = line.split("#");
                res = Integer.parseInt(ls[0]);

            }

            reader.close();
            conn.disconnect();
        }catch (MalformedURLException e) {
			// TODO Auto-generated catch blocks
			System.out.println("密码找回接口 error");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("密码找回接口 error");
			e.printStackTrace();
		}
        return res;
    }

    /**
     * 获取九天和英雄游戏的下载地址
     *
     * @return
     */
    public static String[][] getNgGameDownloadUrl(String downloadflag){
        String[][] res = null;
        try {
            URL url = new URL(GameServerManager.getInstance().getDownloadNgGameUrl());
//            URL url = new URL("http://112.25.14.24/ota/panel/cmgame.php");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = br.readLine();   //返回 hero>游戏入口地址#jiutian>游戏入口地址  *返回内容urlencode编码
            line = URLDecoder.decode(line,"UTF-8");

            String[][] downloandurls = null;

            if(line != null && line.indexOf("#")>0){
                String [] durls = line.split("#");
                String name1 = durls[0].substring(0,durls[0].indexOf(">"));
                String name2 = durls[1].substring(0,durls[1].indexOf(">"));
                String url1 = durls[0].substring(durls[0].indexOf(">")+1);
                String url2 = durls[1].substring(durls[1].indexOf(">")+1);
                log.info("ng game download hero = " + durls[0]+", jiutian="+durls[1]);

                downloandurls = new String[2][2];
                downloandurls[0][0] = name1;
                downloandurls[0][1] = url1;
                downloandurls[1][0] = name2;
                downloandurls[1][1] = url2;
            }
            if(downloandurls != null){
                if(downloadflag == null){
                    res = downloandurls;
                }else{
                    if(downloadflag.equals(downloandurls[0][0])){
                        res = new String[1][2];
                        res[0][0] = downloandurls[1][0];
                        res[0][1] = downloandurls[1][1];
                    }
                    if(downloadflag.equals(downloandurls[1][0])){
                        res = new String[1][2];
                        res[0][0] = downloandurls[0][0];
                        res[0][1] = downloandurls[0][1];
                    }
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }



	
	public static void main(String[] args){
        String content = "[仙镜]找回密码通知，您的密码是：123123";
        String msisdn = "13401104309";
        sendPwdBackSMS("http://112.25.14.24/usercenter/getpassbysms.php",msisdn,content);
//        getNgGameDownloadUrl(null);
//        for(int i=0; i<30; i++){
//            int accountid = getAccountID();
//            System.out.println("get accountID = " +i +":" + accountid);
//        }
//		String username = "sfds09";
//		String password = "123456";
//		String newpass = "000000000";
//		registeToUserCenter(accountid,username,password,1);
//		updatePwdToUserCenter(112137,"11111111","000000");
//		setAccountMsisdn(accountid, "13111111111", "中文手机");
		
//		System.out.println(System.currentTimeMillis());
		
//		String a = "aba";
//		System.out.println("a = " + a.replaceAll("\\]", "\\$").replaceAll("\\[", "\\$"));
		
	/*	Map<Integer,Integer> map = new HashMap<Integer, Integer>();
		for(int i=0; i<5; i++){
			map.put(i, i+10);
		}
		
		System.out.println("map key 3 = " + map.get(9));
		
		for(Iterator<Integer> it = map.keySet().iterator(); it.hasNext();){
			if(it.next() == 3){
				it.remove();
				break;
			}
		}
		for(Iterator<Integer> it = map.keySet().iterator(); it.hasNext();){
			System.out.println("s  = " + it.next());
		}
		
		for(Iterator<Integer> it = map.values().iterator(); it.hasNext();){
			if(it.next() == 11){
				it.remove();
				break;
			}
		}
		
		for(Iterator<Integer> it = map.keySet().iterator(); it.hasNext();){
			System.out.println("t  = " + it.next());
		}*/
		
//		String name="aaa||";
//		System.out.println(name.indexOf("|"));
	}
	
	/**
	 * 此方法测试用
	 * @param accountID
	 * @param msisdn
	 * @param UA
	 * @return
	 */
	private static int setAccountMsisdn(int accountID,String msisdn,String UA){
		int res = -1;
		StringBuffer sf = new StringBuffer();
		sf.append("accountID=").append(accountID)
			.append("&")
			.append("msisdn=").append(msisdn)
			.append("&")
			.append("UA=").append(UA);
		try {
//			URL url = new URL("http://localhost/xj_login/hero_set_msisdn");//?"+sf.toString());
			URL url = new URL("http://220.194.59.171:8000/zdxn.php");
//			URL url = new URL("http://localhost/gmTools/login/addchat");

			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Charset", "UTF-8");
			
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(conn.getOutputStream()));
//			writer.write(sf.toString());
			writer.write("u="+accountID);
//			writer.write("serverID=2&content=聊天内容：ASFASDFS都谁在吧");
			writer.flush();
			
			System.out.println("setAccountMsisdn code = " + conn.getResponseCode());
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while((line = reader.readLine()) != null){
				System.out.println("res line = " + line);
//				res = Integer.parseInt(line);
			}
//			System.out.println("updatePwdToUserCenter res = " + res);
			reader.close();
			conn.disconnect();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
}
