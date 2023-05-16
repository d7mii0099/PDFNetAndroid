package com.pdftron.pdf.widget.toolbar.builder;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

class Id implements Parcelable, Serializable {
    final int id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Id id1 = (Id) o;

        return id == id1.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    Id(int id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
    }

    protected Id(Parcel in) {
        this.id = in.readInt();
    }

    public static final Parcelable.Creator<Id> CREATOR = new Parcelable.Creator<Id>() {
        @Override
        public Id createFromParcel(Parcel source) {
            return new Id(source);
        }

        @Override
        public Id[] newArray(int size) {
            return new Id[size];
        }
    };
}
