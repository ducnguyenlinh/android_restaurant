package com.example.android_restaurant;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.example.android_restaurant.Common.Common;
import com.example.android_restaurant.Retrofit.IMyRestaurantAPI;
import com.example.android_restaurant.Retrofit.RetrofitClient;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int APP_REQUEST_CODE=1234;

    IMyRestaurantAPI myRestaurantAPI;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog;

    @BindView(R.id.btn_sign_in)
    Button btn_sign_in;

    @OnClick(R.id.btn_sign_in)
    void loginUser(){
        Intent intent = new Intent(this, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder builder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(
                        LoginType.PHONE,
                        AccountKitActivity.ResponseType.TOKEN);

        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, builder.build());
        startActivityForResult(intent, APP_REQUEST_CODE);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == APP_REQUEST_CODE) {
            AccountKitLoginResult loginResult = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
            String toastMessage;

            if (loginResult.getError() != null) {
                toastMessage = loginResult.getError().getErrorType().getMessage();
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
            } else if (loginResult.wasCancelled()) {
                toastMessage = "Login Cancelled";
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
            } else {
                // Login Success
                dialog.show();

                AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                    @Override
                    public void onSuccess(Account account) {
                        compositeDisposable.add(myRestaurantAPI.getUser(Common.API_KEY, account.getId())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        userModel -> {
                                            if (userModel.isSuccess()) {
                                                // If user available in database
                                                Common.currentUser = userModel.getResult().get(0);
                                                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                                                finish();
                                            } else {
                                                // If user not available in database, start UpdateInfoActivity for register
                                                startActivity(new Intent(MainActivity.this, UpdateInfoActivity.class));
                                                finish();
                                            }

                                            dialog.dismiss();
                                        },
                                        throwable -> {
                                            dialog.dismiss();

                                            Toast.makeText(MainActivity.this, "[GET USER]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                )
                        );
                    }


                    @Override
                    public void onError(AccountKitError accountKitError) {
                        Toast.makeText(MainActivity.this, "[ACCOUNT KIT ERROR]: " + accountKitError.getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                Toast.makeText(MainActivity.this, "Success Login", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        initialize();
    }

    private void initialize() {
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(IMyRestaurantAPI.class);
    }
}
