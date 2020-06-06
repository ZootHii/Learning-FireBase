package com.zoothii.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {


    public static final int SELECT_IMAGE_REQUEST_CODE = 111;
    public static final int SELECT_IMAGE_INTENT_REQUEST_CODE = 222;
    public static final int TAKE_PHOTO_REQUEST_CODE = 333;
    public static final int TAKE_PHOTO_INTENT_REQUEST_CODE = 444;

    EditText commentText;
    TextView infoText;
    ImageView imageView;
    Uri imageUri;
    String currentPhotoPath;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        commentText = findViewById(R.id.commentText);
        imageView = findViewById(R.id.imageView);
        infoText = findViewById(R.id.infoText);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                takePhoto();
                return false;
            }
        });
        
    }


    // çekilen fotoğrafı çekilen tarihteki hafızadan alıp imagefile yapıyoruz
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(/* prefix */ imageFileName,/* suffix */".jpg",/* directory */storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // çekilen fotoğrafı imageview a koyma
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                System.out.println(ex);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //file to uri // manifest içerisinde authority verilmesi gerekiyor file işlemleri için
                Uri photoURI = FileProvider.getUriForFile(this, "com.zoothii.firebase.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, TAKE_PHOTO_INTENT_REQUEST_CODE);
            }
        }
    }
    // çekilen fotoğrafı imageview a koyma
    public void takePhoto() {
        if (ContextCompat.checkSelfPermission(UploadActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UploadActivity.this, new String[]{Manifest.permission.CAMERA}, TAKE_PHOTO_REQUEST_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    public void selectImage() {
        if (ContextCompat.checkSelfPermission(UploadActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UploadActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, SELECT_IMAGE_REQUEST_CODE);
        } else {
            Intent selectImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(selectImageIntent, SELECT_IMAGE_INTENT_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SELECT_IMAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, SELECT_IMAGE_INTENT_REQUEST_CODE);
            }
        } else if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE_INTENT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            infoText.setText("");
            imageView.setImageURI(imageUri);

        } else if (requestCode == TAKE_PHOTO_INTENT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            File file = new File(currentPhotoPath);
            infoText.setText("");
            imageView.setImageURI(Uri.fromFile(file)); //file olan fotoğrafı uri yapmamız gerekiyor data base eklemek için
        }
    }

    public void saveData(View view) {
        UUID uuid = UUID.randomUUID(); //universel unique id
        final String imageName = "images/" + uuid + ".jpg";

        if (imageUri != null) {
            storageReference.child(imageName).putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Download URL
                    StorageReference downloadReferance = firebaseStorage.getReference(imageName);
                    downloadReferance.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            String downloadUrl = uri.toString();
                            System.out.println(downloadUrl);
                            String email = firebaseUser.getEmail();
                            String comment = commentText.getText().toString();
                            HashMap<String, Object> postData = new HashMap<>();
                            postData.put("email", email);
                            postData.put("downloadurl", downloadUrl);
                            postData.put("comment", comment);
                            postData.put("date", FieldValue.serverTimestamp());
                            firebaseFirestore.collection("Posts").add(postData).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                }
                            });

                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
            finish();
        } else if (currentPhotoPath != null) {
            File file = new File(currentPhotoPath);
            imageUri = Uri.fromFile(file);
            storageReference.child(imageName).putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    StorageReference downloadReferance = firebaseStorage.getReference(imageName);
                    downloadReferance.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            String downloadUrl = uri.toString();
                            System.out.println(downloadUrl);
                            String email = firebaseUser.getEmail();
                            String comment = commentText.getText().toString();
                            HashMap<String, Object> postData = new HashMap<>();
                            postData.put("email", email);
                            postData.put("downloadurl", downloadUrl);
                            postData.put("comment", comment);
                            postData.put("date", FieldValue.serverTimestamp());
                            firebaseFirestore.collection("Posts").add(postData).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(UploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
            finish();
        } else {
            Toast.makeText(UploadActivity.this, "Please select an image or take a photo", Toast.LENGTH_LONG).show();
        }

    }
}