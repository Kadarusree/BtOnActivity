package mahesh.cubex.btonactivity;



import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.core.Fritz;


import ai.fritz.fritzvisionobjectmodel.FritzVisionObjectPredictor;
import ai.fritz.fritzvisionobjectmodel.FritzVisionObjectPredictorOptions;
import ai.fritz.fritzvisionobjectmodel.FritzVisionObjectResult;
import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.inputs.FritzVisionImage;
import ai.fritz.vision.inputs.FritzVisionOrientation;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import mahesh.cubex.btonactivity.logger.Log;
import persondetector.BaseCameraActivity;
import persondetector.OverlayView;

public class DetectorActivity extends BaseCameraActivity implements ImageReader.OnImageAvailableListener, PictureCapturingListener{
    DetectorActivity activity;
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int SELECT_PICTURES = 37;

    // Layout Views
    private ListView mConversationView;
    private Button mSendButton, button_detect;


    private String mConnectedDeviceName = null;
    private ChatAdapter mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    private APictureCapturingService pictureService;
    ArrayList<Bitmap> mBitmaps;






    private static final String TAG = DetectorActivity.class.getSimpleName();
    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 960);
    private AtomicBoolean computing = new AtomicBoolean(false);
    private Toast toast;

    // STEP 1:
    // TODO: Define the predictor variable
    private FritzVisionObjectPredictor predictor;
    // END STEP 1

    FritzVisionObjectResult result;
    FritzVisionImage visionImage;

    boolean sendingInprogress = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fritz.configure(this, "4070261be7804512bf4e82aafa8e2f5c");

        // STEP 1: Get the predictor and set the options.
        // ----------------------------------------------
        // TODO: Add the predictor snippet here
        FritzVisionObjectPredictorOptions options = new FritzVisionObjectPredictorOptions.Builder()
                .confidenceThreshold(.4f)
                .build();
        predictor = new FritzVisionObjectPredictor(options);
        // ----------------------------------------------
        // END STEP 1



        ///Related to Image Sending
        mConversationView = (ListView) findViewById(R.id.in);
        mSendButton = (Button) findViewById(R.id.button_send);
        //  mSendButton.setEnabled(false);
        button_detect = findViewById(R.id.button_detect);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        activity= this;
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        button_detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePic();
            }
        });

        mBitmaps = new ArrayList<>();

    }

    private void capturePic() {
        /*pictureService = PictureCapturingServiceImpl.getInstance(DetectorActivity.this);
        pictureService.startCapturing(DetectorActivity.this);
        sendingInprogress = true;*/

        Intent intent=new Intent();
        setResult(99,intent);
        finish();//finishing activity
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_stylize;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size previewSize, final Size cameraViewSize, final int rotation) {

        final List<String> filteredObjects = new ArrayList<>();
        filteredObjects.add("person");
        // Callback draws a canvas on the OverlayView
        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        // STEP 4: Draw the prediction result
                        // ----------------------------------
                        if (result == null) {
                            return;
                        }

                        // Go through all results
                        for (FritzVisionObject object : result.getVisionObjects()) {
                            String labelText = object.getVisionLabel().getText();


                        float MinconfidenceValue = 0.5f;
                        float actualConfidenceValue = object.getVisionLabel().getConfidence();

                            if (filteredObjects.contains(labelText) && actualConfidenceValue>=MinconfidenceValue ){


                                    capturePic();



                            }

                            float scaleFactorWidth = ((float) cameraViewSize.getWidth()) / visionImage.getBitmap().getWidth();
                            float scaleFactorHeight = ((float) cameraViewSize.getHeight()) / visionImage.getBitmap().getHeight();
                            object.drawOnCanvas(getApplicationContext(), canvas, scaleFactorWidth, scaleFactorHeight);

                        }


                        // ----------------------------------
                        // END STEP 4
                    }
                });


    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (!computing.compareAndSet(false, true)) {
            image.close();
            return;
        }

        // STEP 2: Create the FritzVisionImage object from media.Image
        // ------------------------------------------------------------------------
        // TODO: Add code for creating FritzVisionImage from a media.Image object
        int rotationFromCamera = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        visionImage = FritzVisionImage.fromMediaImage(image, rotationFromCamera);
        // ------------------------------------------------------------------------
        // END STEP 2

        image.close();
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        // STEP 3: Run predict on the image
                        // ---------------------------------------------------
                        // TODO: Add code for running prediction on the image
                        // final long startTime = SystemClock.uptimeMillis();
                        result = predictor.predict(visionImage);
                        // ----------------------------------------------------
                        // END STEP 3


                        // Fire callback to change the OverlayView
                        requestRender();
                        computing.set(false);
                    }
                });
    }

    ///////////Related to Image Sending///////

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_chat, menu);
        return true;
    }


    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ChatAdapter(mBitmaps,activity);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                   /* TextView textView = (TextView)findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);*/

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*"); //allows any image file type. Change * to specific extension to limit it
//**These following line is the important one!
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURES);


            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
 //       mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        Activity activity = this;
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(R.string.title_connected_to);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    Bitmap bmp = BitmapFactory.decodeByteArray(writeBuf, 0, writeBuf.length);
                    Toast.makeText(activity,"Message Sent",Toast.LENGTH_LONG).show();
                    sendingInprogress = false;
                    mBitmaps.add(bmp);
                    mConversationArrayAdapter.notifyDataSetChanged();
                    break;
                case Constants.MESSAGE_READ:
                    Bitmap readBuf = (Bitmap) msg.obj;

                    Toast.makeText(activity,"Message Received",Toast.LENGTH_LONG).show();
                    mBitmaps.add(readBuf);
                    mConversationArrayAdapter.notifyDataSetChanged();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(activity, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }

            case SELECT_PICTURES:
                if(requestCode == SELECT_PICTURES) {
                    if(resultCode == Activity.RESULT_OK) {
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                            Uri imageUri = null;
                            for (int i = 0; i < count; i++) {
                                Bitmap bitmap = null;
                                imageUri = data.getClipData().getItemAt(i).getUri();

                                try {
                                    bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    if (bitmap != null) {
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                        byte[] byteArray = stream.toByteArray();

                                        Model m = new Model(byteArray, Utils.storagePath);

                                        //   mChatService.write(byteArray);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }


                        }
                        //do something with the image (save it to some directory or whatever you need to do with it here)

                        else if (data.getData() != null) {
                            Uri imageUri = data.getData();

                            Bitmap bitmap = null;

                            try {
                                bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                if (bitmap != null) {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                    byte[] byteArray = stream.toByteArray();

                                    ;

                                    mChatService.write(byteArray);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //do something with the image (save it to some directory or whatever you need to do with it here)
                        }
                    }
                }
                break;
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        Toast.makeText(getApplicationContext(),"Picture Captured",Toast.LENGTH_LONG).show();

        if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);

                Toast.makeText(activity,pictureUrl+"",Toast.LENGTH_LONG).show();
                Utils.bitmap = scaled;
                sendPicture(scaled);


            });
            //  showToast("Picture saved to " + pictureUrl);
        }
    }

    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {

    }
    public void sendPicture(Bitmap mBitmap){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if(mBitmap!=null) {
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            mChatService.write(byteArray);
        }
    }
}
