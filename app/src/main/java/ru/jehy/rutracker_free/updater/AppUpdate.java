package ru.jehy.rutracker_free.updater;


import android.os.Parcel;
import android.os.Parcelable;

/*
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
*/
public class AppUpdate implements Parcelable {

    public static final int UPDATE_AVAILABLE = 1234;
    public static final int UP_TO_DATE = 1235;
    public static final int ERROR = 1236;
    public static final Parcelable.Creator<AppUpdate> CREATOR = new Parcelable.Creator<AppUpdate>() {
        public AppUpdate createFromParcel(Parcel in) {
            return new AppUpdate(in);
        }

        public AppUpdate[] newArray(int size) {
            return new AppUpdate[size];
        }
    };
    private String assetUrl;
    private String version;
    private String changelog;
    private int status;

    public AppUpdate(Parcel in) {
        assetUrl = in.readString();
        version = in.readString();
        changelog = in.readString();
        status = in.readInt();
    }

    public AppUpdate(String url, String version, String changelog, int status) {
        this.assetUrl = url;
        this.version = version;
        this.changelog = changelog;
        this.status = status;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(assetUrl);
        out.writeString(version);
        out.writeString(changelog);
        out.writeInt(status);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getAssetUrl() {
        return assetUrl;
    }
}
