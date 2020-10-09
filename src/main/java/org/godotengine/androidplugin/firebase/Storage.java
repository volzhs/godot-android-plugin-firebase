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
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.godotengine.godot.Godot;
import org.godotengine.godot.GodotIO;

import java.io.File;

public class Storage {

    private Activity activity = null;
    private static Storage instance = null;
    private static FirebaseApp firebaseApp = null;
    private static FirebaseStorage firebaseStorage = null;
    private final Godot godot;
    private UploadTask uploadTask = null;
    private GodotIO godotIo = null;

    public Storage(Godot godot)
    {
        this.godot = godot;
        this.activity = godot.getActivity();
    }

    public static Storage getInstance(Godot godot) {
        if (instance == null) {
            instance = new Storage(godot);
        }

        return instance;
    }

    public void init(FirebaseApp firebaseApp, Godot godot) {
        this.firebaseApp = firebaseApp;
        firebaseStorage = FirebaseStorage.getInstance();
//        godotIo = new GodotIO((Godot) activity);
        godotIo = godot.io;

        Utils.logDebug("Storage initialized");
    }

    public void upload(String fileName) {
        if (!isInitialized() || Authentication.getInstance(godot).getCurrentUser() == null) {
            Utils.callScriptFunc("Storage", "upload", "userNotSignedIn");
            return;
        }

        StorageReference storageRef = firebaseStorage.getReference();

        if (fileName.startsWith("user://")) {
            fileName = fileName.replaceFirst("user://", "");
        }

        fileName = godotIo.getDataDir() + "/" + fileName;
        Uri file = Uri.fromFile(new File(fileName));
        StorageReference fileRef = storageRef.child(file.getLastPathSegment());
        uploadTask = fileRef.putFile(file);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Utils.callScriptFunc("Storage", "upload", false);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Utils.callScriptFunc("Storage", "upload", true);
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }

    public void download(String fileName) {
        if (!isInitialized() || Authentication.getInstance(godot).getCurrentUser() == null) {
            Utils.callScriptFunc("Storage", "download", "userNotSignedIn");
            return;
        }

        fileName = godotIo.getDataDir() + "/" + fileName;
        final String deviceFileName = fileName;
        Uri file = Uri.fromFile(new File(fileName));

        StorageReference ref = firebaseStorage.getReference().child(file.getLastPathSegment());
        File localFile = new File(fileName);
        ref.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                //On success, data is the absolute path of the downloaded file
                Utils.callScriptFunc("Storage", "download", deviceFileName);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Utils.callScriptFunc("Storage", "download", false);
            }
        });
    }

    private boolean isInitialized() {
        if (firebaseApp == null) {
            return false;
        }
        return true;
    }
}