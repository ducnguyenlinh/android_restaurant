package com.example.android_restaurant;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.android_restaurant.Common.Common;
import com.example.android_restaurant.Retrofit.IMyRestaurantAPI;
import com.example.android_restaurant.Retrofit.RetrofitClient;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SplashScreen extends AppCompatActivity {

    IMyRestaurantAPI myRestaurantAPI;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    AlertDialog dialog;

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        initialize();

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                            @Override
                            public void onSuccess(Account account) {
                                dialog.show();

                                compositeDisposable.add(myRestaurantAPI.getUser(Common.API_KEY, account.getId())
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(
                                                userModel -> {
                                                    if (userModel.isSuccess()) {
                                                        // If user available in database
                                                        Common.currentUser = userModel.getResult().get(0);
                                                        startActivity(new Intent(SplashScreen.this, HomeActivity.class));
                                                        finish();
                                                    } else {
                                                        // If user not available in database, start UpdateInfoActivity for register
                                                        startActivity(new Intent(SplashScreen.this, UpdateInfoActivity.class));
                                                        finish();
                                                    }

                                                    dialog.dismiss();
                                                },
                                                throwable -> {
                                                    dialog.dismiss();

                                                    Toast.makeText(SplashScreen.this, "[GET USER API]: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                })
                                );
                            }

                            @Override
                            public void onError(AccountKitError accountKitError) {
                                Toast.makeText(SplashScreen.this, "Not sign in! Please sign in.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SplashScreen.this, MainActivity.class));
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(SplashScreen.this, "You must accept this permission to use our app", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    private void initialize() {
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();
        myRestaurantAPI = RetrofitClient.getInstance(Common.API_RESTAURANT_ENDPOINT).create(IMyRestaurantAPI.class);
    }
}
