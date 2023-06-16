/*    */ package yoyo.server.loginserver;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import javax.servlet.ServletException;
/*    */ import javax.servlet.ServletOutputStream;
/*    */ import javax.servlet.http.HttpServlet;
/*    */ import javax.servlet.http.HttpServletRequest;
/*    */ import javax.servlet.http.HttpServletResponse;
/*    */ import yoyo.server.login.LoginServer;
/*    */ import yoyo.server.login.tools.Log;
/*    */ 
/*    */ public class LoginServlet extends HttpServlet
/*    */ {
/*    */   private static final String CONTENT_TYPE = "text/html; charset=UTF-8";
/*    */   private LoginServer loginServer;
/*    */ 
/*    */   public void doGet(HttpServletRequest request, HttpServletResponse response)
/*    */     throws ServletException, IOException
/*    */   {
/* 23 */     response.setContentType("text/html; charset=UTF-8");
/*    */     try
/*    */     {
/*    */		String content = request.getHeader("datastart");
/* 28 */       if (content == null) return;
/* 29 */       byte[] responseContent = this.loginServer.process(content);
/*    */ 
/* 31 */       if (responseContent != null) {
/* 32 */         response.getOutputStream().write(responseContent);
/* 33 */         response.getOutputStream().flush();
/* 34 */         response.getOutputStream().close();
/*    */       } else {
/* 36 */         Log.println("response null");
/*    */       }
/*    */     } catch (Exception e) {
/* 39 */       Log.error(null, e);
/* 40 */       e.printStackTrace();
/*    */     }
/*    */   }
/*    */ 
/*    */   public void init() throws ServletException
/*    */   {
    System.out.println("vinh inittttttttttttt ... " + this.getClass().getName());
/*    */     try {
/* 47 */       this.loginServer = LoginServer.getInstance();
/* 48 */       this.loginServer.init();
/*    */     } catch (Exception e) {
/* 50 */       Log.error(null, e);
/* 51 */       e.printStackTrace();
/*    */     }
/*    */   }
/*    */ 
/*    */   public void destroy()
/*    */   {
/*    */   }
/*    */ 
/*    */   static
/*    */   {
/* 17 */     Log.init();
/*    */   }
/*    */ }