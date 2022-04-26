package com.jonahbauer.qed.model;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.List;

import com.jonahbauer.qed.model.parcel.ParcelExtensions;
import com.jonahbauer.qed.model.room.MessageDao;
import io.reactivex.rxjava3.core.Single;
import lombok.Data;
import lombok.experimental.ExtensionMethod;

@Data
@ExtensionMethod(ParcelExtensions.class)
public class MessageFilter implements Parcelable {
    public static final MessageFilter EMPTY = new MessageFilter(null, null, null, null, null, null, null);

    private final @Nullable String channel;
    private final @Nullable String message;
    private final @Nullable String name;
    private final @Nullable Instant fromDate;
    private final @Nullable Instant toDate;
    private final @Nullable Long fromId;
    private final @Nullable Long toId;

    public Single<List<Message>> search(MessageDao dao, long limit) {
        return dao.findAll(channel, message, name, fromDate, toDate, fromId, toId, limit);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(channel);
        dest.writeString(message);
        dest.writeString(name);
        dest.writeInstant(fromDate);
        dest.writeInstant(toDate);
        dest.writeValue(fromId);
        dest.writeValue(toId);
    }

    public static final Creator<MessageFilter> CREATOR = new Creator<>() {
        @Override
        @SuppressLint("ParcelClassLoader")
        public MessageFilter createFromParcel(Parcel in) {
            return new MessageFilter(
                    in.readString(),
                    in.readString(),
                    in.readString(),
                    in.readInstant(),
                    in.readInstant(),
                    (Long) in.readValue(null),
                    (Long) in.readValue(null)
            );
        }

        @Override
        public MessageFilter[] newArray(int size) {
            return new MessageFilter[size];
        }
    };
}
