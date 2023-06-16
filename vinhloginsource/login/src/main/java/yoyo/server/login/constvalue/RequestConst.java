package yoyo.server.login.constvalue;

public class RequestConst
{
    public static final short REQUEST_ACCOUNTLOGIN                      = 610;
    public static final short REQUEST_DELETEROLE                        = 611;
    public static final short REQUEST_SELTSERVER                      = 615;
    public static final short REQUEST_QUICKLOGIN                    = 616;
    public static final short REQUEST_ACCOUNTREGISTE                    = 618;
    public static final short REQUEST_SERVERLIST                   = 617;
    public static final short REQUEST_CREATEROLE                        = 619;
    public static final short REQUEST_ENTERGAME                         = 620;
    public static final short REQUEST_MODIFYPWD                    = 614;
    public static final short REQUEST_BACKROLELIST                           = 630;
    public static final short REQUEST_QUICKLOGINBYPSEUDOMOBILE = 612;
    public static final short REQUEST_ACCOUNTLOGINBYMOBILE          = 613;
    public static final short REQUEST_DEFAULTROLELIST                  = 660;
    public static final short REQUEST_PHONEURL = 661;
    /**
     * 获取两个网游游戏下载地址的URL
     */
    public static final short REQUEST_DOWNLOAD_NG_GMAE_URL = 663;

    /**
     * 保存下载两个网游的记录
     */
    public static final short REQUEST_SAVE_DOWNLOAD_NG_GAME_FLAG = 664;
    /**
     * 绑定手机号
     */
    public static final short REQUEST_BIND_MSISDN = 665;
    /**
     * 密码找回
     */
    public static final short REQUEST_PWD_BACK = 666;

    /**
     * 快速注册
     */
    public static final short REQUEST_SHORT_REGIST = 667;

    /**
     * 请求Loaing提示列表
     */
    public static final short REQUEST_LOADING_TIPS = 668;

    /**
     * 心跳，维持会话
     */
    public static final short REQUEST_HEARTJUMP = 5120;
    
    /**
     * 新玩家从启动游戏到进入到游戏中的步骤记录
     * 上传客户端当前状态
	 * 1.启动
	 * 2.注册
	 * 3.登陆
	 * 4.服务器列表
	 * 5.角色列表
     * 6.创建角色
	 * 7.游戏中  2011-04-07 jiaodongjie 修改
     */
    public static final short NEW_PLAYER_LOGIN_STEP = 662;

    
}
