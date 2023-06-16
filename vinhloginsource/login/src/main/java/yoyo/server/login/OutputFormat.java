package yoyo.server.login;

import yoyo.server.login.tools.YOYOOutputStream;

public class OutputFormat
{
	private static OutputFormat instance;

    private OutputFormat()
    {
    }
    
    public static OutputFormat getInstance()
    {
    	if(instance == null)
    	{
    		instance = new OutputFormat();
    	}
    	return instance;
    }
    
    public byte[] format(byte gameID, int sessionID,byte[] entity)
    {
    	YOYOOutputStream yos = null;
    	byte[] ret = null;
        try
        {
            if (null != entity)
            {
                yos = new YOYOOutputStream();

                yos.writeShort(0);
                yos.writeByte(gameID);
                yos.writeInt(sessionID);
                yos.writeByte(1);
                yos.writeShort(entity.length);
                yos.writeBytes(entity);

                yos.flush();
                ret = yos.getBytes();
                
                int len = ret.length;
                ret[0] = (byte) ((len - 2) >>> 8);
                ret[1] = (byte) ((len - 2) & 0xff);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally  
        {
        	if(yos != null)
        	{
        		try 
        		{
                    yos.close();
				} 
        		catch (Exception e2)
        		{
				}
        	}
        }
        return ret;
    }

    public byte[] format(byte gameID, int sessionID,byte[] entity1, byte[] entity2)
    {
    	YOYOOutputStream yos = null;
    	byte[] ret = null;
        try
        {
            byte count = 0;
            yos = new YOYOOutputStream();

            yos.writeShort(0);
            yos.writeByte(gameID);
            yos.writeInt(sessionID);
            yos.writeByte(0);

            if (null != entity1)
            {
                yos.writeShort(entity1.length);
                yos.writeBytes(entity1);

                count++;
            }

            if (null != entity2)
            {
                yos.writeShort(entity2.length);
                yos.writeBytes(entity2);

                count++;
            }

            if (count > 0)
            {
                yos.flush();
                ret = yos.getBytes();
                int len = ret.length;
                ret[0] = (byte) ((len - 2) >>> 8);
                ret[1] = (byte) ((len - 2) & 0xff);
                ret[7] = count;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
        	if(yos != null)
        	{
        		try 
        		{
                    yos.close();
				} 
        		catch (Exception e2)
        		{
				}
        	}
        }
        return ret;
    }
    
    /**
     * 上行报文时的标识
     * 用于测试上下行报文时间
     */
    private short key;
    /**
     * 接收上行报文时的时间
     * 用于测试上下行报文时间
     */
    private long receivedTime;
  
    public short getKey() {
		return key;
	}

	public void setKey(short key) {
		this.key = key;
	}

	public long getReceivedTime() {
		return receivedTime;
	}

	public void setReceivedTime(long receivedTime) {
		this.receivedTime = receivedTime;
	}
}
