package yoyo.server.login;

import hero.login.rmi.IYOYOLoginRMI;
import yoyo.server.login.config.RMIConfig;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class GameServer
{
    public static final byte DELETE_ROLE_SUCCESS = 1;
    public static final byte GAME_ID  = 1;
    private int          maxPlayerCount;
    private int          playerCount;
    private boolean      status;
    private short        serverID;
    private String       serverName;
    private int          index;
    private String       tcpURL;
    private String       httpURL;
    private String       httpContext;
    public short onTop;
    private IYOYOLoginRMI rmi;
    
    public short getServerID ()
    {
        return serverID;
    }
    
    public String getServerName ()
    {
        return serverName;
    }

    public String getHttpURLAddr ()
    {
        return httpURL;
    }

    public String getHttpURLContext ()
    {
        return httpContext;
    }

    public String getTcpURL ()
    {
        return tcpURL;
    }

    public boolean isRunning ()
    {
        return status;
    }

    public int getIndex ()
    {
        return index;
    }

    public void setIndex (int _index)
    {
        index = _index;
    }
    
    public int getPlayerCount()
    {
        return playerCount;
    }
    /**
     * 服务器人数状态--1：流畅    2:火爆  3:爆满
     */
    private byte serverPlayerNumberStatus;

    public byte getServerPlayerNumberStatus() 
    {
		return serverPlayerNumberStatus;
	}

	public void setServerPlayerNumberStatus(byte serverPlayerNumberStatus) 
	{
		this.serverPlayerNumberStatus = serverPlayerNumberStatus;
	}

    public GameServer(short serverID, String serverName, int inx,
            int maxPlayerCount, String httpURL,
            String httpCoxt, String tcpURL)
    {
        this.serverID = serverID;
        this.serverName = serverName;
        this.index = inx;
        this.maxPlayerCount = maxPlayerCount;
        this.httpURL = httpURL;
        this.httpContext = httpCoxt;
        this.tcpURL = tcpURL;
        findGameServer();
    }

    public byte[] createRole (int accountID, int userID, String[] paras)
            throws GameServerShutdownException
    {
        if (rmi == null)
        {
            throw new GameServerShutdownException();
        }

        try
        {
            return rmi.createRole(accountID, serverID, userID, paras);
        }
        catch (RemoteException re)
        {
            throw new GameServerShutdownException();
        }
    }

    public byte[] getRoleList (int[] userIDList)
            throws GameServerShutdownException
    {
        if (rmi == null)
        {
            throw new GameServerShutdownException();
        }

        try
        {
            return rmi.listRole(userIDList);
        }
        catch (Exception re)
        {
            throw new GameServerShutdownException();
        }
    }
    
    public byte[] getDefaultRoleList () throws GameServerShutdownException {
        if (rmi == null)
        {
            throw new GameServerShutdownException();
        }

        try
        {
System.out.println("game service getDefaultRoleList  @#@#");
            return rmi.listDefaultRole();
        }
        catch (Exception re)
        {
        	re.printStackTrace();
            throw new GameServerShutdownException();
        }
    }

    public int deleteRole (int userID) throws GameServerShutdownException
    {
        if (rmi == null)
        {
            throw new GameServerShutdownException();
        }

        try
        {
        	System.out.println("准备执行RMI删除过程  userID="+userID );
        	int result = rmi.deleteRole(userID);
        	System.out.println("执行RMI删除角色结果:"+result);
            return result;
        }
        catch (RemoteException re)
        {
        	System.out.println("删除角色的时候服务器已经关闭");
            throw new GameServerShutdownException();
        }
    }

    /**
     * 神州付回调接口
     *
     * @param userID
     * @param transID
     * @param result  1:支付成功   2:支付失败
     * @param orderid
     * @throws GameServerShutdownException
     * @return 1:成功  0:失败
     */
    public int szfFeeCallBack(int userID, String transID, byte result, String orderid,int point) throws GameServerShutdownException {
        int res = 0;
        if (rmi == null)
        {
            throw new GameServerShutdownException();
        }

        try
        {
            res = rmi.szfFeeCallBack(userID,transID,result,orderid,point);
        }
        catch (RemoteException re)
        {
            throw new GameServerShutdownException();
        }
        return res;
    }

    /**
     * 短信回调
     * @param transID
     * @param result
     * @throws GameServerShutdownException
     */
    public void smsCallBack(String transID,String result) throws GameServerShutdownException{
        if(rmi == null){
            throw new GameServerShutdownException();
        }
        try{
            rmi.smsCallBack(transID,result);
        }catch (RemoteException re)
        {
            throw new GameServerShutdownException();
        }
    }

    public void serverStatus ()
    {
        if (rmi == null)
        {
            status = false;
            playerCount = 0;
        }

        try
        {
            rmi.checkStatusOfRun();
            status = true;
            playerCount = rmi.getOnlinePlayerNumber();
        }
        catch (RemoteException re)
        {
            rmi = null;
            status = false;
            playerCount = 0;
        }
    }

    /**
     * 将帐号下的角色状态置为下线
     * 
     * @param _accountID 帐号ID
     * @return 是否完成状态重置
     * @throws GameServerShutdownException
     */
    public boolean resetPlayerStatus (int _accountID)
            throws GameServerShutdownException
    {
        if (rmi == null)
        {
            throw new GameServerShutdownException();
        }

        try
        {
            return rmi.resetPlayersStatus(_accountID);
        }
        catch (RemoteException re)
        {
            throw new GameServerShutdownException();
        }
    }

    public int createSession (int userID, int accountID)
            throws GameServerShutdownException
    {
        if (rmi == null)
        {
            throw new GameServerShutdownException();
        }

        try
        {
            return rmi.createSessionID(userID, accountID);
        }
        catch (RemoteException re)
        {
            throw new GameServerShutdownException();
        }
    }

    public boolean canLogin ()
    {
    	return playerCount <= maxPlayerCount
    				&& getServerPlayerNumberStatus() < GameServerManager.FULL_STATUS;
    }

    public void findGameServer ()
    {
        try
        {
            int rmi_port = Integer.parseInt(RMIConfig.getInstance()
            .getValue("game_server_" + serverID + "_rmi_port"));
            String rmi_object = RMIConfig.getInstance()
            .getValue("game_server_" + serverID + "_rmi_object");

            System.out.println("Vinh Login RMI - OK " + rmi_port + ":::" + rmi_object);
            Registry registry = LocateRegistry.getRegistry(rmi_port);
            rmi = (IYOYOLoginRMI)registry.lookup(rmi_object);

            System.out.println("Vinh Login RMI - OK");

            if (null != rmi)
            {
                status = true;
                playerCount = rmi.getOnlinePlayerNumber();
            }
            else
            {
                status = false;
                playerCount = 0;
            }
        }
        catch (Exception e)
        {
        	e.printStackTrace();
            rmi = null;
            status = false;
            playerCount = 0;
        }
    }

    public int compare(GameServer server1, GameServer server2) 
    {
        return 0;
    }
}
