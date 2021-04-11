package kim.hsl.two_progress_alive;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

import org.w3c.dom.Text;

import java.util.List;

public class ServiceUtils {
    /**
     * 判定 Service 是否在运行
     * @param context
     * @return
     */
    public static boolean isServiceRunning(Context context, String serviceName){
        if(TextUtils.isEmpty(serviceName)) return false;

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        // 最多获取 200 个正在运行的 Service
        List<ActivityManager.RunningServiceInfo> infos =
                activityManager.getRunningServices(200);

        // 遍历当前运行的 Service 信息, 如果找到相同名称的服务 , 说明某进程正在运行
        for (ActivityManager.RunningServiceInfo info: infos){
            if (TextUtils.equals(info.service.getClassName(), serviceName)){
                return true;
            }
        }

        return false;
    }
}
