package com.example.chatbot;

import static com.example.chatbot.MessageModel.SENT_BY_BOT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    RecyclerView recycler_view;
    TextView welcome_text;
    EditText message_edit_text;
    ImageButton send_button;

    List<MessageModel> messagelist;

    MessageAdapter messageAdapter;

    public static final MediaType JSON = MediaType.get("application/json");

    OkHttpClient client = new OkHttpClient();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recycler_view = findViewById(R.id.recycler_view);
        welcome_text = findViewById(R.id.welcome_text);
        message_edit_text = findViewById(R.id.message_edit_text);
        send_button = findViewById(R.id.send_btn);

        messagelist = new ArrayList<>();

        //set recycler view
        messageAdapter = new MessageAdapter(messagelist);
        recycler_view.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recycler_view.setLayoutManager(llm);


        send_button.setOnClickListener((v)->{
            String question = message_edit_text.getText().toString().trim();
            addToChat(question,MessageModel.SENT_BY_ME);
            message_edit_text.setText("");
            callApi(question);
            welcome_text.setVisibility(View.GONE);
        });
    }

    //run inside ui
    void  addToChat(String message, String sentBy){
        runOnUiThread(new Runnable() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run() {
                messagelist.add(new MessageModel(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                recycler_view.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }
    void addResponse(String response){
       messagelist.remove(messagelist.size()-1);
        addToChat(response, SENT_BY_BOT);
    }

    void callApi(String question){
        //okhttp
        messagelist.add(new MessageModel("Typing...", SENT_BY_BOT));

        JSONObject jsonBody= new JSONObject();
        try {
            jsonBody.put("model","gpt-3.5-turbo-instruct");
            jsonBody.put("prompt", question);
            jsonBody.put( "max_tokens",4000);
            jsonBody.put("temperature", 0);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("\n" +
                        "https://api.openai.com/v1/completions")
                .header("Authorization"," Bearer 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")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("failed to load response due to "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }else{
                    addResponse("failed to load response due to "+response.body().string());
                }
            }
        });
    }
}