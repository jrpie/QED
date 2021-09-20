package com.jonahbauer.qed.model.viewmodel;

import static com.jonahbauer.qed.util.StatusWrapper.STATUS_ERROR;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_LOADED;
import static com.jonahbauer.qed.util.StatusWrapper.STATUS_PRELOADED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jonahbauer.qed.model.Person;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.networking.async.QEDPageReceiver;
import com.jonahbauer.qed.networking.pages.QEDDBPages;
import com.jonahbauer.qed.util.StatusWrapper;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class PersonViewModel extends ViewModel implements QEDPageReceiver<Person> {
    private final MutableLiveData<StatusWrapper<Person>> mPerson = new MutableLiveData<>();
    private final CompositeDisposable mDisposable = new CompositeDisposable();

    public void load(@NonNull Person person) {
        if (!person.isLoaded()) {
            this.mPerson.setValue(StatusWrapper.wrap(person, STATUS_PRELOADED));
            mDisposable.add(
                    QEDDBPages.getPerson(person, this)
            );
        } else {
            onPageReceived(person);
        }
    }

    public LiveData<StatusWrapper<Person>> getPerson() {
        return mPerson;
    }

    @Override
    public void onPageReceived(@NonNull Person out) {
        out.setLoaded(true);
        this.mPerson.setValue(StatusWrapper.wrap(out, STATUS_LOADED));
    }

    @Override
    public void onError(Person out, @NonNull Reason reason, @Nullable Throwable cause) {
        QEDPageReceiver.super.onError(out, reason, cause);
        this.mPerson.setValue(StatusWrapper.wrap(out, STATUS_ERROR, reason));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDisposable.clear();
    }
}