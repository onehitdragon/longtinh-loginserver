package yoyo.server.login.tools;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimerTask;
import java.util.Timer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.SimpleLayout;

public class Log extends TimerTask
{
    private static Log     instance;

    private static Timer         timer;

    private static Logger        logger;

    private static FileAppender  appender                     = null;

    private static PatternLayout layout                       = null;

    private static String        pattern                   = "";

    private static String        CURRENT_LOG_FILE_NAME        = "log.txt";

    private static String        CURRENT_LOG_PATH             = "."
                                                                      + File.separator
                                                                      + "xj_login"
                                                                      + File.separator
                                                                      + "logs"
                                                                      + File.separator
                                                                      + "current"
                                                                      + File.separator;

    private static final String  BACKUP_LOG_PATH              = "."
                                                                      + File.separator
                                                                      + "xj_login"
                                                                      + File.separator
                                                                      + "logs"
                                                                      + File.separator
                                                                      + "backup"
                                                                      + File.separator;

    private static final String  BACKUP_LOG_SUFFIX = ".log";

    private static boolean       hasStart                     = false;


    private Log()
    {
    }

    public static void init ()
    {
        pattern = "%d [%t] %-5p %c - %m%n";
        init(CURRENT_LOG_PATH, CURRENT_LOG_FILE_NAME);
    }

    public static void init (String path, String fileName)
    {
        init(path, fileName, "%d [%t] %-5p %c - %m%n");
    }

    private static Log getInstance ()
    {
        if (null == instance)
        {
            instance = new Log();
        }

        return instance;
    }

    public static void init (String path, String fileName, String log4jPattern)
    {
        if (hasStart) return;
        hasStart = true;
        if (log4jPattern == null) 
        {
        	pattern = "%m%n";
        }
        else 
        {
        	pattern = log4jPattern;
        }
        CURRENT_LOG_PATH = path;
        CURRENT_LOG_FILE_NAME = fileName;
        logger = Logger.getLogger("YOYO");
        if (logger == null) 
        {
			System.out.println("get yoyo log null");
		}
        layout = new PatternLayout(pattern);
        File currentLogFolder = new File(CURRENT_LOG_PATH);
        if (!currentLogFolder.exists())
        {
            currentLogFolder.mkdir();
        }

        try
        {
            appender = new FileAppender(layout, CURRENT_LOG_PATH
                    + CURRENT_LOG_FILE_NAME, true);
            logger.addAppender(appender);
        }
        catch (Exception exception)
        {
        }

        logger.setLevel(Level.ALL);
        timer = new Timer();
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.set(11, 24);
        timer.scheduleAtFixedRate(getInstance(), calendar.getTime(), 86400000L);
    }

    public void run ()
    {
        try
        {
            String backupFileName = formatFileName();
            backupFileName = backupFileName + BACKUP_LOG_SUFFIX;
            File backupLogFolder = new File(BACKUP_LOG_PATH);

            if (!backupLogFolder.exists())
            {
                backupLogFolder.mkdir();
            }

            File backupLog = new File(BACKUP_LOG_PATH + backupFileName);

            synchronized (logger)
            {
                logger.removeAppender(appender);
                appender.close();
                File logfile = new File(CURRENT_LOG_PATH + CURRENT_LOG_FILE_NAME);
                logfile.renameTo(backupLog);

                try
                {
                    appender = new FileAppender(layout, CURRENT_LOG_PATH + CURRENT_LOG_FILE_NAME, false);
                }
                catch (Exception exception)
                {
                }
                logger.addAppender(appender);
                logger.addAppender(new ConsoleAppender(new SimpleLayout()));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private String formatFileName ()
    {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(new Date());
        DateFormat df = DateFormat.getDateTimeInstance();
        String backupFileName = df.format(gc.getTime());

        int index = backupFileName.indexOf(" ");
        String fileName = backupFileName.substring(0, index);
        String temp = "";
        temp = backupFileName.substring(index + 1, backupFileName.indexOf(":"));
        backupFileName = backupFileName.substring(backupFileName.indexOf(":") + 1);
        fileName = fileName + "-" + temp;

        temp = backupFileName.substring(0, backupFileName.indexOf(":"));
        fileName = fileName + "-" + temp;

        backupFileName = backupFileName
                .substring(backupFileName.indexOf(":") + 1);
        temp = backupFileName;
        fileName = fileName + "-" + temp;

        return fileName;
    }

    public static void println (String str)
    {
        try
        {
        	str.replace('\'', '_');
        	str.replace('"', '_');
            info(str);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void println (byte[] bytes)
    {
        info(new String(bytes));
    }

    public static void println (char[] chs)
    {
        info(new String(chs));
    }

    public static void println (Exception ex)
    {
        info(ex.toString() + ex.getMessage());
    }

    public static void error (Object obj, Throwable e)
    {
        logger.error(obj, e);
    }

    public static void info (String str)
    {
        logger.info(str);
    }

    public static String formatLog(String desc, boolean success, String... strings)
    {
        StringBuffer sf = new StringBuffer();
        sf.append(desc).append(STARTSIGN).append(format.format(new Date())).append(ENDSIGN);
        for(String s : strings)
        {
            sf.append(formatString(s));
        }
        sf.append(success?"成功":"失败");
        return sf.toString();
    }

    private static String formatString(String str)
    {
        return STARTSIGN + str + ENDSIGN;
    }

    private final static String STARTSIGN = "[";
    private final static String ENDSIGN = "]";
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
}
