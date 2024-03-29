package ar.edu.ips.aus.seminario2.sampleproject;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.abemart.wroup.client.WroupClient;
import com.abemart.wroup.common.WroupDevice;
import com.abemart.wroup.common.listeners.ClientConnectedListener;
import com.abemart.wroup.common.listeners.ClientDisconnectedListener;
import com.abemart.wroup.common.listeners.DataReceivedListener;
import com.abemart.wroup.common.messages.MessageWrapper;
import com.abemart.wroup.service.WroupService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static ar.edu.ips.aus.seminario2.sampleproject.Message.MessageType.PLAYER_DATA;

public class MazeBoardActivity extends AppCompatActivity
        implements View.OnClickListener, DataReceivedListener,
        ClientConnectedListener, ClientDisconnectedListener {

    public static final String EXTRA_SERVER_NAME = "SERVER_NAME";
    public static final String EXTRA_IS_SERVER = "IS_SERVER";
    public static final String EXTRA_OPTION = "OPTION";
    private static final String TAG = MazeBoardActivity.class.getSimpleName();
    private static final int MAX_DEVICES = 3;

    private Button buttonUp, buttonDown, buttonLeft, buttonRight, buttonPause;

    ImageView[] imageViews = null;

    private GameView mazeView;
    private final HashMap<String, WroupDevice> devices = new HashMap<String, WroupDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maze);

        buttonUp = findViewById(R.id.buttonUp);
        buttonDown = findViewById(R.id.buttonDown);
        buttonLeft = findViewById(R.id.buttonLeft);
        buttonRight = findViewById(R.id.buttonRight);
        buttonPause = findViewById(R.id.buttonPause);

        buttonUp.setOnClickListener(this);
        buttonDown.setOnClickListener(this);
        buttonLeft.setOnClickListener(this);
        buttonRight.setOnClickListener(this);
        buttonPause.setOnClickListener(this);

        mazeView = (GameView)findViewById(R.id.gameView);
        mazeView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mazeView.setZOrderMediaOverlay(true);
        mazeView.setZOrderOnTop(true);

        GameApp.getInstance().setServerName(getIntent().getStringExtra(this.EXTRA_SERVER_NAME));
        GameApp.getInstance().setGameServer(getIntent().getBooleanExtra(this.EXTRA_IS_SERVER, false));
        String option = getIntent().getStringExtra(this.EXTRA_OPTION);
        if (GameApp.getInstance().isGameServer()){
            WroupService server = WroupService.getInstance(this);
            server.setDataReceivedListener(this);
            server.setClientDisconnectedListener(this);
            server.setClientConnectedListener(this);
            GameApp.getInstance().setServer(server);
        } else {
            WroupClient client = WroupClient.getInstance(this);
            client.setDataReceivedListener(this);
            client.setClientDisconnectedListener(this);
            client.setClientConnectedListener(this);
            GameApp.getInstance().setClient(client);
        }

        if(GameApp.getInstance().isGameServer()) {
            MazeBoard board = MazeBoard.from(option);
            GameApp.getInstance().setMazeBoard(board);
            setupMazeBoard(board);
        }
    }

    private void setupMazeBoard(MazeBoard board) {

        int height = board.getVerticalTileCount();
        int width = board.getHorizontalTileCount();

        imageViews = new ImageView[width * height];

        int resId = 0;

        TableLayout table = findViewById(R.id.mazeBoard);
        for (int i=0; i<height; i++){
            TableRow row = new TableRow(this);
            TableLayout.LayoutParams rowParams = new TableLayout.LayoutParams();
            rowParams.width = TableRow.LayoutParams.MATCH_PARENT;
            rowParams.height = TableRow.LayoutParams.MATCH_PARENT;
            rowParams.weight = 1;
            rowParams.gravity = Gravity.CENTER;
            row.setLayoutParams(rowParams);
            table.addView(row);

            for (int j=0; j<width; j++){
                BoardPiece piece = board.getPiece(j, i);

                resId = lookupResource(piece);

                ImageView imageView = new ImageView(this);
                imageView.setBackgroundResource(resId);
                TableRow.LayoutParams imageViewParams = new TableRow.LayoutParams();
                imageViewParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                imageViewParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                imageViewParams.weight = 1;
                imageView.setLayoutParams(imageViewParams);

                row.addView(imageView);

                imageViews[(j%board.getHorizontalTileCount())+ board.getVerticalTileCount()*i] = imageView;
            }
        }
        table.invalidate();
   }

    private int lookupResource(BoardPiece piece) {
        if(piece.getExit()){
            return R.drawable.m5b;
        }else{
            int iconIndex = 0b1000 * (piece.isOpen(MazeBoard.Direction.WEST)? 1:0) +
                    0b0100 * (piece.isOpen(MazeBoard.Direction.NORTH)? 1:0) +
                    0b0010 * (piece.isOpen(MazeBoard.Direction.EAST)? 1:0) +
                    0b0001 * (piece.isOpen(MazeBoard.Direction.SOUTH)? 1:0);

            int[] iconLookupTable = { 0,
                    R.drawable.m1b,
                    R.drawable.m1r,
                    R.drawable.m2rb,
                    R.drawable.m1t,
                    R.drawable.m2v,
                    R.drawable.m2tr,
                    R.drawable.m3l,
                    R.drawable.m1l,
                    R.drawable.m2bl,
                    R.drawable.m2h,
                    R.drawable.m3t,
                    R.drawable.m2lt,
                    R.drawable.m3r,
                    R.drawable.m3b,
                    R.drawable.m4};

            return iconLookupTable[iconIndex];
        }
    }

    @Override
    public void onClick(View v) {
        if (v == buttonUp) {
            mazeView.getPlayer().setNewDirection(MazeBoard.Direction.NORTH);
        }
        else if (v == buttonDown) {
            mazeView.getPlayer().setNewDirection(MazeBoard.Direction.SOUTH);
        }
        else if (v == buttonLeft) {
            mazeView.getPlayer().setNewDirection(MazeBoard.Direction.WEST);
        }
        else if (v == buttonRight) {
            mazeView.getPlayer().setNewDirection(MazeBoard.Direction.EAST);
        } else if (v == buttonPause) {
            mazeView.toggleStatus();
        }

    }

    @Override
    public void onClientConnected(final WroupDevice wroupDevice) {
        // if we are server send maze board
        if (GameApp.getInstance().isGameServer()) {
            addToDeviceList(wroupDevice);

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WroupService server = GameApp.getInstance().getServer();
            MessageWrapper message = new MessageWrapper();
            Gson json = new Gson();
            Message<MazeBoard> data = new Message<MazeBoard>(Message.MessageType.GAME_DATA, GameApp.getInstance().getMazeBoard());
            String msg = json.toJson(data);
            message.setMessage(msg);
            message.setMessageType(MessageWrapper.MessageType.NORMAL);
            server.sendMessage(wroupDevice, message); ///!!!!
        }


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), getString(R.string.device_connected, wroupDevice.getDeviceName()), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onClientDisconnected(final WroupDevice wroupDevice) {
        if (GameApp.getInstance().isGameServer())
            removeFromDeviceList(wroupDevice);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), getString(R.string.device_disconnected, wroupDevice.getDeviceName()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean addToDeviceList(WroupDevice wroupDevice) {
        if (devices.size() < MAX_DEVICES) {
            devices.put(wroupDevice.getDeviceMac(), wroupDevice);
            return true;
        }
        return false;
    }

    private boolean removeFromDeviceList(WroupDevice wroupDevice) {
        return (devices.remove(wroupDevice.getDeviceMac()) != null);
    }

    @Override
    public void onDataReceived(MessageWrapper messageWrapper) {
        Gson gson = new Gson();

        if (!GameApp.getInstance().isGameServer()) {
            // client may receive different kind of messages from server
            JsonObject object = JsonParser.parseString(messageWrapper.getMessage()).getAsJsonObject();
            JsonElement typeElement = object.get("type");
            switch (Message.MessageType.valueOf(typeElement.getAsString())){
                case PLAYER_DATA:
                    mazeView.updatePlayerData(messageWrapper.getMessage());
                    break;
                case GAME_DATA:
                    String message = messageWrapper.getMessage();
                    Message<MazeBoard> newMaze = gson.fromJson(message,
                            new TypeToken<Message<MazeBoard>>(){}.getType());
                    final MazeBoard board = newMaze.getPayload();
                    Log.d("CLIENTE: GAME_DATA maze", gson.toJson(board));
                    GameApp.getInstance().setMazeBoard(board);
                    try{
                        Thread.sleep(1000L);
                    }catch(Exception e){}
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Stuff that updates the UI
                            setupMazeBoard(board);
                        }
                    });
                    break;
                case GAME_STATUS:
                    mazeView.updateStatus(messageWrapper.getMessage());
                    break;
            }
        } else {
            // server receives individual player data
            if (devices.containsKey(messageWrapper.getWroupDevice().getDeviceMac())){
                mazeView.updatePlayerData(messageWrapper.getMessage());
            }
        }

    }



}
