package com.example.joshuacheung.beancount;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class DataListing extends AppCompatActivity {

    public static final String TAG = "DATA_LISTING";
    DatabaseHelper mDatabasehelper;
    ArrayAdapter<CoffeeElement> adapter = null;
    ArrayList<CoffeeElement> dataList = new ArrayList<>();
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    List<String> expandableListTitle;
    SortedMap<String, List<CoffeeElement>> expandableListDetail;
    public static String title = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDatabasehelper = new DatabaseHelper(this);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        title = intent.getStringExtra("type");
        if (!title.equals("88 Beans")) {
            setContentView(R.layout.data_listing);
            adapter = new DataListing.ListAdapter();
            displayData(title);
        }
        else {
            setContentView(R.layout.expandable_listview);

            expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
            expandableListDetail = getData();
            expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
            expandableListAdapter = new CustomExpandableListAdapter(this, expandableListTitle, expandableListDetail);
            expandableListView.setAdapter(expandableListAdapter);
            expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

                @Override
                public void onGroupExpand(int groupPosition) {
                    Toast.makeText(getApplicationContext(),
                            expandableListTitle.get(groupPosition) + " List Expanded.",
                            Toast.LENGTH_SHORT).show();
                }
            });

            expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

                @Override
                public void onGroupCollapse(int groupPosition) {
                    Toast.makeText(getApplicationContext(),
                            expandableListTitle.get(groupPosition) + " List Collapsed.",
                            Toast.LENGTH_SHORT).show();

                }
            });

            expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v,
                                            int groupPosition, int childPosition, long id) {

                    final Dialog dialog = new Dialog(DataListing.this);
                    dialog.setContentView(R.layout.edit_or_delete_instance);
                    dialog.show();
//                    Toast.makeText(
//                            getApplicationContext(),
//                            expandableListTitle.get(groupPosition)
//                                    + " -> "
//                                    + expandableListDetail.get(
//                                    expandableListTitle.get(groupPosition)).get(
//                                    childPosition), Toast.LENGTH_SHORT
//                    ).show();
                    return false;
                }
            });
        }

    }

    public SortedMap<String, List<CoffeeElement>> getData() {
        SortedMap<String, List<CoffeeElement>> expandableListDetail = new TreeMap<>(Collections.reverseOrder());
        List<CoffeeElement> days = new ArrayList<>();
        Cursor data = mDatabasehelper.getData();
        if (data.getCount() == 0) {
            return null;
        }

        while (data.moveToNext()) {
            String preDate = data.getString(1);
            String convertDate = preDate.substring(preDate
                    .lastIndexOf(" ")).replace(" ", "");
            String [] convert = convertDate.split("-");
            String date = convert[2] + "-" + convert[0] + "-" + convert[1];

            if (!expandableListDetail.containsKey(date)) {
                expandableListDetail.put(date, new ArrayList<>());
                expandableListDetail.get(date).add(new CoffeeElement(data.getString(1), Integer.parseInt(data.getString(2)),
                        Double.parseDouble(data.getString(3)), data.getString(4)));
            }
            else {
                expandableListDetail.get(date).add(new CoffeeElement(data.getString(1), Integer.parseInt(data.getString(2)),
                        Double.parseDouble(data.getString(3)), data.getString(4)));
            }
        }
        return expandableListDetail;
    }


    private class ListAdapter extends ArrayAdapter<CoffeeElement> {
        public ListAdapter() {
            super(DataListing.this, R.layout.coffee_element, R.id.dataListView, dataList);
        } //Change according to Database
        public View getView(final int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.coffee_element, null);
            }
            //Set Coffee Title
            TextView title = itemView.findViewById(R.id.coffeeTitle);
            TextView date = itemView.findViewById(R.id.dateOutput);
            TextView cups = itemView.findViewById(R.id.cups_served);
            TextView weight = itemView.findViewById(R.id.weight_output);
            title.setText(dataList.get(position).getName());
            date.setText(dataList.get(position).getDate());
            cups.setText(dataList.get(position).getCupsSold() + "");
            weight.setText(dataList.get(position).getWeight() + "");

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Vibrator vibe = (Vibrator) DataListing.this.getSystemService(Context.VIBRATOR_SERVICE) ;
                    vibe.vibrate(80);
                    final Dialog dialog = new Dialog(DataListing.this);
                    dialog.setContentView(R.layout.edit_or_delete_instance);
                    Button update = dialog.findViewById(R.id.update);
                    Button delete = dialog.findViewById(R.id.delete);
                    EditText coffeeText = dialog.findViewById(R.id.typeOfCoffeeText);
                    EditText coffeeFactor = dialog.findViewById(R.id.factor);
                    coffeeText.setText(dataList.get(position).getName());
                    String oldItem = dataList.get(position).getName();
                    Log.d("OLD ITEM: ", "Item Selected: " + oldItem);
//                    coffeeFactor.setText("");
                    update.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Cursor data = mDatabasehelper.getItemID(oldItem);
                            int itemID = -1;
                            while (data.moveToNext()) {
                                itemID = data.getInt(0);
                            }
                            if (itemID > -1) {
                                Log.d("Item clicked", "onItemCLick: the ID is " + itemID);

                                toastMessage("Item was clicked for updating: " + itemID +", " + dataList.get(position));

                                if (coffeeText != null || coffeeText.getText().toString() == "" && coffeeFactor != null) {
                                    String coffeeTitle = coffeeText.getText().toString();
                                    adapter.notifyDataSetChanged();
                                }
                            } else {
                                toastMessage("No ID associated with that name");
                            }
                            dialog.dismiss();
                        }
                    });
                    delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            toastMessage("Item was clicked: " + dataList.get(position).getName());
                            String name = dataList.get(position).getName();
                            Log.d("ITEM CLICKED", "onItemClicked: you clicked on: " + name);

                            Cursor data = mDatabasehelper.getItemID(name);
                            int itemID = -1;
                            while (data.moveToNext()) {
                                itemID = data.getInt(0);
                            }
                            if (itemID > -1) {
                                Log.d("Item clicked", "onItemCLick: the ID is " + itemID);
                                toastMessage("Item was clicked for deletion: " + itemID +", " + dataList.get(position).getName());
                                mDatabasehelper.deleteName(itemID, name);
                                dataList.remove(position);
                                adapter.notifyDataSetChanged();
                            } else {
                                toastMessage("No ID associated with that name");
                            }
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                    return false;
                }
            });

            return itemView;
        }
    }


    public void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void displayData(String title) {
        TextView listings = findViewById(R.id.dataListTitle);
        listings.setText(title + ": All Entries");
        ListView list = (ListView) findViewById(R.id.dataListView);
        list.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        Cursor data = mDatabasehelper.getData();
        if (data.getCount() == 0) {
            return;
        }
        while (data.moveToNext()) {
            if (data.getString(4).equals(title)) {
                dataList.add(0, new CoffeeElement(data.getString(1), Integer.parseInt(data.getString(2)),
                        Double.parseDouble(data.getString(3)), data.getString(4)));
            }
        }
    }
}
