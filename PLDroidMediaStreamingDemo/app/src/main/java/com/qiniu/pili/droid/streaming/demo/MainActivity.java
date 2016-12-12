package com.qiniu.pili.droid.streaming.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String url = "Your app server url which get StreamJson";
    private static final String url2 = "Your app server url which get PublishUrl";

    private static final String INPUT_TYPE_STREAM_JSON      = "StreamJson";
    private static final String INPUT_TYPE_AUTHORIZED_URL   = "AuthorizedUrl";
    private static final String INPUT_TYPE_UNAUTHORIZED_URL = "UnauthorizedUrl";

    private static final String[] mInputTypeList = {
            "Please select input type of publish url:",
            INPUT_TYPE_STREAM_JSON,
            INPUT_TYPE_AUTHORIZED_URL,
            INPUT_TYPE_UNAUTHORIZED_URL
    };

    private Button mHwCodecCameraStreamingBtn;
    private Button mSwCodecCameraStreamingBtn;
    private Button mAudioStreamingBtn;

    private EditText mInputUrlEditText;

    private String mSelectedInputType = null;

    private Intent intent; //add for permission jump

    private static boolean isSupportHWEncode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    private String requestStream(String appServerUrl) {
        try {
            HttpURLConnection httpConn = (HttpURLConnection) new URL(appServerUrl).openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setConnectTimeout(5000);
            httpConn.setReadTimeout(10000);
            int responseCode = httpConn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int length = httpConn.getContentLength();
            if (length <= 0) {
                length = 16*1024;
            }
            InputStream is = httpConn.getInputStream();
            byte[] data = new byte[length];
            int read = is.read(data);
            is.close();
            if (read <= 0) {
                return null;
            }
            return new String(data, 0, read);
        } catch (Exception e) {
            showToast("Network error!");
        }
        return null;
    }

    void showToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startStreamingActivity(final Intent intent) {
        //judge the permission
        //now meizu's permission behaviour is different with other manufacturer
        //at least in Flyme Android 5.1
        boolean isMeizu = false;
        if (Build.FINGERPRINT.contains("Flyme")
                || Pattern.compile("Flyme", Pattern.CASE_INSENSITIVE).matcher(Build.DISPLAY).find()
                || Build.MANUFACTURER.contains("Meizu")
                || Build.MANUFACTURER.contains("MeiZu")) {
            Log.i(TAG, "the phone is meizu");
            isMeizu = true;
        }
        if (!isMeizu && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "no permission");
            this.intent = intent;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 0);
            return;
        } else {
            Log.e(TAG, "has permission");
        }
        String inputText = mInputUrlEditText.getText().toString().trim();
        final String inputUrl = inputText.equals("") ? "rtmp://pili-publish.faceunity.com/faceunity-test/demotest" : inputText;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String publishUrl = null;
                Log.i(TAG, "mSelectedInputType:" + mSelectedInputType + ",inputUrl:" + inputUrl);
                if (!"".equalsIgnoreCase(inputUrl)) {
                    publishUrl = Config.EXTRA_PUBLISH_URL_PREFIX + inputUrl;
                } else {
                    if (mSelectedInputType != null) {
                        if (INPUT_TYPE_STREAM_JSON.equalsIgnoreCase(mSelectedInputType)) {
                            publishUrl = requestStream(url);
                            if (publishUrl != null) {
                                publishUrl = Config.EXTRA_PUBLISH_JSON_PREFIX + publishUrl;
                            }
                        } else if (INPUT_TYPE_AUTHORIZED_URL.equalsIgnoreCase(mSelectedInputType)) {
                            publishUrl = requestStream(url2);
                            if (publishUrl != null) {
                                publishUrl = Config.EXTRA_PUBLISH_URL_PREFIX + publishUrl;
                            }
                        } else if (INPUT_TYPE_UNAUTHORIZED_URL.equalsIgnoreCase(mSelectedInputType)) {
                            // just for test
                            publishUrl = requestStream(url2);
                            try {
                                URI u = new URI(publishUrl);
                                publishUrl = Config.EXTRA_PUBLISH_URL_PREFIX + String.format("rtmp://401.qbox.net%s?%s", u.getPath(), u.getRawQuery());
                            } catch (Exception e) {
                                e.printStackTrace();
                                publishUrl = null;
                            }
                        } else {
                            throw new IllegalArgumentException("Illegal input type");
                        }
                    }
                }

                if (publishUrl == null) {
                    showToast("Publish Url Got Fail!");
                    return;
                }
                intent.putExtra(Config.EXTRA_KEY_PUB_URL, publishUrl);
                startActivity(intent);
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //now i just regard it as CAMERA
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startStreamingActivity(intent);
        } else {
            Toast.makeText(this, "you must permit the camera permission!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView mVersionInfoTextView = (TextView) findViewById(R.id.version_info);
        mVersionInfoTextView.setText(Config.VERSION_HINT);

        Spinner inputTypeSpinner = (Spinner) findViewById(R.id.spinner_input_type);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mInputTypeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputTypeSpinner.setAdapter(adapter);
        inputTypeSpinner.setOnItemSelectedListener(new SpinnerSelectedListener());
        inputTypeSpinner.setVisibility(View.VISIBLE);
        //set default is Authorized Url
        inputTypeSpinner.setSelection(Arrays.asList(mInputTypeList).indexOf(INPUT_TYPE_AUTHORIZED_URL));

        mInputUrlEditText = (EditText) findViewById(R.id.input_url);

        mHwCodecCameraStreamingBtn = (Button) findViewById(R.id.hw_codec_camera_streaming_btn);
        mHwCodecCameraStreamingBtn.setVisibility(View.GONE);
        mHwCodecCameraStreamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HWCodecCameraStreamingActivity.class);
                startStreamingActivity(intent);
            }
        });

        mSwCodecCameraStreamingBtn = (Button) findViewById(R.id.sw_codec_camera_streaming_btn);
        mSwCodecCameraStreamingBtn.setVisibility(View.GONE);
        mSwCodecCameraStreamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SWCodecCameraStreamingActivity.class);
                startStreamingActivity(intent);
            }
        });

        mAudioStreamingBtn = (Button) findViewById(R.id.start_pure_audio_streaming_btn);
        mAudioStreamingBtn.setVisibility(View.GONE);
        mAudioStreamingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AudioStreamingActivity.class);
                startStreamingActivity(intent);
            }
        });
    }

    private class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            if (arg2 != 0) {
                mSelectedInputType = mInputTypeList[arg2];
                if (isSupportHWEncode()) {
                    mHwCodecCameraStreamingBtn.setVisibility(View.VISIBLE);
                }
                mSwCodecCameraStreamingBtn.setVisibility(View.VISIBLE);
                mAudioStreamingBtn.setVisibility(View.VISIBLE);
            }
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }
}
