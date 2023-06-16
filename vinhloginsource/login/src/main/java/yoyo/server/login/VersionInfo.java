package yoyo.server.login;

import java.util.HashMap;
import java.util.Map;

public class VersionInfo
{

    public static String curVersion;
    public static String compVersion;
 
    /**
     * 各渠道下载地址
     */
    public static Map<Integer,String> downloadUrlMap= new HashMap<Integer,String>();

    public static final int XJ_DOWNLOAD_ID = 1;
    public static final int DANGLE_DOWNLOAD_ID = 1001;
    public static final int JIUYOU_DOWNLOAD_ID = 1002;
    private VersionInfo()
    {
    }
}
