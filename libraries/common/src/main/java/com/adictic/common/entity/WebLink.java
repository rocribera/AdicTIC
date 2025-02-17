package com.adictic.common.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class WebLink implements Parcelable {
    public static final Creator<WebLink> CREATOR = new Creator<WebLink>() {
        @Override
        public WebLink createFromParcel(Parcel in) {
            return new WebLink(in);
        }

        @Override
        public WebLink[] newArray(int size) {
            return new WebLink[size];
        }
    };
    public String name;
    public String url;

    public WebLink(){ }

    protected WebLink(Parcel in) {
        name = in.readString();
        url = in.readString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        WebLink webLink = (WebLink) obj;
        assert webLink != null;
        return this.name.equals(webLink.name) && this.url.equals(webLink.url);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(url);
    }
}