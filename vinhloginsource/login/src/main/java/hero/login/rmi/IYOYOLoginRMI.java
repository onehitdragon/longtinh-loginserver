package hero.login.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface IYOYOLoginRMI extends Remote
{
    /**
     * 获取角色信息
     * 
     * @param userIDs
     * @return
     * @throws RemoteException
     */
    public byte[] listRole (int[] userIDs) throws RemoteException;

    /**
     * 创建角色
     * 
     * @param accountID
     * @param serverID
     * @param userID
     * @param paras
     * @return
     * @throws RemoteException
     */
    public byte[] createRole (int accountID, short serverID, int userID,
            String[] paras) throws RemoteException;
    
    /**
     * 获取默认角色信息
     * @return
     * @throws RemoteException
     */
    public byte[] listDefaultRole () throws RemoteException;

    /**
     * 删除角色
     * 
     * @param userId
     * @return
     * @throws RemoteException
     */
    public int deleteRole (int userId) throws RemoteException;

    /**
     * 检查当前的角色是否正在使用之中，如果正在使用，强行将其踢下线
     * 
     * @param _userIDs
     * @return
     */
    public boolean resetPlayersStatus (int _accountID) throws RemoteException;

    /**
     * 检查逻辑服务器状态
     * 
     * @return
     * @throws RemoteException
     */
    public boolean checkStatusOfRun () throws RemoteException;

    /**
     * 获取在线人数
     * 
     * @return
     * @throws RemoteException
     */
    public int getOnlinePlayerNumber () throws RemoteException;

    /**
     * 获取客户端即将与游戏服务器交互时需要的SessionID
     * 
     * @param _userId
     * @return
     */
    public int createSessionID (int _userId, int _accountId)
            throws RemoteException;

    /**
     * 通过userID获取玩家信息，用于GM查询
     * @param _userID
     * @return
     * @throws RemoteException
     */
    public String getPlayerInfoByUserID(int _userID) throws RemoteException;

    /**
     * 通过昵称获取玩家信息，用于GM查询
     * @param nickname
     * @return
     * @throws RemoteException
     */
    public String getPlayerInfoByNickname(String nickname) throws RemoteException;

    /**
     * 根据 userID 冻结角色
     * @param _userID
     * @param keepTime
     * @param startTime
     * @param endTime
     * @param memo
     * @return
     * @throws RemoteException
     */
//    public boolean setPlayerUserIDBlack(int _userID,int keepTime,String startTime,String endTime,String memo) throws RemoteException;

    /**
     * 根据 accountID 冻结账号
     * @param _accountID
     * @param keepTime
     * @param startTime
     * @param endTime
     * @param memo
     * @return
     * @throws RemoteException
     */
//    public boolean setPlayerAccountIDBlack(int _accountID,int keepTime,String startTime,String endTime,String memo) throws RemoteException;

    /**
     * 根据 userID 禁言
     * @param _userID
     * @param keepTime
     * @param startTime
     * @param endTime
     * @param memo
     * @return
     * @throws RemoteException
     */
//    public boolean setPlayerChatBlack(int _userID,int keepTime,String startTime,String endTime,String memo) throws RemoteException;

    /**
     * 根据 userID 解冻角色
     * @param _userID
     * @return
     * @throws RemoteException
     */
//    public boolean deletePlayerUserIDBlack(int _userID) throws RemoteException;

    /**
     * 根据 accountID 解冻账号
     * @param _accountID
     * @return
     * @throws RemoteException
     */
//    public boolean deletePlayerAccountIDBlack(int _accountID) throws RemoteException;

    /**
     * 根据 userID 解禁禁言
     * @param _userID
     * @return
     * @throws RemoteException
     */
//    public boolean deletePlayerChatBlack(int _userID) throws RemoteException;

    /**
     * 根据 nickname 冻结角色
     * @param nickname
     * @param keepTime
     * @param startTime
     * @param endTime
     * @param memo
     * @return
     * @throws RemoteException
     */
//    public boolean setPlayerUserIDBlack(String nickname,int keepTime,String startTime,String endTime,String memo) throws RemoteException;

    /**
     * 根据 username 冻结账号
     * @param username
     * @param keepTime
     * @param startTime
     * @param endTime
     * @param memo
     * @return
     * @throws RemoteException
     */
//    public boolean setPlayerAccountIDBlack(String username,int keepTime,String startTime,String endTime,String memo) throws RemoteException;

    /**
     * 根据 nickname 禁言
     * @param nickname
     * @param keepTime
     * @param startTime
     * @param endTime
     * @param memo
     * @return
     * @throws RemoteException
     */
//    public boolean setPlayerChatBlack(String nickname,int keepTime,String startTime,String endTime,String memo) throws RemoteException;

    /**
     * 根据 nickname 解冻角色
     * @param nickname
     * @return
     * @throws RemoteException
     */
//    public boolean deletePlayerUserIDBlack(String nickname) throws RemoteException;

    /**
     * 根据 username 解冻账号
     * @param username
     * @return
     * @throws RemoteException
     */
//    public boolean deletePlayerAccountIDBlack(String username) throws RemoteException;

    /**
     * 根据 nickname 解禁禁言
     * @param nickname
     * @return
     * @throws RemoteException
     */
//    public boolean deletePlayerChatBlack(String nickname) throws RemoteException;
    
    /**
     * 获取游戏的地图列表
     * mapID-mapName,mapID-mapName,... 格式
     * @return
     * @throws RemoteException
     */
    public String getGameMapList() throws RemoteException;
    
    /**
     * GM 发公告
     * @param gmName
     * @param content
     * @throws RemoteException
     */
    public void sendNoticeGM(String gmName, String content) throws RemoteException;
    
    /**
     * GM 回复玩家邮件
     * 用 gmLetterID 到数据库里查询具体信息，然后给玩家发信
     * @param gmLetterID
     * @throws RemoteException
     */
    public void GMReplyLetter(int gmLetterID) throws RemoteException;
    
    /**
     * 地图瞬移
     * @param mapID
     * @param userID
     * @throws RemoteException
     */
    public int blink(short mapID,int userID) throws RemoteException;

    /**
     * 神州付充值回调接口
     * @param userID
     * @param transID 流水号
     * @param result   返回的结果    1:支付成功    2:支付失败
     * @return
     * @throws RemoteException
     */
    public int szfFeeCallBack(int userID,String transID,byte result,String orderID,int point) throws RemoteException;

    /**
     * GM给玩家添加物品
     * @param userID
     * @param goodsID
     * @param number
     * @return
     * @throws RemoteException
     */
    public int addGoodsForPlayer(int userID,int goodsID,int number) throws RemoteException;

    /**
     * GM给玩家加点
     * @param userID
     * @param point
     * @return
     * @throws RemoteException
     */
    public int addPointForPlayer(int userID,int point) throws RemoteException;

    /**
     * 修改玩家金钱，爱情值，等级
     * @param userID
     * @param money
     * @param loverValue
     * @param level
     * @return
     * @throws RemoteException
     */
    public int modifyPlayerInfo(int userID,int money,int loverValue,int level,int skillPoint) throws RemoteException;

    /**
     * 获取物品名称
     * @param goodsID
     * @return
     * @throws RemoteException
     */
    public String getGoodsName(int goodsID) throws RemoteException;

    public void smsCallBack(String transID,String result) throws RemoteException;
    
    /**
     * 强行踢下所有在线玩家
     * 
     * @return
     */
    public void resetPlayers () throws RemoteException;

}
