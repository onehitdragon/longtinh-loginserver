package yoyo.server.login;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import yoyo.server.login.tools.Log;


public class InvaildWordChecker
{
    private static ArrayList<String> INVAILDWORDMAP;

    private InvaildWordChecker()
    {
    }

    public static boolean isInvaildWord (String coxt)
    {
        for (int i = 0; i < INVAILDWORDMAP.size(); i++)
        {
            if (coxt.indexOf(INVAILDWORDMAP.get(i)) != -1)
            {
                return false;
            }
        }

        return true;
    }

    public static void init ()
    {
        BufferedReader br = null;
        try
        {
            if (null == INVAILDWORDMAP)
            {
                INVAILDWORDMAP = new ArrayList<String>();
                File file = new File(INVAILDWORD_PATH);

                if (file.exists())
                {
                    br = new BufferedReader(new InputStreamReader(
        					new FileInputStream(file.getAbsolutePath()), "GB2312"));
                    String record = br.readLine();

                    while (record != null)
                    {
                        if (!record.trim().equals(""))
                        {
                            INVAILDWORDMAP.add(record);
                        }

                        record = br.readLine();
                    }

                    Log.println("非法字符列表加载完毕");
                }
                else
                {
                    Log.println("非法字符文件不存在");
                }
            }
        }
        catch (Exception e)
        {
            Log.error(null, e);
        }
        finally
        {
            try
            {
                if (null != br)
                {
                    br.close();
                    br = null;
                }
            }
            catch (Exception e)
            {
            }
        }
    }

    private static final String INVAILDWORD_PATH = System.getProperty("user.dir")
                                                        + File.separator
                                                        + "xj_login"
                                                        + File.separator
                                                        + "config"
                                                        + File.separator
                                                        + "nickname_dirty_string.txt";
}
