package com.jonahbauer.qed.model;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.room.Converters;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(of = "id")
@TypeConverters(Converters.class)
public class Image implements Parcelable {
    public static final long NO_ID = Long.MIN_VALUE;
    public static final Set<String> VIDEO_FILE_EXTENSIONS;
    public static final Set<String> AUDIO_FILE_EXTENSIONS;

    public static final String DATA_KEY_ALBUM = "album";
    public static final String DATA_KEY_OWNER = "owner";
    public static final String DATA_KEY_FORMAT = "format";
    public static final String DATA_KEY_UPLOAD_DATE = "uploaded";
    public static final String DATA_KEY_CREATION_DATE = "created";
    public static final String DATA_KEY_ORIENTATION = "orientation";
    public static final String DATA_KEY_MANUFACTURER = "manufacturer";
    public static final String DATA_KEY_MODEL = "model";
    public static final String DATA_KEY_FOCAL_LENGTH = "focal length";
    public static final String DATA_KEY_FOCAL_RATIO = "focal ration";
    public static final String DATA_KEY_EXPOSURE_TIME = "exposure time";
    public static final String DATA_KEY_ISO = "iso";
    public static final String DATA_KEY_POSITION = "position";
    public static final String DATA_KEY_FLASH = "flash";
    public static final String DATA_KEY_VISITS = "visits";
    public static final String DATA_KEY_RESOLUTION = "resolution";

    static {
        VIDEO_FILE_EXTENSIONS = ObjectSet.of("mp4", "mkv", "flv", "mov", "avi", "wmv", "mpeg", "webm");
        AUDIO_FILE_EXTENSIONS = ObjectSet.of("wav", "mp3", "ogg", "m4a", "flac", "opus");
    }

    @PrimaryKey
    private final long id;

    @ColumnInfo(name = "album_id")
    private long albumId;

    @ColumnInfo(name = "order")
    private long order;

    @ColumnInfo(name = "album_name")
    private String albumName;

    @ColumnInfo(name = "format")
    private String format;

    @ColumnInfo(name = "path")
    private String path;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "owner")
    private String owner;

    @ColumnInfo(name = "upload_date")
    private Instant uploadTime;

    @ColumnInfo(name = "creation_date")
    private Instant creationTime;

    @ColumnInfo(name = "original")
    private boolean original;

    @ColumnInfo(name = "thumbnail")
    private Bitmap thumbnail;

    @ColumnInfo(name = "data")
    private Map<String, String> data = new HashMap<>();

    @ColumnInfo(name = "loaded")
    private boolean loaded;

    @Ignore
    private boolean databaseLoaded;

    public void set(Image image) {
        this.setAlbumId(image.getAlbumId());
        this.setAlbumName(image.getAlbumName());
        this.setFormat(image.getFormat());
        this.setPath(image.getPath());
        this.setName(image.getName());
        this.setOwner(image.getOwner());
        this.setUploadTime(image.getUploadTime());
        this.setCreationTime(image.getCreationTime());
        this.setOriginal(image.isOriginal());
        this.setLoaded(image.isLoaded());
        this.setThumbnail(image.getThumbnail());
        this.setData(image.getData());
    }

    @Override
    @NonNull
    public String toString() {
        LinkedList<String> entries = new LinkedList<>();
        if (id != NO_ID) entries.add("\"id\":" + id);
        if (format != null) entries.add("\"format\":\"" + format + "\"");
        if (path != null) entries.add("\"path\":\"" + path + "\"");
        if (name != null) entries.add("\"name\":\"" + name + "\"");
        if (owner != null) entries.add("\"owner\":\"" + owner + "\"");
        if (uploadTime != null) entries.add("\"uploadDate\":" + uploadTime.getEpochSecond());
        if (creationTime != null) entries.add("\"creationDate\":" + creationTime.getEpochSecond());
        entries.add("\"original\":" + original);
        entries.add("\"loaded\":" + loaded);
        if (!data.isEmpty()) entries.add(data.entrySet().stream().map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"").collect(Collectors.joining(",")));
        if (albumName != null) entries.add("\"albumName\":\"" + albumName + "\"");
        if (albumId != Album.NO_ID) entries.add("\"albumId\":" + albumId);
        return entries.stream().collect(Collectors.joining(", ", "{", "}"));
    }

    public Type getType() {
        if (getFormat() != null) {
            String format = getFormat().split("/")[0];
            if ("audio".equals(format)) {
                return Type.AUDIO;
            } else if ("video".equals(format)) {
                return Type.VIDEO;
            } else {
                return Type.IMAGE;
            }
        } else if (getName() != null) {
            String suffix = getName();
            suffix = suffix.substring(suffix.lastIndexOf('.') + 1);

            if (Image.VIDEO_FILE_EXTENSIONS.contains(suffix)) {
                return Type.VIDEO;
            } else if (Image.AUDIO_FILE_EXTENSIONS.contains(suffix)) {
                return Type.AUDIO;
            }
        }

        return Type.IMAGE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(albumId);
        dest.writeLong(order);
        dest.writeString(albumName);
        dest.writeString(format);
        dest.writeString(path);
        dest.writeString(name);
        dest.writeString(owner);
        ParcelExtensions.writeInstant(dest, uploadTime);
        ParcelExtensions.writeInstant(dest, creationTime);
        ParcelExtensions.writeBoolean(dest, original);
        dest.writeMap(data);
        ParcelExtensions.writeBoolean(dest, loaded);
    }

    public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<>() {
        @NonNull
        @Override
        public Image createFromParcel(@NonNull Parcel source) {
            Image image = new Image(source.readLong());
            image.albumId = source.readLong();
            image.order = source.readLong();
            image.albumName = source.readString();
            image.format = source.readString();
            image.path = source.readString();
            image.name = source.readString();
            image.owner = source.readString();
            image.uploadTime = ParcelExtensions.readInstant(source);
            image.creationTime = ParcelExtensions.readInstant(source);
            image.original = ParcelExtensions.readBoolean(source);
            source.readMap(image.data, Image.class.getClassLoader());
            image.loaded = ParcelExtensions.readBoolean(source);
            return image;
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    public enum Type {
        IMAGE, VIDEO, AUDIO
    }
}
