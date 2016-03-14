package hk.ust.cse.comp4521.chatapp;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import hk.ust.cse.comp4521.chatapp.Adapter.MessageAdapter;
import hk.ust.cse.comp4521.chatapp.Model.Message;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {
    private EditText mInputMessageView;
    private RecyclerView mMessagesView;
    private List<Message> mMessages = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;
    private static String TAG = "Chat Fragment";

    private Socket socket;

    {
        try {
            // Get the socket
            socket = IO.socket("http://192.168.1.38:3000");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        socket.connect();
        // Receive message through the socket
        socket.on("message", handleIncomingMessages);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessagesView = (RecyclerView) view.findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new MessageAdapter(mMessages);
        mMessagesView.setAdapter(mAdapter);

        ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        mInputMessageView = (EditText) view.findViewById(R.id.message_input);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String message = mInputMessageView.getText().toString().trim();
        mInputMessageView.setText("");
        Log.i(TAG, "sendMessage(): " + message);
        addMessage(message);
        socket.emit("message", message);
    }

    private void addMessage(String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .message(message).build());
        Log.i(TAG, "addMessage(): " + mMessages.size());
        mAdapter = new MessageAdapter(mMessages);
        mAdapter.notifyItemInserted(0);
        scrollToBottom();
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private Emitter.Listener handleIncomingMessages = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String message;
                    String imageText;
                    try {
                        message = data.getString("text").toString();
                        addMessage(message);

                    } catch (JSONException e) {
                        // return;
                    }
                    try {
                        imageText = data.getString("image");
                        // addImage(decodeImage(imageText));
                    } catch (JSONException e) {
                        //return;
                    }

                }
            });
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Disconnect from the socket
        socket.disconnect();
    }
}
