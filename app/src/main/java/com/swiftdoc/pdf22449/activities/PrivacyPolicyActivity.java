package com.swiftdoc.pdf22449.activities;
import android.content.Intent; import android.content.SharedPreferences;
import android.os.Bundle; import android.view.View; import android.widget.Button;
import android.widget.ScrollView; import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity; import com.swiftdoc.pdf22449.R;
public class PrivacyPolicyActivity extends AppCompatActivity {
    private Button btnAllow, btnDeny;
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s); setContentView(R.layout.activity_privacy_policy);
        btnAllow=findViewById(R.id.btn_allow); btnDeny=findViewById(R.id.btn_deny);
        // Buttons disabled until user scrolls to bottom
        btnAllow.setEnabled(false); btnDeny.setEnabled(false);
        btnAllow.setAlpha(0.4f); btnDeny.setAlpha(0.4f);
        ScrollView sv=findViewById(R.id.scroll_policy);
        sv.getViewTreeObserver().addOnScrollChangedListener(()->{
            View child=sv.getChildAt(0);
            if(child!=null){
                int diff=child.getBottom()-(sv.getHeight()+sv.getScrollY());
                if(diff<=10){
                    btnAllow.setEnabled(true); btnDeny.setEnabled(true);
                    btnAllow.setAlpha(1f); btnDeny.setAlpha(1f);
                }}});
        btnAllow.setOnClickListener(v->{
            getSharedPreferences("swift_prefs",MODE_PRIVATE).edit().putBoolean("privacy_consented",true).apply();
            startActivity(new Intent(this,MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            finish();});
        btnDeny.setOnClickListener(v->{
            Toast.makeText(this,"Permission Required to use SwiftDoc",Toast.LENGTH_LONG).show();
            finishAffinity(); android.os.Process.killProcess(android.os.Process.myPid());});}
}
