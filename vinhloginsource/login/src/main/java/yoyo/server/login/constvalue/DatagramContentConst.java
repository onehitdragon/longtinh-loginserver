package yoyo.server.login.constvalue;

import yoyo.server.login.OutputFormat;
import yoyo.server.login.GameServer;
import yoyo.server.login.tools.Log;
import yoyo.server.login.tools.YOYOOutputStream;


public class DatagramContentConst
{
    public static byte[] NEW_VERSION_TIP;

    public static byte[] HEART_BEAT;

    public static byte[] CLOSE_CLIENT;

    public static byte[] SERVICE_UNINIT;

    public static void init ()
    {
        try
        {
            YOYOOutputStream dos = new YOYOOutputStream();

            dos.reset();
            dos.writeShort(ResponseConst.RESPONSE_OF_HEARTJUMP);
            dos.flush();
            HEART_BEAT = OutputFormat.getInstance().format(GameServer.GAME_ID, 0, dos
                    .getBytes());

            dos.reset();
            dos.writeShort(ResponseConst.RESPONSE_CLOSECLIENT);
            dos.flush();
            CLOSE_CLIENT = OutputFormat.getInstance().format(GameServer.GAME_ID, 0, dos
                    .getBytes());

            dos.reset();
            dos.writeShort(ResponseConst.RESPONSE_TIP);
            dos.writeUTF(TipConst.LOGIN_SERVICE_NOT_READY);
            dos.flush();
            SERVICE_UNINIT = OutputFormat.getInstance().format(GameServer.GAME_ID, 0,
                    dos.getBytes());

            dos.close();
            dos = null;
        }
        catch (Exception e)
        {
            Log.error(null, e);
        }
    }
}
