package www.lee311.percentprogressbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private PercentProgressBar mPercentProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPercentProgressBar = (PercentProgressBar) findViewById(R.id.percent_progress_bar);
        mPercentProgressBar.setTextColor(getResources().getColor(R.color.colorPrimary));
        mPercentProgressBar.setTextSize(64);
        mPercentProgressBar.setTotal(329990);
        mPercentProgressBar.setProgress(30);
        mPercentProgressBar.setChangedListener(new PercentProgressBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(int currentValue, int percent) {

            }
        });
    }
}
