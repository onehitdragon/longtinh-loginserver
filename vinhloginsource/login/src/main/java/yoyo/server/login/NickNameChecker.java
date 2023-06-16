package yoyo.server.login;

import yoyo.server.login.database.LoginDAO;

public class NickNameChecker
{
    public static final int NAMEVALID          = 1;
    public static final int NAMEEXISTS            = 0;
    public static final int NAMEINVAILD             = -1;
    public static final int NAMEHASSPACE = -2;

    /**
     * 职业描述
     */
    private static final String[] DESCLIST = {
            "武者", "道者", "力士", "侍卫", "鼓手", "弩手", "圣手", "术者", "魂者", "剑舞者", "百夫长",
            "神眼", "天擂", "金刚", "神医", "贤者", "御灵师", "天舞者" };
    
    private NickNameChecker()
    {
    }
    
    private static boolean inDesc (String name)
    {
        for (String des : DESCLIST)
        {
            if (des.equals(name))
            {
                return true;
            }
        }

        return false;
    }

    public static int check (String nickname, short serverID)
    {
        if (nickname.indexOf(" ") != -1)
        {
            return NAMEHASSPACE;
        }
        if (nickname.indexOf("　") != -1)
        {
            return NAMEHASSPACE;
        }
        if ((nickname.indexOf("g") != -1 || (nickname.indexOf("G") != -1) || 
        		(nickname.indexOf("Ｇ") != -1))
                && (nickname.indexOf("m") != -1
                        || (nickname.indexOf("M") != -1) || (nickname.indexOf("Ｍ") != -1)))
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("客") != -1 && nickname.indexOf("服") != -1)
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("管") != -1 && nickname.indexOf("理") != -1)
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("系") != -1 && nickname.indexOf("统") != -1)
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("公") != -1 && nickname.indexOf("告") != -1)
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("游") != -1 && nickname.indexOf("戏") != -1)
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("'") != -1)
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("%") != -1)
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("*") != -1)
        {
            return NAMEINVAILD;
        }
        if (nickname.indexOf("&") != -1)
        {
            return NAMEINVAILD;
        }
        if (inDesc(nickname))
        {
            return NAMEINVAILD;
        }
        if (!InvaildWordChecker.isInvaildWord(nickname))
        {
            return NAMEINVAILD;
        }
        if (!LoginDAO.nicknameCanBeUse(nickname, serverID))
        {
            return NAMEEXISTS;
        }

        return NAMEVALID;
    }
}
