package com.example.adictic.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Oficina implements Parcelable {
    public Long id;
    public String name;
    public Double latitude;
    public Double longitude;
    public String address;
    public String ciutat;
    public String description;
    public String telf;
    public String website;

    protected Oficina(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        name = in.readString();
        if (in.readByte() == 0) {
            latitude = null;
        } else {
            latitude = in.readDouble();
        }
        if (in.readByte() == 0) {
            longitude = null;
        } else {
            longitude = in.readDouble();
        }
        address = in.readString();
        ciutat = in.readString();
        description = in.readString();
        telf = in.readString();
        website = in.readString();
    }

    public static final Creator<Oficina> CREATOR = new Creator<Oficina>() {
        @Override
        public Oficina createFromParcel(Parcel in) {
            return new Oficina(in);
        }

        @Override
        public Oficina[] newArray(int size) {
            return new Oficina[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return name + " (" + ciutat.toUpperCase() + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (id == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeLong(id);
        }
        parcel.writeString(name);
        if (latitude == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeDouble(latitude);
        }
        if (longitude == null) {
            parcel.writeByte((byte) 0);
        } else {
            parcel.writeByte((byte) 1);
            parcel.writeDouble(longitude);
        }
        parcel.writeString(address);
        parcel.writeString(ciutat);
        parcel.writeString(description);
        parcel.writeString(telf);
        parcel.writeString(website);
    }
}
