/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;


/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int EXISTING_PET_LOADER = 0; //identifies a specific loader used in this component



    /**
     * EditText field to enter the pet's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the pet's breed
     */
    private EditText mBreedEditText;

    /**
     * EditText field to enter the pet's weight
     */
    private EditText mWeightEditText;

//    PetDbHelper mDbHelper = new PetDbHelper(this);//db helper object

    /**
     * EditText field to enter the pet's gender
     */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible valid values are in the PetContract.java file:
     * {@link PetEntry#GENDER_UNKNOWN}, {@link PetEntry#GENDER_MALE}, or
     * {@link PetEntry#GENDER_FEMALE}.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;

    public String LOG_TAG = EditorActivity.class.getSimpleName();
    private Uri mCurrentPetUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        //Set onTouchListeners
        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();

        //get intent from CatalogActivity
        Intent intent = getIntent();
        mCurrentPetUri = intent.getData();
        //Log received uri
        Activity activity = EditorActivity.this;

        if (mCurrentPetUri == null) {
            activity.setTitle(R.string.editor_activity_title_new_pet);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        }
        else
        {
            activity.setTitle(R.string.editor_activity_title_edit_pet);
            //Kick off loader
            getLoaderManager().initLoader(EXISTING_PET_LOADER,null,this);

        }

    }


    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE;
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE;
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                if(!controlInputs())
                {
                    Toast.makeText(getApplicationContext(),R.string.input_error_msg,Toast.LENGTH_SHORT).show();
                    break;
                }
                    savePet();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Check see if we are in edit mode
                if(mCurrentPetUri!=null) { //we are in edit mode
                    showDeleteConfirmationDialog();
                }
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:

                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private boolean controlInputs() {
        String petName=mNameEditText.getText().toString();
        String petWeight=mWeightEditText.getText().toString();
        String petBreed=mBreedEditText.getText().toString();
        
        if (TextUtils.isEmpty(petName) ||
                mGenderSpinner.getSelectedItemPosition() == PetEntry.GENDER_UNKNOWN
                ) return false;
         if(TextUtils.isEmpty(petWeight)) { //weight cannot be null
             mWeightEditText.setText("0");
         }
         return true;
    }

    private void savePet() {
        //read input values
        ContentValues values = new ContentValues();//create contentvalues
        values.put(PetEntry.COLUMN_PET_NAME, mNameEditText.getText().toString());
        values.put(PetEntry.COLUMN_PET_BREED, mBreedEditText.getText().toString());
        values.put(PetEntry.COLUMN_PET_WEIGHT, Integer.parseInt(mWeightEditText.getText().toString()));
        values.put(PetEntry.COLUMN_PET_GENDER, mGenderSpinner.getSelectedItemPosition());

        //Determine save mode i.e. check whether it is insert mode or edit mode
        if(mCurrentPetUri==null) // insert mode
        {
            Toast.makeText(getApplicationContext(),R.string.successful_insertion_msg,Toast.LENGTH_LONG).show();
            getContentResolver().insert(PetEntry.CONTENT_URI, values);

        }
        else { //uri is not null so an item has been passed for edit, thus edit mode
            int rowsAffected=getContentResolver().update(mCurrentPetUri, values, null, null);
            if(rowsAffected != 0){
                Toast.makeText(getApplicationContext(),R.string.edit_pet_success_msg, Toast.LENGTH_LONG).show();

            }
            else {
                Toast.makeText(getApplicationContext(), R.string.edit_pet_failure_msg, Toast.LENGTH_LONG).show();

            }
        }

    }


    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {



        //Create a projection for CursorLoader
        String[] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_WEIGHT,
                PetEntry.COLUMN_PET_GENDER
        };




        //Take action based on the ID of the Loader that is being created.
        switch (id) {
            case EXISTING_PET_LOADER:
                //return a new CursorLoader
                return new CursorLoader(
                        getApplicationContext(),
                        mCurrentPetUri,
                        projection,
                        null,
                        null,
                        null
                );
            default:
                //invalid id was passed
                return null;
        }


    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {

        data.moveToFirst();

        //Find indexes for data
        int nameIndex = data.getColumnIndexOrThrow(PetEntry.COLUMN_PET_NAME);
        int breedIndex = data.getColumnIndexOrThrow(PetEntry.COLUMN_PET_BREED);
        int weightIndex = data.getColumnIndexOrThrow(PetEntry.COLUMN_PET_WEIGHT);
        int genderIndex = data.getColumnIndexOrThrow(PetEntry.COLUMN_PET_GENDER);

        String petName = data.getString(nameIndex);
        String petBreed = data.getString(breedIndex);
        int petWeight = data.getInt(weightIndex);
        int petGender = data.getInt(genderIndex);

        //Fill in fields data
        mNameEditText.setText(petName);
        mBreedEditText.setText(petBreed);
        mWeightEditText.setText(String.valueOf(petWeight));
        mGenderSpinner.setSelection(petGender);

    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        //reset fields
        mNameEditText.setText(null);
        mBreedEditText.setText(null);
        mWeightEditText.setText(null);
        mGenderSpinner.setSelection(PetEntry.GENDER_UNKNOWN);

    }

    //Create and onTouchListener
    private boolean mPetHasChanged;
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mPetHasChanged = true;
            return false;
        }
    };

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mPetHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    /**
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        //Delete the pet with the Uri of mCurrentPetUri
        int rowsDeleted=getContentResolver().delete(mCurrentPetUri, null, null);
        if(rowsDeleted>0) Toast.makeText(getApplicationContext(),R.string.editor_delete_pet_successful,Toast.LENGTH_SHORT).show();
        else Toast.makeText(getApplicationContext(),R.string.editor_delete_pet_failed,Toast.LENGTH_SHORT).show();
    }


}