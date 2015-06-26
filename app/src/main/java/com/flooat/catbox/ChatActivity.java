package com.flooat.catbox;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.flooat.catbox.models.App;
import com.flooat.catbox.models.AppUtil;
import com.flooat.catbox.models.Message;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class ChatActivity extends Activity {

    private static final int REQUEST_LOGIN = 0;
    private static final int TYPING_TIMER_LENGTH = 600;

    private SharedPreferences prefs;
    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private List<Message> mMessages = new ArrayList<Message>();
    private RecyclerView.Adapter mAdapter;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();

    String boxId;
    String boxName;
    IO.Options opts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        final Intent i = getIntent();
        boxId = i.getStringExtra("id");
        boxName = i.getStringExtra("name");

        setTitle(boxName);

        opts = new IO.Options();
        opts.query = "boxId=" + boxId + "&name=" + App.userName + "&userId=" + App.userId;
        opts.forceNew = true;
        opts.reconnection = false;
        {
            try {
                App.mSocket = IO.socket(App.URL, opts);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        Log.d("socket.io", "Open socket"); // Dev
        App.mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        App.mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        App.mSocket.on(Socket.EVENT_CONNECT, onConnect);
        App.mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        App.mSocket.on("add user", onNewMessage);
        App.mSocket.on("new message", onNewMessage);
        App.mSocket.on("user joined", onUserJoined);
        App.mSocket.on("user left", onUserLeft);
        App.mSocket.on("typing", onTyping);
        App.mSocket.on("stop typing", onStopTyping);
        App.mSocket.connect();

        mAdapter = new MessageAdapter(getApplicationContext(), mMessages);
        mMessagesView = (RecyclerView) findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mMessagesView.setAdapter(mAdapter);

        mInputMessageView = (EditText) findViewById(R.id.message_input);
        mInputMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });
        mInputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!App.mSocket.connected()) return;

                if (!mTyping) {
                    mTyping = true;
                    App.mSocket.emit("typing");
                }

                mTypingHandler.removeCallbacks(onTypingTimeout);
                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend();
            }
        });

        addLog(getResources().getString(R.string.message_welcome));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        App.mSocket.off("add user", onNewMessage);
        App.mSocket.off("new message", onNewMessage);
        App.mSocket.off("user joined", onUserJoined);
        App.mSocket.off("user left", onUserLeft);
        App.mSocket.off("typing", onTyping);
        App.mSocket.off("stop typing", onStopTyping);
        App.mSocket.disconnect();
        App.mSocket.close();
    }

    private void addLog(String message) {
        mMessages.add(new Message.Builder(Message.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addMessage(String name, String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .name(name).message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addTyping(String name) {
        mMessages.add(new Message.Builder(Message.TYPE_ACTION)
                .name(name).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void removeTyping(String name) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            Message message = mMessages.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getName().equals(name)) {
                mMessages.remove(i);
                mAdapter.notifyItemRemoved(i);
            }
        }
    }

    private void attemptSend() {
        if (null == App.userId) return;
        if (!App.mSocket.connected()) return;

        mTyping = false;

        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return;
        }

        mInputMessageView.setText("");
        addMessage(App.userName, message);

        // perform the sending message attempt.
        App.mSocket.emit("new message", message);
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        String name = data.getString("name");
                        String message = data.getString("message");

                        removeTyping(name);
                        addMessage(name, message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String name;
                    try {
                        name = data.getString("name");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_joined, name));
                }
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String name;
                    try {
                        name = data.getString("name");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_left, name));
                    removeTyping(name);
                }
            });
        }
    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String name;
                    try {
                        name = data.getString("name");
                    } catch (JSONException e) {
                        return;
                    }
                    addTyping(name);
                }
            });
        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String name;
                    try {
                        name = data.getString("name");
                    } catch (JSONException e) {
                        return;
                    }
                    removeTyping(name);
                }
            });
        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping)
                return;

            mTyping = false;
            App.mSocket.emit("stop typing");
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("socket.io", "Could not connect to server."); // Dev
                    Toast.makeText(ChatActivity.this.getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("socket.io", "Connected Successfully"); // Dev
                    AppUtil.showToast(String.format("You have joined %s", boxName), ChatActivity.this);
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ChatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("socket.io", "Disconnected connection");
                }
            });
        }
    };


}
