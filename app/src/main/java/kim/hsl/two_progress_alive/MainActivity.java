package kim.hsl.two_progress_alive;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 通过前台 Service 提升应用权限
        // 启动普通 Service , 但是在该 Service 的 onCreate 方法中执行了 startForeground
        // 变成了前台 Service 服务
        startService(new Intent(this, LocalForegroundService.class));
        startService(new Intent(this, RemoteForegroundService.class));
    }
}