package yoyo.server.login.constvalue;

import yoyo.server.login.OutputFormat;
import yoyo.server.login.GameServer;
import yoyo.server.login.tools.Log;
import yoyo.server.login.tools.YOYOOutputStream;


public class MsgConst
{
    public static byte[] NEWCLIENTEXISTS;
    public static byte[] HEARTJUMP;
    public static byte[] CLIENTCLOSE;
    public static byte[] SERVICENOTREADY;

    public static void init ()
    {
    	YOYOOutputStream dos = null;
        try
        {
            dos = new YOYOOutputStream();

            dos.reset();
            dos.writeShort(ResponseConst.RESPONSE_OF_HEARTJUMP);
            dos.flush();
            HEARTJUMP = OutputFormat.getInstance().format(GameServer.GAME_ID, 0, dos.getBytes());

            dos.reset();
            dos.writeShort(ResponseConst.RESPONSE_CLOSECLIENT);
            dos.writeUTF(TipConst.ERROR_REQUEST);
            dos.flush();
            CLIENTCLOSE = OutputFormat.getInstance().format(GameServer.GAME_ID, 0, dos.getBytes());

            dos.reset();
            dos.writeShort(ResponseConst.RESPONSE_TIP);
            dos.writeUTF(TipConst.LOGIN_SERVICE_NOT_READY);
            dos.flush();
            SERVICENOTREADY = OutputFormat.getInstance().format(GameServer.GAME_ID, 0,dos.getBytes());

        }
        catch (Exception e)
        {
            Log.error(null, e);
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
        		catch (Exception e) 
				{
				}
        	}
        }
    }
}
