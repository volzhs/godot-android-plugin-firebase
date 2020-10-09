/**
 * Copyright 2020 Yalcin Ata. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.godotengine.androidplugin.firebase;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.godotengine.godot.Godot;

public class Authentication {

    private static Activity activity = null;
    private static Authentication instance = null;
    private FirebaseApp firebaseApp = null;
    private Godot godot;

    public Authentication(Godot godot)
    {
        this.godot = godot;
        this.activity = godot.getActivity();
    }

    public static Authentication getInstance(Godot godot) {
        if (instance == null) {
            Log.d("godot", "getInstance: make new");
            instance = new Authentication(godot);
        }

        return instance;
    }

    public void init(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
        AuthenticationGoogle.getInstance(godot).init();

        Utils.logDebug("Authentication initialized");
    }

    public void signIn() {
        if (!isInitialized()) {
            return;
        }

        AuthenticationGoogle.getInstance(godot).signIn();
    }

    public void signOut() {
        if (!isInitialized()) {
            return;
        }

        AuthenticationGoogle.getInstance(godot).signOut();
    }

    public String getUserDetails() {
        if (!isInitialized()) {
            return "null";
        }

        if (AuthenticationGoogle.getInstance(godot).isConnected()) {
            return AuthenticationGoogle.getInstance(godot).getUserDetails();
        }

        return "null";
    }

    public void getIdToken() {
        if (!isInitialized()) {
            Utils.callScriptFunc("Authentication", "idToken", null);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Utils.callScriptFunc("Authentication", "idToken", null);
            return;
        }

        user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    Utils.callScriptFunc("Authentication", "idToken", idToken);
                } else {
                    Utils.callScriptFunc("Authentication", "idToken", null);
                }
            }
        });
    }

    /*
    public void refresh() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || mLastAuthCredential == null) return;

        Task<Void> task = user.reauthenticate(mLastAuthCredential);
        task.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.e("auth", "refresh success");
                    gettingToken();
                } else {
                    Log.e("auth", "refresh fail");
                }
            }
        });
    }
     */

    @Nullable
    public FirebaseUser getCurrentUser() {
        if (!isInitialized()) {
            return null;
        }

        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public boolean isConnected() {

        boolean isConnected = false;

        isConnected = AuthenticationGoogle.getInstance(godot).isConnected();

        return isConnected;
    }

    private boolean isInitialized() {
        if (firebaseApp == null) {
            return false;
        }

        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("godot", "onActivityResult");
        if (!isInitialized()) {
            return;
        }

        AuthenticationGoogle.getInstance(godot).onActivityResult(requestCode, resultCode, data);
    }

    public void onStart() {
        if (!isInitialized()) {
            return;
        }
    }

    public void onPause() {
        // Left empty for now.
    }

    public void onResume() {
        // Left empty for now.
    }

    public void onStop() {
        if (!isInitialized()) {
            return;
        }

        AuthenticationGoogle.getInstance(godot).onStop();
    }
}