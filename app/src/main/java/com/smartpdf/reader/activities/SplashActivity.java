package com.smartpdf.reader.activities;
import android.content.Intent; import android.os.Bundle; import android.os.Handler; import android.os.Looper;
import android.view.animation.AlphaAnimation; import android.widget.ImageView; import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity; import com.smartpdf.reader.R;
public class SplashActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s){
        super.onCreate(s); setContentView(R.layout.activity_splash);
        AlphaAnimation fa=new AlphaAnimation(0f,1f); fa.setDuration(700); fa.setFillAfter(true);
        AlphaAnimation fa2=new AlphaAnimation(0f,1f); fa2.setDuration(700); fa2.setStartOffset(300); fa2.setFillAfter(true);
        ((ImageView)findViewById(R.id.iv_splash_logo)).startAnimation(fa);
        ((TextView)findViewById(R.id.tv_splash_name)).startAnimation(fa2);
        ((TextView)findViewById(R.id.tv_splash_tagline)).startAnimation(fa2);
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            startActivity(new Intent(this,MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            finish();},1800);}
}
