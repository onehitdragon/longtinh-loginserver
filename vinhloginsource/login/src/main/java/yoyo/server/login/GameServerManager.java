package yoyo.server.login;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import yoyo.server.login.config.RMIConfig;
import yoyo.server.login.tools.GameServerCompare;
import yoyo.server.login.tools.Log;
import yoyo.server.login.tools.YOYOOutputStream;


public class GameServerManager
{
    private static Logger log = Logger.getLogger(GameServerManager.class);

    private ArrayList<GameServer> lstServer;
    private int runningServerCount;
    private long checkingInterval;
    private long serverSortInterval;
    private long intervalOfServerSortDelay;

    private int	serverFullCount;
    private int	serverCrowdCount;

    private Timer timer;
    private RefreshTask refreshTask;
    private SortTask serverSortTask;


    private long lastTimeOfSorting;
    private long intervalOfSequenceRefreshing;
    private int publisherNumber;
    
    /**
     * 服务器人数状态--1：正常    2:拥挤  3:爆满
     */
    public static final byte FULL_STATUS = 3;
    public static final byte CROWD_STATUS = 2;
    public static final byte NORMAL_STATUS = 1;
    

    private String getPhoneURL;

    private String GET_ACCOUNT_ID_URL;

    private String REGISTE_TO_USER_CENTER_URL;

    private String UPDATE_PASSWORD_TO_USER_CENTER_URL;

    private String download_ng_game_url;

    private String download_agent;

    private int download_size;

    private String mobile_login_url_1;
    private String moblie_login_url_2;

    private String sms_url;

    private int roll_lady_time;

    private String show_down_server;

    private static GameServerManager instance;

    private GameServerManager()
    {
    }

    public int getRunningServerCount ()
    {
        return runningServerCount;
    }

    public synchronized static GameServerManager getInstance ()
    {
        if (instance == null)
        {
            instance = new GameServerManager();
        }

        return instance;
    }

    public void init ()
    {
    	System.out.println("vinh gameservermanager init start");
    	System.out.println(lstServer);
        if (null == lstServer)
        {
        	try {
				
			
            lstServer = new ArrayList<GameServer>();
            timer = new Timer();
            refreshTask = new RefreshTask();
            serverSortTask = new SortTask();
            checkingInterval = Long.parseLong(RMIConfig.getInstance().getValue("interval_of_game_server_status_checking"));
            intervalOfSequenceRefreshing = Long.parseLong(RMIConfig.getInstance().getValue("interval_of_refreshing_sequence"));

            intervalOfServerSortDelay = Long.parseLong(RMIConfig.getInstance().getValue("interval_of_server_sort_delay"));
            serverSortInterval = Long.parseLong(RMIConfig.getInstance().getValue("interval_of_server_sort"));

            serverFullCount = Integer.parseInt(RMIConfig.getInstance().getValue("game_status_full_player_number"));
            serverCrowdCount = Integer.parseInt(RMIConfig.getInstance().getValue("game_status_crowd_player_number"));
            
            publisherNumber = Integer.parseInt(RMIConfig.getInstance().getValue("game_publisher_num"));
         
            getPhoneURL = RMIConfig.getInstance().getValue("get_phone_url");
            
            GET_ACCOUNT_ID_URL=RMIConfig.getInstance().getValue("GET_ACCOUNT_ID_URL");
            REGISTE_TO_USER_CENTER_URL=RMIConfig.getInstance().getValue("REGISTE_TO_USER_CENTER_URL");
            UPDATE_PASSWORD_TO_USER_CENTER_URL= RMIConfig.getInstance().getValue("UPDATE_PASSWORD_TO_USER_CENTER_URL");

            download_ng_game_url = RMIConfig.getInstance().getValue("download_ng_game_url");
            download_agent = RMIConfig.getInstance().getValue("download_agent");
            download_size = Integer.parseInt(RMIConfig.getInstance().getValue("download_size"));

            mobile_login_url_1 = RMIConfig.getInstance().getValue("mobile_login_url_1");
            moblie_login_url_2 = RMIConfig.getInstance().getValue("moblie_login_url_2");

            sms_url = RMIConfig.getInstance().getValue("sms_url");
            roll_lady_time = Integer.parseInt(RMIConfig.getInstance().getValue("roll_lady_time"));
            show_down_server = RMIConfig.getInstance().getValue("show_down_server");
            
            gameServerList();
            lastTimeOfSorting = System.currentTimeMillis();
            timer.schedule(refreshTask, checkingInterval,checkingInterval);
            timer.schedule(serverSortTask,intervalOfServerSortDelay,serverSortInterval);
        	} catch (Exception e) {
				e.printStackTrace();
			}
        }
        System.out.println("gameservermanager init end");
    }


    public boolean showDownServer()
    {
        if(show_down_server == null)
        {
            return false;
        }
        return show_down_server.equals("yes");
    }

    public int getRoll_lady_time()
    {
        return roll_lady_time;
    }

    public String getSMSURL()
    {
        return sms_url;
    }

    public String[] getMobileLoginUrl()
    {
        return new String[]{mobile_login_url_1, moblie_login_url_2};
    }

    public String getGET_ACCOUNT_ID_URL()
    {
    	return GET_ACCOUNT_ID_URL;
    }

    public String getREGISTE_TO_USER_CENTER_URL()
    {
    	return REGISTE_TO_USER_CENTER_URL;
    }

    public String getUPDATE_PASSWORD_TO_USER_CENTER_URL()
    {
    	return UPDATE_PASSWORD_TO_USER_CENTER_URL;
    }
    
    public String getPhoneURL()
    {
    	return getPhoneURL;
    }

    public String getDownloadNgGameUrl()
    {
        return  download_ng_game_url;
    }

    public String getDownloadAgent()
    {
        return download_agent;
    }

    public int getDownloadSize()
    {
        return download_size;
    }


    private void gameServerList ()
    {
        try
        {
            int serverCount = Integer.parseInt(RMIConfig.getInstance().getValue("game_server_number"));

            short serverID;
            int sequenceInList, onlinePlayerLimit;
            String gameServerName, httpURLAddr, httpURLContext, tcpURL;
            GameServer gameService;

            VersionInfo.curVersion = RMIConfig.getInstance().getValue("game_current_version");
            VersionInfo.compVersion = RMIConfig.getInstance().getValue("game_compatible_versions");

            VersionInfo.downloadUrlMap.put(VersionInfo.XJ_DOWNLOAD_ID,RMIConfig.getInstance().getValue("game_publisher_download_url_"+1));
            VersionInfo.downloadUrlMap.put(VersionInfo.DANGLE_DOWNLOAD_ID,RMIConfig.getInstance().getValue("game_publisher_download_url_"+1001));
            VersionInfo.downloadUrlMap.put(VersionInfo.JIUYOU_DOWNLOAD_ID,RMIConfig.getInstance().getValue("game_publisher_download_url_"+1002));


            for (int i = 1; i <= serverCount; i++)
            {
            	
                serverID = Short.parseShort(RMIConfig.getInstance().getValue("game_server" + i + "_ID"));
                sequenceInList = Integer.parseInt(RMIConfig.getInstance().getValue("game_server_" + i + "_view_sequence"));
                gameServerName = new String(RMIConfig.getInstance().getValue("game_server_" + serverID + "_name").getBytes("ISO8859-1"), "UTF-8");
                onlinePlayerLimit = Integer.parseInt(RMIConfig.getInstance().getValue("game_server_" + i+ "_max_online_number_limit"));
                httpURLAddr = RMIConfig.getInstance().getValue(
                        "game_server_" + serverID + "_http_url_address");

                httpURLContext = RMIConfig.getInstance().getValue(
                        "game_server_" + serverID + "_http_url_context");

                if (httpURLContext.equals("/"))
                {
                    httpURLContext = "";
                }

                tcpURL = RMIConfig.getInstance().getValue(
                        "game_server_" + serverID + "_tcp_url");
                
                gameService = new GameServer(serverID, gameServerName,
                        sequenceInList, onlinePlayerLimit, httpURLAddr,
                        httpURLContext, tcpURL);
                gameService.onTop = Short.parseShort(RMIConfig.getInstance().getValue("game_server" + i + "_onTop"));
                gameService.setServerPlayerNumberStatus(NORMAL_STATUS);
                lstServer.add(gameService);

                if (gameService.isRunning())
                {
                    runningServerCount++;
                }
            }

            Collections.sort(lstServer,new GameServerCompare());


            System.out.println("\n=== 服务器列表加载完毕 ===\n");
        }
        catch (Exception e)
        {
        	e.printStackTrace();
            Log.error(null, e);
        }
    }

    public GameServer getGameServer (short serverID)
    {
        for (GameServer server : lstServer)
        {
            if (server.getServerID() == serverID) 
            { 
            	return server; 
            }
        }

        return null;
    }

    public void resetPlayerStatus (int accountID)
    {
        for (GameServer server : lstServer)
        {
            if (server.isRunning())
            {
                try
                {
                	server.resetPlayerStatus(accountID);
                }
                catch (Exception e)
                {
                	e.printStackTrace();
                }
            }
        }
    }

    public void serverListInfo (YOYOOutputStream yos)throws IOException
    {
        ArrayList<GameServer> runningServiceList = runningServiceList();
        if(showDownServer()){
            runningServiceList = lstServer;
        }
        System.out.println("running service list size = " + runningServiceList.size());
        yos.writeByte(runningServiceList.size());

        int i=1;
        for (GameServer service : runningServiceList)
        {
            if (null != service)
            {
                String servername = service.getServerName();
                if(!service.isRunning())
                {
                    servername += "(维护中...)";
                }
                yos.writeByte(i);
                yos.writeShort(service.getServerID());
                yos.writeUTF(servername);
                yos.writeByte(service.isRunning() ? 1 : 0);
                yos.writeByte(service.getServerPlayerNumberStatus());
                i++;
                log.info("server["+service.getServerName()+"],"+service.getPlayerCount()+",status="+service.getServerPlayerNumberStatus());
            }
        }
    }

    private void scan ()
    {
        try
        {
            for (GameServer server : lstServer)
            {
                log.info("扫描列表中的游戏服务器运行状态 isrunning="+server.isRunning()+",playerNumber="+server.getPlayerCount());
                if (server.isRunning())
                {
                	server.serverStatus();
                    if(server.getPlayerCount() == serverFullCount)
                    {
                    	server.setServerPlayerNumberStatus(FULL_STATUS);
                    }
                    else if(server.getPlayerCount() < serverFullCount
                    		&& server.getPlayerCount() >= serverCrowdCount)
                    {
                    	server.setServerPlayerNumberStatus(CROWD_STATUS);
                    }
                    else
                    {
                    	server.setServerPlayerNumberStatus(NORMAL_STATUS);
                    }

                }
                else
                {
                	server.findGameServer();
                }
            }
        }
        catch (Exception e)
        {
        	e.printStackTrace();
            Log.error(null, e);
        }
    }
    
    private ArrayList<GameServer> runningServiceList()
    {
        ArrayList<GameServer> runningServiceList = new ArrayList<GameServer>();
        for(GameServer server : lstServer)
        {
            if(server.isRunning())
            {
                runningServiceList.add(server);
            }
        }
        return runningServiceList;
    }

    class SortTask extends TimerTask
    {
        public void run() 
        {
            Collections.sort(lstServer,new GameServerCompare());
        }
    }

    class RefreshTask extends TimerTask
    {
        public void run ()
        {
            scan();
        }
    }
}
