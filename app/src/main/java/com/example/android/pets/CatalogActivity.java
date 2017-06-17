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

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.pets.data.PetContract;
import com.example.android.pets.data.PetDbHelper;
import com.example.android.pets.data.PetContract.PetEntry;

import java.util.Random;


/**
 * Displays list of pets that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PETS = 100;
    private static final int PET_ID = 101;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    public static final String LOG_TAG = CatalogActivity.class.getSimpleName();

    public static final int URL_LOADER=0;//identifies a specific loader used in this component

    public PetCursorAdapter mPetCursorAdapter;
    public ListView mPetListView;

    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);

            }
        });
        //Find the listview
        mPetListView = (ListView) findViewById(R.id.list_view_pet);

        //find empty view for our listview
        View emptyView=(View)findViewById(R.id.empty_view);
        mPetListView.setEmptyView(emptyView);

        //Set up an adapter to create a list item for each row of pet data in the Cursor.
        //There is no pet data yet (until the loader finishes). So we set its data to null for now.
        mPetCursorAdapter= new PetCursorAdapter(this,null);
        mPetListView.setAdapter(mPetCursorAdapter);

        mPetListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(CatalogActivity.this, EditorActivity.class);
                //create Uri to send to EditorActivity
                Uri singleUri = ContentUris.withAppendedId(PetEntry.CONTENT_URI, id);
                intent.setData(singleUri);
                //set to edit mode
                startActivity(intent);
            }
        });

        //Kick off the loader

        getLoaderManager().initLoader(URL_LOADER,null,this);//initialize loader





    }





    private void insertPet() {
        PetDbHelper petDbHelper = new PetDbHelper(this);
        SQLiteDatabase db = petDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(PetEntry.COLUMN_PET_NAME, "Cute " + getSaltString());
        values.put(PetEntry.COLUMN_PET_BREED, "Popular " + getSaltString());
        values.put(PetEntry.COLUMN_PET_WEIGHT, 11);
        values.put(PetEntry.COLUMN_PET_GENDER, PetEntry.GENDER_MALE);

        Uri uri = getContentResolver().insert(PetEntry.CONTENT_URI, values);

        switch (sUriMatcher.match(uri)) {
            case PET_ID:
                Toast.makeText(getApplicationContext(), R.string.successful_insertion_msg, Toast.LENGTH_LONG).show();
                return;
            default:
                Toast.makeText(getApplicationContext(), R.string.insert_failed_message, Toast.LENGTH_LONG).show();

        }


    }
    protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 5) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertPet();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                //Add confirmation
                AlertDialog.Builder builder= new AlertDialog.Builder(this);
                builder.setMessage(R.string.delete_all_pets);
                builder.setTitle(R.string.warning);
                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //if user confirmed delete, then do it!
                        deleteAllPets();
                    }
                });
                builder.setNegativeButton(R.string.cancel,null);
                AlertDialog alert=builder.create();
                alert.show();
                //Delete all pets

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAllPets() {
        int rowsDeleted=getContentResolver().delete(PetEntry.CONTENT_URI,null,null);
        if(rowsDeleted>0){
            Toast.makeText(getApplicationContext(),R.string.all_pets_deleted_successfully,Toast.LENGTH_SHORT).show();
        }
        else { //show failure
            Toast.makeText(getApplicationContext(),R.string.pets_delete_failed,Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Create a projection for CursorLoader
        String [] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                };

        //Take action based on the ID of the Loader that is being created.
        switch(id){
            case URL_LOADER:
                //return a new CursorLoader
                return new CursorLoader(
                        getApplicationContext(),
                        PetEntry.CONTENT_URI,
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

        mPetCursorAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        mPetCursorAdapter.swapCursor(null);
    }


}
