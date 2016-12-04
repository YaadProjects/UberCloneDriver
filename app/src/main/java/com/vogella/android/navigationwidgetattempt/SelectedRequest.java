package com.vogella.android.navigationwidgetattempt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.Wisam.Events.PassengerCanceled;
import com.Wisam.POJO.StatusResponse;

import org.greenrobot.eventbus.EventBus;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SelectedRequest extends AppCompatActivity {

    private static final String TAG = "Selected Request";
    private Intent intent;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_request);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        prefManager = new PrefManager(this);

        intent = getIntent();

        ((TextView) findViewById(R.id.request_details_pickup)).setText(intent.getStringExtra("pickup_text"));
        ((TextView) findViewById(R.id.request_details_dest)).setText(intent.getStringExtra("dest_text"));
        ((TextView) findViewById(R.id.request_details_price)).setText(intent.getStringExtra("price"));
        ((TextView) findViewById(R.id.request_details_time)).setText(intent.getStringExtra("time"));

        ((TextView) findViewById(R.id.request_details_start)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        ((TextView) findViewById(R.id.request_details_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBus.getDefault().post(new PassengerCanceled(intent.getStringExtra("request_id")));
                sendCancel(intent.getStringExtra("request_id"));

            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id==android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    private void sendCancel(final String request_id) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RestServiceConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String email = prefManager.pref.getString("UserEmail", "");
        String password = prefManager.pref.getString("UserPassword", "");

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.cancelling_request));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();


        RestService service = retrofit.create(RestService.class);
        Call<StatusResponse> call = service.cancel("Basic " + Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP),
                request_id);
        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                if(progress.isShowing()) progress.dismiss();
                Log.d(TAG, "onResponse: raw: " + response.body());
                if (response.isSuccess() && response.body() != null) {
//                    endRequest(REQUEST_CANCELLED);
//                    setUI(MainActivity.UI_STATE.SIMPLE);
                    Toast.makeText(SelectedRequest.this, R.string.request_cancelled_successfully, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "The request has been cancelled successfully");
                    Intent temp = new Intent();
                    setResult(RESULT_OK, temp);
                    finish();
                } else if (response.code() == 401) {
                    Toast.makeText(SelectedRequest.this, R.string.authorization_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onResume: User not logged in");
                    prefManager.setIsLoggedIn(false);
                    Intent intent = new Intent(SelectedRequest.this, LoginActivity.class);
                    startActivity(intent);
//                    finish();
                } else {
//                    clearHistoryEntries();
                    Toast.makeText(SelectedRequest.this, R.string.server_unknown_error, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Unknown error occurred");
                }

            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                if(progress.isShowing()) progress.dismiss();
                Toast.makeText(SelectedRequest.this, R.string.server_timeout, Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Couldn't connect to the server");
            }
        });
    }


}