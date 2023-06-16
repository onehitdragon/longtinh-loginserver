 package yoyo.server.loginserver;
 
 import java.io.IOException;
 import javax.servlet.ServletException;
 import javax.servlet.ServletOutputStream;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import yoyo.server.login.LoginServer;
 import yoyo.server.login.tools.Log;
 
 public class ResetPlayerStatus extends HttpServlet
 {
   private static final String CONTENT_TYPE = "text/html; charset=UTF-8";
   private LoginServer loginServer;
 
   public void doPost(HttpServletRequest request, HttpServletResponse response)
     throws ServletException, IOException
   {
     response.setContentType("text/html; charset=UTF-8");
     try
     {

		String accountID = request.getParameter("accountID");
Log.info("ResetPlayerStatus accoountid= "+accountID);
		this.loginServer.resetPlayerStatus(Integer.parseInt(accountID));

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
		 String accountID = request.getParameter("accountID");
		 Log.info("ResetPlayerStatus accoountid= "+accountID);
		this.loginServer.resetPlayerStatus(Integer.parseInt(accountID));

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

