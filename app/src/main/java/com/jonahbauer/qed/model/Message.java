package com.jonahbauer.qed.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jonahbauer.qed.model.room.Converters;
import com.jonahbauer.qed.networking.NetworkConstants;
import com.jonahbauer.qed.util.Preferences;

import org.jetbrains.annotations.Contract;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * An object representing a message in the qed chat
 */
@Data
@Entity
@EqualsAndHashCode(of = "id")
@TypeConverters(Converters.class)
public class Message implements Parcelable, Comparable<Message>, Serializable {
    private static final String LOG_TAG = Message.class.getName();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.GERMANY)
            .withZone(NetworkConstants.SERVER_TIME_ZONE);

    public static final Message PONG = new Message(0, "PONG", "PONG", Instant.EPOCH, 0, "PONG", "000000", 0, "PONG");
    public static final Message PING = new Message(0, "PING", "PING", Instant.EPOCH, 0, "PING", "000000", 0, "PING");
    public static final Message ACK = new Message(0, "ACK", "ACK", Instant.EPOCH, 0, "ACK", "000000", 0, "ACK");
    public static final Message OK = new Message(0, "OK", "OK", Instant.EPOCH, 0, "OK", "000000", 0, "OK");

    @PrimaryKey
    private final long id;

    @NonNull
    @ColumnInfo(name = "name")
    private final String rawName;

    @NonNull
    @Ignore
    private final String name;

    @NonNull
    @ColumnInfo(name = "message")
    private final String message;

    @NonNull
    @ColumnInfo(name = "date")
    private final Instant date;

    @ColumnInfo(name = "user_id")
    private final long userId;

    @Nullable
    @ColumnInfo(name = "user_name")
    private final String userName;

    @NonNull
    @ColumnInfo(name = "color")
    private final String color;

    @NonNull
    @ColumnInfo(name = "channel")
    private final String channel;

    @ColumnInfo(name = "bottag")
    private final int bottag;

    @Ignore
    @ColorInt
    private final int transformedColor;

    @Ignore
    @Getter(AccessLevel.PRIVATE)
    private final boolean error;

    @Ignore
    public Message(@NonNull String message) {
        this.name = "Error";
        this.rawName = "Error";
        this.message = message;
        this.date = Instant.now();
        this.userId = 503;
        this.userName = "Error";
        this.color = "220000";
        this.id = Long.MAX_VALUE;
        this.bottag = 0;
        this.channel = "";

        this.transformedColor = Color.rgb(255, 0, 0);
        this.error = true;
    }

    @Ignore
    public Message(long id,
                   @NonNull String rawName,
                   @NonNull String message,
                   @NonNull Instant date,
                   @NonNull String color,
                   int bottag,
                   @NonNull String channel) {
        this(id, rawName, message, date, -1, null, color, bottag, channel);
    }

    public Message(long id,
                   @NonNull String rawName,
                   @NonNull String message,
                   @NonNull Instant date,
                   long userId,
                   @Nullable String userName,
                   @NonNull String color,
                   int bottag,
                   @NonNull String channel) {
        this.rawName = rawName;
        this.name = rawName.trim();
        this.message = message;
        this.date = date;
        this.userId = userId;
        this.userName = userName;
        this.color = color;
        this.id = id;
        this.bottag = bottag;
        this.channel = channel;

        this.transformedColor = transformColor(color);
        this.error = false;
    }

    @NonNull
    public String toString() {
        return "{"
                + "\"id\":\"" + id + "\", "
                + "\"name\":\"" + name + "\", "
                + "\"message\":\"" + message + "\", "
                + "\"date\":\"" + date + "\", "
                + "\"userId\":\"" + userId + "\", "
                + "\"userName\":\"" + userName + "\", "
                + "\"color\":\"" + color + "\", "
                + "\"bottag\":\"" + bottag + "\", "
                + "\"channel\":\"" + channel + "\"}";
    }

    public Type getType() {
        if (this.isError()) {
            return Type.ERROR;
        } else if (this == PING) {
            return Type.PING;
        } else if (this == PONG) {
            return Type.PONG;
        } else if (this == ACK) {
            return Type.ACK;
        } else if (this == OK) {
            return Type.OK;
        } else {
            return Type.POST;
        }
    }

    public LocalDate getLocalDate() {
        return ZonedDateTime.ofInstant(date, ZoneId.systemDefault()).toLocalDate();
    }

    public boolean isBot() {
        return getBottag() != 0;
    }

    public boolean isAnonymous() {
        return name.isEmpty();
    }

    public int compareTo(Message other) {
        return Long.compare(id,other.id);
    }

    /**
     * @see #interpretJSONMessage(JSONObject)
     */
    @Nullable
    @Contract("null -> null")
    public static Message interpretJSONMessage(@Nullable String jsonMessage) {
        if (jsonMessage == null) return null;

        try {
            JSONObject json = new JSONObject(jsonMessage);
            return interpretJSONMessage(json);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not parse message: " + jsonMessage, e);
            return null;
        }
    }

    /**
     * Parses a string as obtained by the chat web socket to a message object.
     */
    @Nullable
    public static Message interpretJSONMessage(@NonNull JSONObject json) {
        try {
            String type = json.getString("type");
            switch (type) {
                case "ping":
                    return PING;
                case "pong":
                    return PONG;
                case "ack":
                    return ACK;
                case "ok":
                    return OK;
                case "post":
                    String rawName = json.getString("name");
                    String message = json.getString("message");
                    String username = json.isNull("username") ? null : json.getString("username");
                    String color = json.getString("color");
                    String dateStr = json.getString("date");
                    String channel = json.getString("channel");
                    int userid = json.optInt("user_id", -1);
                    int id = json.getInt("id");
                    int bottag = json.getInt("bottag");

                    Instant date;
                    try {
                        date = Instant.from(DATE_TIME_FORMATTER.parse(dateStr));
                    } catch (DateTimeParseException e) {
                        Log.w(LOG_TAG, "Message did not contain a valid date: " + json);
                        return null;
                    }

                    return new Message(id, rawName, message, date, userid, username, color, bottag, channel);
                default:
                    Log.e(LOG_TAG, "Unknown message type: \"" + type + "\"");
                    return null;
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not parse message: " + json, e);
            return null;
        }
    }

    @ColorInt
    private static int transformColor(String color) {
        return transformColor(Color.parseColor("#" + color));
    }

    @ColorInt
    private static int transformColor(int color) {
        if (Preferences.general().isNightMode()) {
            return color;
        } else {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);

            hsv[1] = 1;
            hsv[2] = 0.85f;

            return Color.HSVToColor(hsv);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(rawName);
        dest.writeString(message);
        dest.writeSerializable(date);
        dest.writeLong(userId);
        dest.writeString(userName);
        dest.writeString(color);
        dest.writeString(channel);
        dest.writeInt(bottag);
        dest.writeLong(id);
    }

    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<>() {

        @Override
        public Message createFromParcel(Parcel source) {
            String rawName = Objects.requireNonNull(source.readString());
            String message = Objects.requireNonNull(source.readString());
            Instant date = Objects.requireNonNull((Instant) source.readSerializable());
            long userId = source.readLong();
            String userName = source.readString();
            String color = Objects.requireNonNull(source.readString());
            String channel = Objects.requireNonNull(source.readString());
            int bottag = source.readInt();
            long id = source.readLong();

            return new Message(id, rawName, message, date, userId, userName, color, bottag, channel);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public enum Type {
        PING,
        PONG,
        ACK,
        OK,
        POST,
        ERROR
    }
}
