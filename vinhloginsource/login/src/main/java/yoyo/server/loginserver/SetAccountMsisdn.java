 package yoyo.server.loginserver;
 
 import java.io.IOException;
 import javax.servlet.ServletException;
 import javax.servlet.ServletOutputStream;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import yoyo.server.login.LoginServer;
 import yoyo.server.login.tools.Log;

 import java.net.HttpURLConnection;
 import java.net.URL;
 
 public class SetAccountMsisdn extends HttpServlet
 {
   private static final String CONTENT_TYPE = "text/html; charset=UTF-8";
   private LoginServer loginServer;
 
   public void doPost(HttpServletRequest request, HttpServletResponse response)
     throws ServletException, IOException
   {
     response.setContentType("text/html; charset=UTF-8");
     try
     {
		 String s = request.getParameter("s");//启动:start_1231231,登录:login_1231231,注册:regist_13123132
		 System.out.println("vinh ------ " + s);
Log.info("@@ SetAccountMsisdnServlet ... s="+s);
		 String msisdn = request.getParameter("msisdn");
//		 if(s == null || s.trim().length()==0){
		if(s.startsWith("login") || s.startsWith("regist")){//登录或注册获取的手机号
		   String accountID = request.getParameter("accountID");		   
		   String agent = request.getParameter("UA");
 Log.info("@@ SetAccountMsisdnServlet ... accountid="+accountID+",msisdn="+msisdn+",agent="+(new String(agent.getBytes("ISO-8859-1"),"UTF-8")));
		   if (accountID == null || msisdn == null){
				response.getWriter().println(msisdn+"_"+(-1));
				response.getWriter().flush();
				response.getWriter().close();
				return;
		   }
		   if(agent != null && agent.trim().length()>0)
				agent = new String(agent.getBytes("ISO-8859-1"),"UTF-8");
		   int responseContent = this.loginServer.setAccountMobileUA(Integer.parseInt(accountID),msisdn,agent,s);
Log.info("@@ SetAccountMsisdnServlet responseContent == " + responseContent); 
		   if (responseContent == Integer.parseInt(accountID)) {//成功
			 response.getWriter().println(msisdn);
		   } else {
			 response.getWriter().println(msisdn+"_"+0);
			 //Log.println("response = " + responseContent);
		   }
		   
		 }else{//游戏启动时也要获取一次手机号，发回来只保存手机号
Log.info("@@ post SetAccountMsisdnServlet addMobile == " + msisdn); 
			this.loginServer.addMobile(msisdn);
			response.getWriter().println(msisdn);
		 }
		 response.getWriter().flush();
		 response.getWriter().close();
		
     } catch (Exception e) {
       Log.error(null, e);
       e.printStackTrace();
     }
   }
   public void doGet(HttpServletRequest request, HttpServletResponse response)
     throws ServletException, IOException
   {
     response.setContentType("text/html; charset=UTF-8");
     try
     {
		 String s = request.getParameter("s");
Log.info("@@ SetAccountMsisdnServlet ... s="+s);
       String accountID = request.getParameter("accountID");
	   String msisdn = request.getParameter("msisdn");
		if(s.startsWith("login") || s.startsWith("regist")){
		   String agent = request.getParameter("UA");
Log.info("@@ doget SetAccountMsisdnServlet ... accountid="+accountID+",msisdn="+msisdn+",s="+s+",agent="+ (new String(agent.getBytes("ISO-8859-1"),"UTF-8")));
		   if (accountID == null || msisdn == null){
				response.getWriter().println(msisdn+"_"+(-1));
				response.getWriter().flush();
				response.getWriter().close();
				return;
		   }
		   if(agent != null && agent.trim().length()>0)
				agent = new String(agent.getBytes("ISO-8859-1"),"UTF-8");
		   int responseContent = this.loginServer.setAccountMobileUA(Integer.parseInt(accountID),msisdn,agent,s);
	Log.info("@@ doget SetAccountMsisdnServlet responseContent == " + responseContent); 
		   if (responseContent == Integer.parseInt(accountID)) {//成功
			 response.getWriter().println(msisdn);
		   } else {
			 response.getWriter().println(msisdn+"_"+0);
		   }
	   }else{
Log.info("@@ doget SetAccountMsisdnServlet addMobile == " + msisdn); 
			this.loginServer.addMobile(msisdn);
			response.getWriter().println(msisdn);
	   }
	   response.getWriter().flush();
       response.getWriter().close();
     } catch (Exception e) {
       Log.error(null, e);
       e.printStackTrace();
     }
   }
 
   public void init() throws ServletException
   {
     try {
       this.loginServer = LoginServer.getInstance();
     } catch (Exception e) {
       Log.error(null, e);
       e.printStackTrace();
     }
   }
 
   public void destroy()
   {
   }
 
   static
   {
     Log.init();
   }
 }

