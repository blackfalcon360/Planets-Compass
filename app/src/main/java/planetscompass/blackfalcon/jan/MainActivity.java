package planetscompass.blackfalcon.jan;

import android.Manifest;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.GeomagneticField;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Planets in the shape of a compass, with a time slider. Works offline with GPS. */
public class MainActivity extends Activity
        implements HeadingProvider.Listener, LocationHelper.Callback {

    private static final int REQ_LOCATION = 1;
    private static final int SLIDER_MAX = 288;     // 48 hours in 10-minute steps
    private static final int SLIDER_CENTER = 144;  // = the moment you started moving the slider
    private static final String[] DIRS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private PlanetsView view;
    private TextView timeLabel;
    private SeekBar seek;
    private final TextView[] rows = new TextView[Astronomy.NAMES.length];

    private HeadingProvider provider;
    private LocationHelper location;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshSky();
            handler.postDelayed(this, 1000);
        }
    };

    private boolean hasLoc;
    private double lat, lon;
    private float declination;
    private float trueHeading;
    private String coordText = "";
    private Astronomy.Body[] bodies;

    // time travel
    private boolean live = true;
    private long baseMillis;
    private int minuteOffset;
    private int dayOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        int widthPx = getResources().getDisplayMetrics().widthPixels;
        view = new PlanetsView(this);
        view.setOnClickListener(v -> {
            if (!hasLoc) requestLocation();
        });
        root.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, widthPx));

        timeLabel = new TextView(this);
        timeLabel.setTextColor(Color.WHITE);
        timeLabel.setTextSize(14);
        timeLabel.setTypeface(Typeface.DEFAULT_BOLD);
        timeLabel.setGravity(Gravity.CENTER);
        timeLabel.setPadding(0, dp(4), 0, 0);
        root.addView(timeLabel);

        seek = new SeekBar(this);
        seek.setMax(SLIDER_MAX);
        seek.setProgress(SLIDER_CENTER);
        seek.setPadding(dp(20), dp(8), dp(20), dp(8));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (!fromUser) return;
                enterScrub();
                minuteOffset = (progress - SLIDER_CENTER) * 10;
                refreshSky();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        root.addView(seek, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.addView(button("\u2212 1 day", v -> {
            enterScrub();
            dayOffset--;
            refreshSky();
        }), weighted());
        buttons.addView(button("Now", v -> {
            live = true;
            minuteOffset = 0;
            dayOffset = 0;
            seek.setProgress(SLIDER_CENTER);
            refreshSky();
        }), weighted());
        buttons.addView(button("+ 1 day", v -> {
            enterScrub();
            dayOffset++;
            refreshSky();
        }), weighted());
        root.addView(buttons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(4), dp(12), dp(12));
        for (int i = 0; i < rows.length; i++) {
            TextView tv = new TextView(this);
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextSize(12);
            tv.setTextColor(Color.WHITE);
            tv.setPadding(0, dp(2), 0, dp(2));
            rows[i] = tv;
            list.addView(tv);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        provider = new HeadingProvider(this, this, 0.15);
        location = new LocationHelper(this, this);
        if (location.hasSaved) onLocation(location.lat, location.lon); // works offline from the last position

        if (HeadingProvider.isSupported(this)) {
            requestLocation();
        }
        refreshSky();
    }

    private void requestLocation() {
        if (location.hasPermission()) {
            location.start();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            location.start();
            refreshSky();
        }
    }

    @Override
    public void onLocation(double la, double lo) {
        hasLoc = true;
        lat = la;
        lon = lo;
        declination = new GeomagneticField((float) la, (float) lo, 0f,
                System.currentTimeMillis()).getDeclination();
        coordText = LocationHelper.dms(la, true) + "\n" + LocationHelper.dms(lo, false);
        refreshSky();
    }

    @Override
    public void onHeading(float magneticDegrees) {
        trueHeading = (magneticDegrees + declination + 360f) % 360f;
        pushToView();
    }

    private void enterScrub() {
        if (live) {
            live = false;
            baseMillis = System.currentTimeMillis();
        }
    }

    private long shownMillis() {
        return live
                ? System.currentTimeMillis()
                : baseMillis + minuteOffset * 60000L + dayOffset * 86400000L;
    }

    /** Recomputes the planets for the time on the slider and updates the list and the map. */
    private void refreshSky() {
        long t = shownMillis();
        timeLabel.setText(new SimpleDateFormat("EEE d MMM yyyy  HH:mm", Locale.getDefault()).format(new Date(t))
                + (live ? "   \u2022  LIVE" : "   \u2022  " + offsetText()));

        if (!hasLoc) {
            bodies = null;
            for (TextView r : rows) r.setText("");
            rows[0].setText("Waiting for your GPS position\u2026");
            rows[0].setTextColor(0xFF9AA0A6);
            pushToView();
            return;
        }

        bodies = Astronomy.compute(t, lat, lon);
        for (int i = 0; i < bodies.length; i++) {
            Astronomy.Body b = bodies[i];
            rows[i].setText(rowText(b));
            rows[i].setTextColor(b.altitude >= 0 ? Color.WHITE : 0xFF7C828A);
        }
        pushToView();
    }

    private void pushToView() {
        String message = "";
        if (!hasLoc) {
            if (!location.hasPermission()) {
                message = "Allow location (GPS) to see the planets\n(tap here to try again)";
            } else if (!location.isLocationOn()) {
                message = "Turn on Location (GPS) in your phone settings";
            } else {
                message = "Searching for GPS\u2026\nGo outside or near a window.\nNo internet needed.";
            }
        }
        String locText = hasLoc ? coordText + "\n" + location.statusLine() : location.statusLine();
        view.setState(bodies, hasLoc, trueHeading, locText, message);
    }

    private String offsetText() {
        int total = minuteOffset + dayOffset * 1440;
        if (total == 0) return "not live";
        int abs = Math.abs(total);
        StringBuilder sb = new StringBuilder(total > 0 ? "+" : "\u2212");
        if (abs >= 1440) sb.append(abs / 1440).append("d ");
        if ((abs % 1440) / 60 > 0) sb.append((abs % 1440) / 60).append("h ");
        if (abs % 60 > 0) sb.append(abs % 60).append("m");
        return sb.toString().trim();
    }

    private String rowText(Astronomy.Body b) {
        int az = (int) (Math.round(b.azimuth) % 360);
        int alt = (int) Math.round(b.altitude);
        String dir = DIRS[((int) ((b.azimuth + 22.5) / 45.0)) % 8];
        String dist = "Moon".equals(b.name)
                ? String.format(Locale.US, "%,d km", Math.round(b.distance * 6378.14))
                : String.format(Locale.US, "%.2f AU", b.distance);
        return String.format(Locale.US, "%-8s Az %3d\u00B0 %-3s Alt %+3d\u00B0 %s", b.name, az, dir, alt, dist);
    }

    @Override
    protected void onResume() {
        super.onResume();
        provider.start(SensorManager.SENSOR_DELAY_GAME);
        if (location.hasPermission()) location.start();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        provider.stop();
        location.stop();
        handler.removeCallbacks(ticker);
    }

    private Button button(String text, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
