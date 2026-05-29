package com.swiftdoc.pdf22449.activities;
import android.os.Bundle; import android.view.MenuItem; import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity; import androidx.appcompat.widget.Toolbar;
import com.swiftdoc.pdf22449.R; import com.swiftdoc.pdf22449.database.DBHelper;

public class StatsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s); setContentView(R.layout.activity_stats);
        Toolbar tb = findViewById(R.id.toolbar_stats); setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reading Stats");
        }
        DBHelper db = DBHelper.getInstance(this);
        // Total reading time
        long ms = db.getTotalReadingTime();
        long sec = ms / 1000; long hrs = sec / 3600; long mins = (sec % 3600) / 60;
        String t = hrs > 0 ? hrs + "h " + mins + "m" : mins + "m " + (sec % 60) + "s";
        ((TextView) findViewById(R.id.tv_total_time)).setText(t);
        // Total pages placeholder (tracked via reading_progress)
        ((TextView) findViewById(R.id.tv_total_pages)).setText("—");
        ((TextView) findViewById(R.id.tv_tagline)).setText("Keep reading to grow your stats!");
    }
    @Override public boolean onOptionsItemSelected(MenuItem i) {
        if (i.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(i);
    }
}
