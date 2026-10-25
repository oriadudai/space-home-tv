package com.dudai.spacehome;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The "space" home screen for the TV.
 *
 * The whole screen is built on a fixed 1280x720 "stage" (the size of the design mockup)
 * and the stage is then scaled to fit the real screen, so positions match the design exactly.
 */
public class MainActivity extends Activity {

    // ---------- Settings you may want to change ----------

    /** Which HDMI port the "HDMI" tile opens: 0 = the first HDMI input the TV reports, 1 = the second, ... */
    private static final int HDMI_INDEX = 0;

    /** The four tiles, right to left. Packages are tried first; if none is installed, the app is found by name. */
    private final AppTarget[] apps = {
            new AppTarget("YouTube", 0xFFFF5DA2, false,
                    new String[]{"com.google.android.youtube.tv", "com.google.android.youtube"},
                    new String[]{"youtube"}),
            new AppTarget("סלקום TV", 0xFF4DE1FF, false,
                    new String[]{},
                    new String[]{"סלקום", "cellcom"}),
            new AppTarget("Netflix", 0xFFFFE14D, false,
                    new String[]{"com.netflix.ninja", "com.netflix.mediaclient"},
                    new String[]{"netflix"}),
            new AppTarget("HDMI", 0xFFA6FF4D, true,
                    new String[]{},
                    new String[]{}),
    };

    // ---------- Design constants ----------

    private static final int W = 1280, H = 720;          // design stage size
    private static final int BG = 0xFF1E0B45;            // night-sky purple
    private static final int INK = 0xFF1E0B45;           // dark text on bright tiles
    private static final int YELLOW = 0xFFFFE14D;
    private static final int LILAC = 0xFFE3D9FF;

    private static final int TILE_W = 240, TILE_H = 150, TILE_GAP = 44;
    private static final float TILE_FOCUS_SCALE = 1.18f;

    private static final Locale HEBREW = Locale.forLanguageTag("he");

    // ---------- State ----------

    private final List<Animator> loops = new ArrayList<>();   // background animations (paused when hidden)
    private final List<Tile> tiles = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private FrameLayout stage;
    private TextView dateView;
    private TextView focusLabel;
    private Typeface rubik;
    private Typeface fredoka;

    static class AppTarget {
        final String name;
        final int color;
        final boolean hdmi;
        final String[] packages;
        final String[] keywords;

        AppTarget(String name, int color, boolean hdmi, String[] packages, String[] keywords) {
            this.name = name;
            this.color = color;
            this.hdmi = hdmi;
            this.packages = packages;
            this.keywords = keywords;
        }
    }

    static class Tile {
        AppTarget app;
        LinearLayout view;
        GradientDrawable bg;
        View glow;
        ObjectAnimator bounce;
    }

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rubik = getResources().getFont(R.font.rubik);
        fredoka = getResources().getFont(R.font.fredoka);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        stage = new FrameLayout(this);
        stage.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        stage.setClipChildren(true);
        root.addView(stage, new FrameLayout.LayoutParams(W, H, Gravity.TOP | Gravity.LEFT));
        setContentView(root);

        buildBackground();
        buildPlanetAndRocket();
        buildClock();
        buildRobot();
        buildGreeting();
        buildTiles();

        // Scale the 1280x720 stage to the real screen, keeping the aspect ratio.
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitStage(root));
        root.post(() -> tiles.get(0).view.requestFocus());
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (Animator a : loops) a.resume();
        updateDate.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (Animator a : loops) a.pause();
        handler.removeCallbacks(updateDate);
    }

    /** This is the home screen: the Back button has nowhere to go. */
    @Override
    public void onBackPressed() {
        // intentionally empty
    }

    private void fitStage(View root) {
        if (root.getWidth() == 0 || root.getHeight() == 0) return;
        float s = Math.min(root.getWidth() / (float) W, root.getHeight() / (float) H);
        stage.setPivotX(0);
        stage.setPivotY(0);
        stage.setScaleX(s);
        stage.setScaleY(s);
        stage.setTranslationX((root.getWidth() - W * s) / 2f);
        stage.setTranslationY((root.getHeight() - H * s) / 2f);
    }

    // =====================================================================
    // Scene
    // =====================================================================

    /** Soft nebula glows, twinkling stars and two shooting stars. */
    private void buildBackground() {
        add(glowBlob(0xFF5B2BB5, 250), 300, -160, 700, 500).setAlpha(0.55f);
        add(glowBlob(0xFFB0307A, 200), 780, 440, 600, 400).setAlpha(0.45f);

        // x, y, size, color, period (ms), delay (ms)
        int[][] stars = {
                {420, 70, 8, YELLOW, 2400, 0},
                {120, 160, 6, 0xFFFFFFFF, 3100, 800},
                {600, 320, 7, 0xFF4DE1FF, 1900, 1400},
                {754, 120, 6, 0xFFFFFFFF, 2400, 0},
                {611, 400, 9, 0xFFFF5DA2, 3100, 800},
                {954, 36, 6, 0xFFA6FF4D, 1900, 1400},
                {270, 250, 5, 0xFFFFFFFF, 2400, 0},
                {820, 210, 5, 0xFFFFFFFF, 3100, 800},
                {900, 90, 7, YELLOW, 1900, 1400},
        };
        for (int[] s : stars) {
            View star = new View(this);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(s[3]);
            star.setBackground(dot);
            star.setAlpha(0.25f);
            add(star, s[0], s[1], s[2], s[2]);

            ObjectAnimator tw = ObjectAnimator.ofPropertyValuesHolder(star,
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0.25f, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f, 1.2f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f, 1.2f));
            pingPong(tw, s[4] / 2, s[5]);
        }

        shootingStar(1100, 40, 160, 0xFFFFFFFF, 6000, 0);
        shootingStar(1240, 140, 120, YELLOW, 8000, 3000);
    }

    private void shootingStar(int x, int y, int length, int color, int period, int delay) {
        View streak = new View(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{color, color & 0x00FFFFFF});
        g.setCornerRadius(3);
        streak.setBackground(g);
        streak.setRotation(-20);
        streak.setAlpha(0f);
        add(streak, x, y, length, 3);

        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(period);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(new LinearInterpolator());
        a.addUpdateListener(an -> {
            float f = (float) an.getAnimatedValue();
            if (f < 0.22f) {
                float t = f / 0.22f;                       // the streak crosses the sky in the first 22%
                streak.setTranslationX(-700 * t);
                streak.setTranslationY(255 * t);
                streak.setAlpha(t < 0.2f ? t / 0.2f : 1f - (t - 0.2f) / 0.8f);
            } else {
                streak.setAlpha(0f);                       // then waits for the next round
            }
        });
        start(a);
    }

    /** Ringed planet that drifts, and a little rocket that flies across the screen. */
    private void buildPlanetAndRocket() {
        ImageView planet = image(R.drawable.planet);
        add(planet, 50, 50, 320, 240);
        ObjectAnimator orbit = ObjectAnimator.ofPropertyValuesHolder(planet,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, -14f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 14f),
                PropertyValuesHolder.ofFloat(View.ROTATION, -6f, 4f));
        pingPong(orbit, 4000, 0);

        FrameLayout rocket = new FrameLayout(this);
        ImageView flame = image(R.drawable.rocket_flame);
        ImageView body = image(R.drawable.rocket);
        rocket.addView(flame, new FrameLayout.LayoutParams(120, 60));
        rocket.addView(body, new FrameLayout.LayoutParams(120, 60));
        add(rocket, 1300, 330, 120, 60);

        flame.setPivotX(86);   // where the flame meets the rocket
        flame.setPivotY(30);
        pingPong(ObjectAnimator.ofFloat(flame, View.SCALE_X, 0.7f, 1.25f), 250, 0);

        ValueAnimator fly = ValueAnimator.ofFloat(0f, 1f);
        fly.setDuration(14000);
        fly.setRepeatCount(ValueAnimator.INFINITE);
        fly.setInterpolator(new LinearInterpolator());
        fly.addUpdateListener(an -> {
            float f = (float) an.getAnimatedValue();
            if (f < 0.5f) {
                float t = f / 0.5f;
                rocket.setAlpha(1f);
                rocket.setTranslationX(-1500 * t);
                rocket.setTranslationY(-60 * t);
                rocket.setRotation(-8 - 4 * t);
            } else {
                rocket.setAlpha(0f);
            }
        });
        start(fly);
    }

    private void buildClock() {
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        styleText(clock, fredoka, 80, 700, YELLOW);
        clock.setIncludeFontPadding(false);
        addRight(clock, 60, 44);

        dateView = new TextView(this);
        styleText(dateView, rubik, 24, 500, LILAC);
        addRight(dateView, 60, 128);
    }

    private final Runnable updateDate = new Runnable() {
        @Override
        public void run() {
            dateView.setText(new SimpleDateFormat("EEEE, d בMMMM", HEBREW).format(new Date()));
            handler.removeCallbacks(this);
            handler.postDelayed(this, 60_000);
        }
    };

    /** The robot is three layers: body, a waving arm and blinking eyes. */
    private void buildRobot() {
        final int rw = 220, rh = 260;   // drawn from a 200x240 design
        FrameLayout robot = new FrameLayout(this);
        ImageView body = image(R.drawable.robot_body);
        ImageView arm = image(R.drawable.robot_arm);
        ImageView eyes = image(R.drawable.robot_eyes);
        robot.addView(body, new FrameLayout.LayoutParams(rw, rh));
        robot.addView(eyes, new FrameLayout.LayoutParams(rw, rh));
        robot.addView(arm, new FrameLayout.LayoutParams(rw, rh));
        add(robot, W - 90 - rw, 190, rw, rh);

        pingPong(ObjectAnimator.ofFloat(robot, View.TRANSLATION_Y, 0f, -16f), 1400, 0);

        arm.setPivotX(155f * rw / 200f);    // shoulder
        arm.setPivotY(146f * rh / 240f);
        pingPong(ObjectAnimator.ofFloat(arm, View.ROTATION, -20f, 25f), 550, 0);

        eyes.setPivotX(rw / 2f);
        eyes.setPivotY(90f * rh / 240f);
        ValueAnimator blink = ValueAnimator.ofFloat(0f, 1f);
        blink.setDuration(4000);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.setInterpolator(new LinearInterpolator());
        blink.addUpdateListener(an -> {
            float f = (float) an.getAnimatedValue();
            float d = Math.abs(f - 0.95f) / 0.03f;          // a quick blink around 95% of the cycle
            eyes.setScaleY(d < 1f ? 0.1f + 0.9f * d : 1f);
        });
        start(blink);
    }

    private void buildGreeting() {
        TextView bubble = new TextView(this);
        bubble.setText("היי! לאן טסים היום?");
        styleText(bubble, rubik, 34, 600, INK);
        bubble.setPadding(28, 16, 28, 16);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        // top-left, top-right, bottom-right, bottom-left
        bg.setCornerRadii(new float[]{30, 30, 30, 30, 6, 6, 30, 30});
        bubble.setBackground(bg);
        addRight(bubble, 340, 230);

        focusLabel = new TextView(this);
        styleText(focusLabel, rubik, 22, 700, YELLOW);
        addRight(focusLabel, 340, 318);
    }

    /** The four app tiles along the bottom, right to left, navigated with the remote's arrows. */
    private void buildTiles() {
        int y = H - 56 - TILE_H;
        for (int i = 0; i < apps.length; i++) {
            AppTarget app = apps[i];
            int x = W - 60 - TILE_W - i * (TILE_W + TILE_GAP);

            Tile t = new Tile();
            t.app = app;

            t.glow = glowBlob(app.color, 170);
            t.glow.setAlpha(0f);
            add(t.glow, x - 80, y - 60, TILE_W + 160, TILE_H + 120);

            t.bg = new GradientDrawable();
            t.bg.setColor(app.color);
            t.bg.setCornerRadius(36);

            LinearLayout v = new LinearLayout(this);
            v.setOrientation(LinearLayout.VERTICAL);
            v.setGravity(Gravity.RIGHT);
            v.setPadding(22, 22, 22, 18);
            v.setBackground(t.bg);
            v.setAlpha(0.85f);
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);
            v.setClickable(true);
            v.setId(View.generateViewId());
            v.setPivotX(TILE_W / 2f);
            v.setPivotY(TILE_H);

            ImageView icon = image(app.hdmi ? R.drawable.ic_gamepad : R.drawable.ic_play);
            v.addView(icon, new LinearLayout.LayoutParams(44, 44));

            View spacer = new View(this);
            v.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

            TextView name = new TextView(this);
            name.setText(app.name);
            styleText(name, rubik, 28, 800, INK);
            name.setIncludeFontPadding(false);
            v.addView(name, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            add(v, x, y, TILE_W, TILE_H);
            t.view = v;
            tiles.add(t);

            v.setOnFocusChangeListener((view, hasFocus) -> onTileFocus(t, hasFocus));
            v.setOnClickListener(view -> {
                view.animate().scaleX(1.05f).scaleY(1.05f).setDuration(90)
                        .withEndAction(() -> view.animate().scaleX(TILE_FOCUS_SCALE)
                                .scaleY(TILE_FOCUS_SCALE).setDuration(120).start())
                        .start();
                launch(app);
            });
        }

        // Arrow keys: left/right move along the row (it is right-to-left), up/down stay put.
        for (int i = 0; i < tiles.size(); i++) {
            View v = tiles.get(i).view;
            v.setNextFocusLeftId(tiles.get(Math.min(i + 1, tiles.size() - 1)).view.getId());
            v.setNextFocusRightId(tiles.get(Math.max(i - 1, 0)).view.getId());
            v.setNextFocusUpId(v.getId());
            v.setNextFocusDownId(v.getId());
        }
    }

    private void onTileFocus(Tile t, boolean focused) {
        View v = t.view;
        if (focused) {
            v.animate().scaleX(TILE_FOCUS_SCALE).scaleY(TILE_FOCUS_SCALE).alpha(1f).setDuration(200).start();
            t.glow.animate().alpha(1f).setDuration(250).start();
            t.bg.setStroke(5, 0xFFFFFFFF);

            t.bounce = ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 0f, -12f);
            t.bounce.setDuration(700);
            t.bounce.setRepeatCount(ValueAnimator.INFINITE);
            t.bounce.setRepeatMode(ValueAnimator.REVERSE);
            t.bounce.setInterpolator(new AccelerateDecelerateInterpolator());
            t.bounce.start();

            focusLabel.setText("← " + t.app.name);
            focusLabel.setTextColor(t.app.color);
        } else {
            if (t.bounce != null) t.bounce.cancel();
            v.animate().scaleX(1f).scaleY(1f).alpha(0.85f).translationY(0f).setDuration(200).start();
            t.glow.animate().alpha(0f).setDuration(200).start();
            t.bg.setStroke(0, 0x00000000);
        }
    }

    // =====================================================================
    // Opening apps and the HDMI input
    // =====================================================================

    private void launch(AppTarget app) {
        if (app.hdmi) {
            openHdmi();
            return;
        }
        Intent intent = findLaunchIntent(app);
        if (intent == null) {
            toast("לא מצאתי את " + app.name + " בטלוויזיה");
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            toast("לא הצלחתי לפתוח את " + app.name);
        }
    }

    private Intent findLaunchIntent(AppTarget app) {
        PackageManager pm = getPackageManager();

        // 1) Known package names
        for (String pkg : app.packages) {
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i != null) return i;
        }

        // 2) Search installed apps by name (used for Cellcom TV, whose package name may vary)
        if (app.keywords.length == 0) return null;
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : categories) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                String label = String.valueOf(ri.loadLabel(pm)).toLowerCase(Locale.ROOT);
                String pkgLower = pkg.toLowerCase(Locale.ROOT);
                for (String k : app.keywords) {
                    String key = k.toLowerCase(Locale.ROOT);
                    if (label.contains(key) || pkgLower.contains(key)) {
                        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat);
                        i.setClassName(pkg, ri.activityInfo.name);
                        return i;
                    }
                }
            }
        }
        return null;
    }

    private void openHdmi() {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        List<TvInputInfo> ports = new ArrayList<>();
        if (tim != null) {
            for (TvInputInfo info : tim.getTvInputList()) {
                // HDMI ports only (devices behind a port, like a console over HDMI-CEC, have a parent)
                if (info.getType() == TvInputInfo.TYPE_HDMI && info.getParentId() == null) {
                    ports.add(info);
                }
            }
        }
        if (ports.isEmpty()) {
            toast("לא נמצאה כניסת HDMI");
            return;
        }
        TvInputInfo port = ports.get(Math.min(HDMI_INDEX, ports.size() - 1));
        Intent i = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(port.getId()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            toast("הטלוויזיה לא אפשרה לפתוח את כניסת ה-HDMI");
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Adds a view at (x, y) on the stage, in design pixels. */
    private View add(View v, int x, int y, int w, int h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h, Gravity.TOP | Gravity.LEFT);
        lp.leftMargin = x;
        lp.topMargin = y;
        stage.addView(v, lp);
        return v;
    }

    /** Adds a wrap-content view anchored to the right edge of the stage. */
    private void addRight(View v, int right, int top) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.RIGHT);
        lp.rightMargin = right;
        lp.topMargin = top;
        stage.addView(v, lp);
    }

    private ImageView image(int resId) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(resId);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        return iv;
    }

    /** A soft round glow: the color fades out toward the edge. */
    private View glowBlob(int color, float radius) {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(radius);
        g.setColors(new int[]{color, color & 0x00FFFFFF});
        v.setBackground(g);
        return v;
    }

    private void styleText(TextView tv, Typeface font, int sizePx, int weight, int color) {
        tv.setTypeface(font);
        tv.setFontVariationSettings("'wght' " + weight);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        tv.setTextColor(color);
    }

    /** Runs an animator back and forth forever. */
    private void pingPong(ObjectAnimator a, int halfPeriod, int delay) {
        a.setDuration(halfPeriod);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        start(a);
    }

    private void start(Animator a) {
        loops.add(a);
        a.start();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
