package com.yashoid.openpainting;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity implements TouchGatherer.OnBatchListener {

    private static final String LOCAL_HOST = "http://192.168.1.102:44127/";
    private static final String REMOTE_HOST = "https://openpainting.yashoid.com/socket/";

    private static final String HOST = LOCAL_HOST;

    private String mColor;

    private WorldView mWorld;
    private ImageView mToggleMode;

    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setLogo(R.drawable.ic_launcher_foreground);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_launcher_foreground);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        float[] hsv = { 0f, 1, 0.7f };
        hsv[0] = new Random().nextFloat() * 360;
        mColor = Integer.toHexString(Color.HSVToColor(hsv)).substring(2);

        mWorld = findViewById(R.id.world);
        mToggleMode = findViewById(R.id.toggle_mode);

        try {
            mSocket = IO.socket(HOST);

            mSocket.on(Socket.EVENT_CONNECT, args -> Log.d("AAA", "connected"));
            mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.d("AAA", "connect error"));

            mSocket.on("snapshot", args -> {
                String snapShot = args[1].toString();
                createBitmap(snapShot);
            });

            mSocket.on("paint", args -> {
                try {
                    JSONObject data = new JSONObject(args[0].toString());
                    JSONArray jTouches = data.getJSONArray("t");

                    for (int i = 0; i < jTouches.length(); i++) {
                        Touch touch = new Touch(jTouches.getJSONObject(i));
                        mWorld.setColor(touch.x, touch.y, Color.parseColor("#" + touch.color));
                    }
                } catch (JSONException e) { }
            });

            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        final TouchGatherer touchGatherer = new TouchGatherer(this);

        mWorld.setOnPointListener(new WorldView.OnPointListener() {

            @Override
            public void onPoint(int x, int y) {
                touchGatherer.addTouchConnecting(new Touch(x, y, mColor));
            }

            @Override
            public void onTouchFinished() {
                touchGatherer.disconnectTouches();
            }

        });

        setMode(WorldView.MODE_PAINT);

        mToggleMode.setOnClickListener(v -> setMode(mWorld.getMode() == WorldView.MODE_PAINT ? WorldView.MODE_PAN : WorldView.MODE_PAINT));

        findViewById(R.id.button_palette).setOnClickListener(v -> {
            ColorPickerDialogBuilder
                    .with(this)
                    .setTitle("Choose color")
                    .initialColor(Color.parseColor("#" + mColor))
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .lightnessSliderOnly()
                    .density(12)
                    .setPositiveButton("ok", (dialog, selectedColor, allColors) -> mColor = Integer.toHexString(selectedColor).substring(2))
                    .setNegativeButton("cancel", (dialog, which) -> {})
                    .build()
                    .show();
        });
    }

    private void setMode(int mode) {
        mWorld.setMode(mode);

        switch (mode) {
            case WorldView.MODE_PAINT:
                mToggleMode.setImageResource(R.drawable.ic_paint);
                break;
            case WorldView.MODE_PAN:
                mToggleMode.setImageResource(R.drawable.ic_hand);
                break;
        }
    }

    @Override
    public void sendBatchTouches(List<Touch> touches) {
        JSONObject data = new JSONObject();

        try {
            JSONArray jTouches = new JSONArray();

            for (Touch touch: touches) {
                jTouches.put(touch.toJSON());
            }

            data.put("t", jTouches);
        } catch (JSONException e) { }

        mSocket.emit("paint", data.toString());
    }

    private void createBitmap(String snapShot) {
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                int c = (j * 256 + i) * 6;
                String color = "#" + snapShot.substring(c, c + 6);
                bitmap.setPixel(i, j, Color.parseColor(color));
            }
        }

        runOnUiThread(() -> mWorld.setBitmap(bitmap));
    }

}