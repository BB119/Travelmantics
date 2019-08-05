package com.bbexcellence.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {

    private static final int PICTURE_RESULT = 42;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    private EditText textTitle;
    private EditText textPrice;
    private EditText textDescription;
    private ImageView mImageView;
    private TravelDeal mCurrentDeal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);

        FirebaseUtil.openFrbReference("traveldeals", this);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;

        textTitle = findViewById(R.id.text_title);
        textPrice = findViewById(R.id.text_price);
        textDescription = findViewById(R.id.text_description);
        mImageView = findViewById(R.id.image);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");

        if (deal == null) {
            deal = new TravelDeal();
        }
        this.mCurrentDeal = deal;
        updateUI();

        Button imageButton = findViewById(R.id.image_button);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "Insert Picture"), PICTURE_RESULT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    Task<Uri> uri = taskSnapshot.getStorage().getDownloadUrl();
                    while(!uri.isComplete());
                    String url = uri.getResult().toString();
                    String pictureName = taskSnapshot.getStorage().getPath();
                    mCurrentDeal.setImageUrl(url);
                    mCurrentDeal.setImageName(pictureName);
                    Log.d("Url: ", url);
                    Log.d("Name: ", pictureName);
                    showImage(url);
                }
            });
        }
    }

    private void updateUI() {
        textTitle.setText(mCurrentDeal.getTitle());
        textDescription.setText(mCurrentDeal.getDescription());
        textPrice.setText(mCurrentDeal.getPrice());
        showImage(mCurrentDeal.getImageUrl());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Deactivating delete and save options for non admins
        boolean adminCheck = FirebaseUtil.isAdmin;
        menu.findItem(R.id.delete_menu).setVisible(adminCheck);
        menu.findItem(R.id.save_menu).setVisible(adminCheck);
        enableEditTexts(adminCheck);
        findViewById(R.id.image_button).setEnabled(adminCheck);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal saved..", Toast.LENGTH_LONG).show();
                cleanUI();
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal deleted", Toast.LENGTH_LONG).show();
                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void cleanUI() {
        textTitle.setText("");
        textPrice.setText("");
        textDescription.setText("");

        textTitle.requestFocus();
    }

    private void saveDeal() {
        mCurrentDeal.setTitle(textTitle.getText().toString());
        mCurrentDeal.setPrice(textPrice.getText().toString());
        mCurrentDeal.setDescription(textDescription.getText().toString());

        if (mCurrentDeal.getId() == null) {
            mDatabaseReference.push().setValue(mCurrentDeal);
        } else {
            mDatabaseReference.child(mCurrentDeal.getId()).setValue(mCurrentDeal);
        }
    }

    private void deleteDeal() {
        if (mCurrentDeal.getId() == null) {
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(mCurrentDeal.getId()).removeValue();

        String pictureName = mCurrentDeal.getImageName();
        if (pictureName != null && !pictureName.isEmpty()) {
            StorageReference picRef = FirebaseUtil.mStorage.getReference().child(mCurrentDeal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete Image", "Image Successfully Deleted");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", e.getMessage());
                }
            });
        }
    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    private void enableEditTexts(boolean isEnabled) {
        textTitle.setEnabled(isEnabled);
        textPrice.setEnabled(isEnabled);
        textDescription.setEnabled(isEnabled);
    }

    private void showImage(String url) {
        if (url != null && !url.isEmpty()) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels - 32;
            Picasso.get()
                    .load(url)
                    .resize(width, width * 2/3)
                    .centerCrop()
                    .into(mImageView);
        }
    }
}
