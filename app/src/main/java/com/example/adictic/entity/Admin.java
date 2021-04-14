package com.example.adictic.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Admin implements Parcelable {
    public Long idUser;
    public String name;
    public String professio;
    public String description;
    public List<WebLink> webLinks;
    public Long idOficina;

    protected Admin(Parcel in) {
        if (in.readByte() == 0) {
            idUser = null;
        } else {
            idUser = in.readLong();
        }
        name = in.readString();
        professio = in.readString();
        description = in.readString();
        webLinks = in.createTypedArrayList(WebLink.CREATOR);
        if (in.readByte() == 0) {
            idOficina = null;
        } else {
            idOficina = in.readLong();
        }
    }

    public static final Creator<Admin> CREATOR = new Creator<Admin>() {
        @Override
        public Admin createFromParcel(Parcel in) {
            return new Admin(in);
        }

        @Override
        public Admin[] newArray(int size) {
            return new Admin[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(idUser);
        parcel.writeString(name);
        parcel.writeString(professio);
        parcel.writeString(description);
        parcel.writeLong(idOficina);
        parcel.writeList(webLinks);
    }
}
