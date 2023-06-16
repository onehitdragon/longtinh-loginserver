package yoyo.server.login.channel;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import yoyo.server.login.tools.M;


public class DCNService {
    private static Logger log = Logger.getLogger(DCNService.class);
	
	public static final String LOGIN_URL = "http://app.d.cn/connect/member/login";// login url"http://202.142.19.66:8877/connect/member/login";///
	public static final String SYNC_URL = "http://app.d.cn/connect/member/autosync";// sync url
	public static final String BBS_TOPIC_LIST_URL = "http://app.d.cn/connect/forum/list";//bbs topic list
	public static final String TOPIC_URL = "http://app.d.cn/connect/forum/show-";//topic url
	public static final String NEW_TOPIC_URL = "http://app.d.cn/connect/forum/new-topic";//new topic
	public static final String REPLY_TOPIC_URL = "http://app.d.cn/connect/forum/reply-";//new topic
	public static final String UPLOAD_GAMEIMG_URL = "http://app.d.cn/connect/forum/game-img";//new topic
	
	public static final String API_KEY = "40"; // 应用ID
	public static final String CHARSET = "utf-8";
	private static final String KEY = "sdf5432c"; // 密钥KEY
	public static final String CHANNEL_ID = "1001";//渠道ID

	/**
	 * 获取当乐渠道的登录url
	 * @author rlj
	 * @param mid
	 * @param username
	 * @param pwd
	 * @return
	 */
	public static String getLoginUrl(String mid, String username, String pwd) {
		StringBuffer sbu = new StringBuffer();
		try {
			long time = System.currentTimeMillis();
			String verString = new StringBuffer().append("api_key=").append(API_KEY)
			.append("&call_id=").append(time).append("&mid=").append(mid).append("&username=").append(java.net.URLEncoder.encode(username, CHARSET)).toString();
			System.out.println("verString:" + verString);
			System.out.println("pwd sha256:" + M.sha256(pwd, CHARSET).toUpperCase());
			String sig = DCNService.getSign(new StringBuffer().append(verString).append("&sha256_pwd=").append(M.sha256(pwd, CHARSET).toUpperCase())
					.append("&secret_key=").append(KEY).toString()).toUpperCase();
			String vc = DCNService.getSign(new StringBuffer().append(verString).append("&sig=").append(sig).toString()).toUpperCase();
			sbu.append(LOGIN_URL).append("?").append(verString).append("&vc=").append(vc).append("&sig=").append(sig);
			log.info("login_url" + sbu.toString());
		} catch(Exception ex) {
			log.error("生成登录得url", ex);
		}
		return sbu.toString();
	}
	
	/**
	 * 登录当乐接口
	 * @author rlj
	 * @param mid
	 * @param username
	 * @param pwd
	 * @return
	 */
	public static Result login(String mid, String username, String pwd) {
		InputStream reStr = M.httpRequest(getLoginUrl(mid,username,pwd), "当乐用户" + mid + username + "登录"); 
		Result result = new Result();
		result.setResult(false);
		result.setReList("");
		if (reStr != null) {
			try {  
				SAXReader reader = new SAXReader();
				Document doc = reader.read(reStr);
				Element root = doc.getRootElement();
				if(root != null) {
					Attribute attribute = root.attribute("status");
					if(attribute != null) {
						String status = attribute.getValue();
						System.out.println("status:" + status);
						if(status.equals("0")) {
							Element user = root.element("user");
							String mid2 = user.element("mid").getText();
							result.setResult(true);
							result.setReList(mid2);
							log.info("mid:" + mid2);
						} else {
							result.setReList(status);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("当乐登录请求返回" + reStr);
		}
		return result;
	}
	
	/**
	 * 获取当乐的同步url
	 * @author rlj
	 * @param mid
	 * @param username
	 * @param pwd
	 * @return
	 */
	public static String getSyncUrl(String unique_id,String playerName, String pwd) {
		long time = System.currentTimeMillis();
		StringBuffer ver = new StringBuffer();
		ver.append("api_key=").append(API_KEY).append("&call_id=").append(time).append("&unique_id=").append(unique_id)
		.append("&show_name=").append(playerName).append("&sha256_pwd=").append(pwd);
		
		String sig = DCNService.getSign(new StringBuffer().append("api_key=")
				.append(API_KEY).append("&call_id=").append(time).append("&unique_id=").append(unique_id)
				.append("&show_name=").append(playerName).append("&sha256_pwd=").append(pwd)
				.append("&secret_key=").append(KEY).toString()).toUpperCase();
		StringBuffer sbu = new StringBuffer();
		sbu.append(SYNC_URL).append("?").append(ver.toString()).append("&sig=").append(sig);
		return sbu.toString();
	}
	
	/**
	 * 发起同步
	 * @author rlj
	 * @param mid
	 * @param username
	 * @param pwd
	 * @return
	 */
	public static Result sys(String unique_id,String playerName, String pwd) {
		InputStream reStr = M.httpRequest(getSyncUrl(unique_id,playerName,pwd), "当乐同步用户unique_id" + unique_id); 
		Result result = new Result();
		result.setResult(false);
		result.setReList("");
		if (reStr != null) {
			try {  
				SAXReader reader = new SAXReader();
				Document doc = reader.read(reStr);
				Element root = doc.getRootElement();
				if(root != null) {
					Attribute attribute = root.attribute("status");
					if(attribute != null) {
						String status = attribute.getValue();
						if(status.equals("0")) {
							Element user = root.element("user");
							result.setResult(true);
							result.setReList(user.element("mid").getText());
						} else {
							result.setReList(status);
						}
					}
				}
			} catch (Exception e) {
				log.error("当乐用户同步接口:",e);
			}
		} else {
			log.info("当乐登录请求返回" + reStr);
		}
		return result;
	}
	
	public static String getSign(String params) {
		String dString = M.md5(params,CHARSET);
		return dString;
	}
	
//	hMap = new HashMap();
//	hMap.put("0", "验证成功");
//	hMap.put("1", "未知错误");
//	hMap.put("201", "call_id错误");
//	hMap.put("202", "sig计算错误或密码错误");
//	hMap.put("203", "api_key错误");
//	hMap.put("204", "未知参数错误");
//	hMap.put("302", "mid或username错误");
//	hMap.put("601", "无此用户");
	
}