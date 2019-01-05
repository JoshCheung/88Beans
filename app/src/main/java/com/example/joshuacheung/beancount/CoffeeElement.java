package com.example.joshuacheung.beancount;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class CoffeeElement implements Parcelable{
    private String name;
    private String date;
    private double weight;
    private int cupsSold;

    public CoffeeElement(String date, int cupsSold, double weight, String name) {
        this.date = date;
        this.cupsSold = cupsSold;
        this.weight = weight;
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public int getCupsSold() { return cupsSold; }

    public double getWeight() { return weight; }

    public String getName() {
        return name;
    }

    protected CoffeeElement(Parcel in) {
        date = in.readString();
        cupsSold = in.readInt();
        weight = in.readDouble();
        name = in.readString();
    }

    public static Comparator<CoffeeElement> DateComparison = new Comparator<CoffeeElement>() {

        public int compare(CoffeeElement d1, CoffeeElement d2) {
            SimpleDateFormat format = new SimpleDateFormat("EEEE, MM-dd-yyyy");
            Date date1 = null;
            Date date2 = null;
            try {
                date1 = format.parse(d1.getDate());
                date2 = format.parse(d2.getDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
//          descending order
            return date2.compareTo(date1);
        }
    };

    public String toString() {
        return getName() + ", " + getDate() + ", " + getCupsSold() + ", " + getWeight();
    }

    public static final Creator<CoffeeElement> CREATOR = new Creator<CoffeeElement>() {
        @Override
        public CoffeeElement createFromParcel(Parcel in) {
            return new CoffeeElement(in);
        }

        @Override
        public CoffeeElement[] newArray(int size) {
            return new CoffeeElement[size];
        }
    };
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(date);
        parcel.writeInt(cupsSold);
        parcel.writeDouble(weight);
        parcel.writeString(name);
    }
}