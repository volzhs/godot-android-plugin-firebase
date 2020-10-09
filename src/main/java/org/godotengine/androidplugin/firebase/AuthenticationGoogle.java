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

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthProvider;

import org.godotengine.godot.Godot;
import org.json.JSONException;
import org.json.JSONObject;

public class AuthenticationGoogle {

    private static final int RC_SIGN_IN = 9001;

    private Activity activity = null;
    private static AuthenticationGoogle instance = null;
    private static GoogleSignInClient googleSignInClient = null;
    private final Godot godot;
    private FirebaseAuth auth;
    private JSONObject currentGoogleUser = new JSONObject();
    private boolean isGooglePlayConnected = false;

    public AuthenticationGoogle(Godot godot)
    {
        this.godot = godot;
        this.activity = godot.getActivity();
    }

    public static AuthenticationGoogle getInstance(Godot godot) {
        if (instance == null) {
            instance = new AuthenticationGoogle(godot);
        }

        return instance;
    }

    public void init() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(activity, gso);
//        googleSignInClient.silentSignIn();

        Utils.logDebug("AuthenticationGoogle initialized");

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            signIn();
        }
    }

    public void signIn() {
        if (googleSignInClient == null) {
            return;
        }

        Intent singInIntent = googleSignInClient.getSignInIntent();
        godot.startActivityForResult(singInIntent, RC_SIGN_IN);
    }

    public void signOut() {
        auth.signOut();

        googleSignInClient.signOut().addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                successSignOut();
            }
        });
    }

    public String getUserDetails() {
        return currentGoogleUser.toString();
    }

    public boolean isConnected() {
        return isGooglePlayConnected;
    }

    private void successSignIn(FirebaseUser user) {
        isGooglePlayConnected = true;

        try {
            currentGoogleUser.put("uid", user.getUid());
            currentGoogleUser.put("name", user.getDisplayName());
            currentGoogleUser.put("email", user.getEmail());
            currentGoogleUser.put("photo_uri", user.getPhotoUrl());

            Task<GetTokenResult> result = user.getIdToken(false);
            result.addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                @Override
                public void onSuccess(GetTokenResult getTokenResult) {
                    String token = getTokenResult.getToken();
                    String provider = getTokenResult.getSignInProvider();
                    try {
                        currentGoogleUser.put("token", token);
                        currentGoogleUser.put("provider", provider);
                    } catch (JSONException e) {
                        Utils.logDebug("AuthenticationGoogle successSignIn() JSONException " + e.toString());
                    }
                    Utils.callScriptFunc("Authentication", "Google", "true");
                }
            });
        } catch (JSONException e) {
            Utils.logDebug("AuthenticationGoogle successSignIn() JSONException " + e.toString());
        }

//        Utils.callScriptFunc("Authentication", "Google", "true");
    }

    private void successSignOut() {
        isGooglePlayConnected = false;
        currentGoogleUser = null;
        currentGoogleUser = new JSONObject();

        Utils.callScriptFunc("Authentication", "Google", "false");
    }

    private void firebaseAuthenticationWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential).addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    successSignIn(auth.getCurrentUser());
                } else {
                    // Left empty for now.
                }

            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent()
        if (requestCode == RC_SIGN_IN && resultCode == Activity.RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthenticationWithGoogle(account);
            } catch (ApiException e) {
                Utils.logWarn("AuthenticationGoogle onActivityResult(), Sign in failed. " + e.toString());
            }
        } else {
            Utils.callScriptFunc("Authentication", "Google", "other");
        }
    }

    public void onStop() {
        isGooglePlayConnected = false;
        activity = null;
    }
}