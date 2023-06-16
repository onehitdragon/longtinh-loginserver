package yoyo.server.login.tools;


import java.util.Comparator;

import yoyo.server.login.GameServer;

/**
 * Created by IntelliJ IDEA.
 * User: jiaodj
 * Date: 11-3-22
 * Time: 上午10:26
 *
 * 升序
 * 如果人数相等，则按序号排序
 * 如果未运行，则在已运行的后面
 * @param o
 * @return
 */
public class GameServerCompare implements Comparator<GameServer>{
    public int compare(GameServer o1, GameServer o2) {
        if(1 == o1.onTop && 0 == o2.onTop){
        	return -1;
        }
        if(0 == o1.onTop && 1 == o2.onTop){
        	return 1;
        }
        if(o1.isRunning() && o2.isRunning()){
            if(o1.getPlayerCount() > o2.getPlayerCount()){
                return 1;
            }else if(o1.getPlayerCount() == o2.getPlayerCount()){
                if(o1.getIndex() > o2.getIndex()){
                    return 1;
                }
                return 0;
            }
            return -1;
        }else {
            if(o1.isRunning() && !o2.isRunning()){
                return -1;
            }
            if(!o1.isRunning() && !o2.isRunning()){
                if(o1.getIndex() > o2.getIndex()){
                    return 1;
                }else if(o1.getIndex() == o2.getIndex()){
                    return 0;
                }else if(o1.getIndex() < o2.getIndex()){
                    return -1;
                }
            }
            if(!o1.isRunning() && o2.isRunning()){
                return 1;
            }
            return 0;
        }
    }
}
