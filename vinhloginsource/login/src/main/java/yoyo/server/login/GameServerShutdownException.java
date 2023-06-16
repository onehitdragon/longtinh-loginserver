package yoyo.server.login;


public class GameServerShutdownException extends Exception
{
	private static final long serialVersionUID = 2360470866807646934L;

	public GameServerShutdownException()
    {
        super("游戏服务器已关闭");
    }
}
