package com.bbexcellence.travelmantics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DealActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    private EditText textTitle;
    private EditText textPrice;
    private EditText textDescription;
    private TravelDeal mCurrentDeal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert);

        FirebaseUtil.openFrbReference("traveldeals", this);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;

        textTitle = findViewById(R.id.text_title);
        textPrice = findViewById(R.id.text_price);
        textDescription = findViewById(R.id.text_description);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");

        if (deal == null) {
            deal = new TravelDeal();
        }
        this.mCurrentDeal = deal;
        updateUI();
    }

    private void updateUI() {
        textTitle.setText(mCurrentDeal.getTitle());
        textDescription.setText(mCurrentDeal.getDescription());
        textPrice.setText(mCurrentDeal.getPrice());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save_menu, menu);

        //Deactivating delete and save options for non admins
        boolean adminCheck = FirebaseUtil.isAdmin;
        menu.findItem(R.id.delete_menu).setVisible(adminCheck);
        menu.findItem(R.id.save_menu).setVisible(adminCheck);
        enableEditTexts(adminCheck);

        return true;
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
}
