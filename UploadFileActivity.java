package com.example.storyapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;


public class UploadFileActivity extends AppCompatActivity {
    StorageReference storageRef;
    DatabaseReference databaseRef;
    EditText filenameText, typeText;
    Button uploadButton, backButton;
    RadioGroup filetype;
    int typeID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_file);

        filenameText = findViewById(R.id.fileText);
        uploadButton = findViewById(R.id.uploadButton);
        backButton=findViewById(R.id.back);
        typeText=findViewById(R.id.typelabel);
        filetype=findViewById(R.id.typegroup);
        typeID = filetype.getCheckedRadioButtonId();
        storageRef = FirebaseStorage.getInstance().getReference();
        databaseRef = FirebaseDatabase.getInstance().getReference("Uploads");

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UploadFileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFile();
            }
        });

    }

    private void selectFile() {
        typeID = filetype.getCheckedRadioButtonId();
        Intent intent = new Intent();
        switch (typeID){
            case R.id.typePDF:
                intent.setType("application/pdf");
                break;
            case R.id.typeRTF:
                intent.setType("text/rtf");
                break;
            case R.id.typeTXT:
            default:
                intent.setType("text/plain");
                break;
        }
        intent.setAction(intent.ACTION_GET_CONTENT);
    //    Intent chooserIntent = Intent.createChooser(intent, "choosefile");
  //      getResult.launch(chooserIntent);
        startActivityForResult(Intent.createChooser(intent, "choosefile"), 12);
    }


  /*  ActivityResultLauncher<Intent> getResult=registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        uploadButton.setEnabled(true);
                        text.setText(result.getData().getDataString().substring(result.getData().getDataString().lastIndexOf("/")+1));
                        
                        uploadButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                uploadPDFFirebase(result.getData().getData());
                            }
                        });
                    }
                }
            });
   */
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==12 && resultCode == RESULT_OK && data != null && data.getData()!= null){
            filenameText.setText(data.getDataString().substring(data.getDataString().lastIndexOf("/")+1));

            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    uploadFileFirebase(data.getData());
                }
            });
        }
    }

    private void uploadFileFirebase(Uri data) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("File loading...");
        progressDialog.show();
        StorageReference ref;
        switch (typeID){
            case R.id.typePDF:
                ref = storageRef.child("upload"+System.currentTimeMillis()+".pdf");
                break;
            case R.id.typeRTF:
                ref = storageRef.child("upload"+System.currentTimeMillis()+".rtf");
                break;
            case R.id.typeTXT:
            default:
                ref = storageRef.child("upload"+System.currentTimeMillis()+".txt");
                break;
        }

        ref.putFile(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while(!uriTask.isComplete());//wait
                Uri uri = uriTask.getResult();

                String currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                PutFileInStorage putFile = new PutFileInStorage(filenameText.getText().toString(), uri.toString(), currentUID);
                databaseRef.child(databaseRef.push().getKey()).setValue(putFile);
                DatabaseReference userDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
                userDatabase.child(currentUID).child("stories").child(filenameText.getText().toString()).setValue(uri.toString());
                Toast.makeText(UploadFileActivity.this, "File Uploaded", Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress=(100.0*snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                progressDialog.setMessage("Uploaded "+(int)progress+"%");
            }
        });
    }
}