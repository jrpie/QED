package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.fragment.NavHostFragment;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentChatDatabaseBinding;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.MessageFilter;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.model.room.Database;
import com.jonahbauer.qed.model.room.MessageDao;
import com.jonahbauer.qed.model.viewmodel.MessageListViewModel;
import com.jonahbauer.qed.util.*;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.time.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ChatDatabaseFragment extends Fragment {
    private static final String LOG_TAG = ChatDatabaseFragment.class.getName();

    private MessageAdapter mMessageAdapter;
    private MessageListViewModel mMessageListViewModel;

    private MutableLiveData<LocalDate> mDateFrom;
    private MutableLiveData<LocalTime> mTimeFrom;
    private MutableLiveData<LocalDate> mDateTo;
    private MutableLiveData<LocalTime> mTimeTo;

    private FragmentChatDatabaseBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Search the database for possible misassigned dates due to daylight savings time
        // and try to fix them
        Function<Message, Message> dateFixer = MessageUtils.dateFixer();
        MessageDao messageDao = Database.getInstance(getContext()).messageDao();
        //noinspection ResultOfMethodCallIgnored
        messageDao.possibleDateErrors()
                  .subscribeOn(Schedulers.io())
                  .observeOn(Schedulers.computation())
                  .map(list -> list.stream()
                                   .map(msg -> {
                                       Message out = dateFixer.apply(msg);
                                       if (out == msg) return null;
                                       else return out;
                                   })
                                   .filter(Objects::nonNull)
                                   .collect(Collectors.toList())
                  )
                  .flatMapCompletable(messageDao::insert)
                  .subscribe(
                          () -> {},
                          err -> Log.e(LOG_TAG, "Error fixing dates.", err)
                  );

        TransitionUtils.setupDefaultTransitions(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentChatDatabaseBinding.inflate(inflater, container, false);
        mMessageListViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_chat_db).get(MessageListViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mDateFrom = new MutableLiveData<>(LocalDate.now());
        mTimeFrom = new MutableLiveData<>(LocalTime.now());
        mDateTo = new MutableLiveData<>(LocalDate.now());
        mTimeTo = new MutableLiveData<>(LocalTime.now());

        // setup list view
        mMessageAdapter = new MessageAdapter(requireContext(), new ArrayList<>(), null, false, null, true);
        mBinding.messageListView.setOnItemClickListener((parent, v, position, id) -> {
            setCheckedItem(MessageAdapter.INVALID_POSITION);
        });
        mBinding.messageListView.setOnItemLongClickListener((parent, v, position, id) -> {
            setCheckedItem(position);
            return true;
        });
        mBinding.messageListView.setAdapter(mMessageAdapter);

        mBinding.searchButton.setOnClickListener(v -> search());

        ViewUtils.setupExpandable(mBinding.expandCheckBox, mBinding.expandable);
        ViewUtils.link(mBinding.databaseChannelCheckbox, mBinding.databaseChannelEditText);
        ViewUtils.link(mBinding.databaseMessageCheckbox, mBinding.databaseMessageEditText);
        ViewUtils.link(mBinding.databaseNameCheckbox, mBinding.databaseNameEditText);
        ViewUtils.link(mBinding.databaseDateFromCheckbox, mBinding.databaseDateFromEditText, mBinding.databaseTimeFromEditText);
        ViewUtils.link(mBinding.databaseDateToCheckbox, mBinding.databaseDateToEditText, mBinding.databaseTimeToEditText);
        ViewUtils.link(mBinding.databaseIdCheckbox, mBinding.databaseIdEditText);

        ViewUtils.setupDateEditText(mBinding.databaseDateFromEditText, mDateFrom);
        ViewUtils.setupDateEditText(mBinding.databaseDateToEditText, mDateTo);
        ViewUtils.setupTimeEditText(mBinding.databaseTimeFromEditText, mTimeFrom);
        ViewUtils.setupTimeEditText(mBinding.databaseTimeToEditText, mTimeTo);

        mMessageListViewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            mBinding.setStatus(messages.getCode());

            mMessageAdapter.clear();
            if (messages.getCode() == StatusWrapper.STATUS_LOADED) {
                mMessageAdapter.addAll(messages.getValue());

                mBinding.setHits(getString(R.string.hits, messages.getValue().size()));
            }
            mMessageAdapter.notifyDataSetChanged();

            if (messages.getCode() == StatusWrapper.STATUS_LOADED) {
                setCheckedItem(mMessageListViewModel.getCheckedItemPosition());
            } else {
                setCheckedItem(MessageAdapter.INVALID_POSITION);
            }
        });
    }

    /**
     * Sets the checked item in the list view and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link #mMessageAdapter}
     */
    private void setCheckedItem(int position) {
        mMessageListViewModel.setCheckedItemPosition(position);
        MessageUtils.setCheckedItem(
                this,
                mBinding.messageListView,
                mMessageAdapter,
                (mode, msg) -> NavHostFragment.findNavController(this)
                                              .navigate(ChatDatabaseFragmentDirections.showMessage(msg)),
                null,
                position
        );
    }

    private void search() {
        if (!checkFilters()) return;

        String channel = null;
        String message = null;
        String name = null;
        Instant fromDate = null;
        Instant toDate = null;
        Long fromId = null;
        Long toId = null;

        if (mBinding.databaseChannelCheckbox.isChecked()) {
            channel = mBinding.databaseChannelEditText.getText().toString();
        }

        if (mBinding.databaseMessageCheckbox.isChecked()) {
            message = mBinding.databaseMessageEditText.getText().toString();
        }

        if (mBinding.databaseNameCheckbox.isChecked()) {
            name = mBinding.databaseNameEditText.getText().toString();
        }
        
        if (mBinding.databaseDateFromCheckbox.isChecked()) {
            LocalDate date = mDateFrom.getValue();
            LocalTime time = mTimeFrom.getValue();
            fromDate = ZonedDateTime.of(date, time, ZoneId.systemDefault()).toInstant();
        }

        if (mBinding.databaseDateToCheckbox.isChecked()) {
            LocalDate date = mDateTo.getValue();
            LocalTime time = mTimeTo.getValue();
            toDate = ZonedDateTime.of(date, time, ZoneId.systemDefault()).toInstant();
        }

        if (mBinding.databaseIdCheckbox.isChecked()) {
            String str = mBinding.databaseIdEditText.getText().toString();
            Pattern pattern = Pattern.compile("(\\d+)?(-)?(\\d+)?");
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
                String from = matcher.group(1);
                boolean single = matcher.group(2) == null;
                String to = matcher.group(3);

                if (from != null) {
                    try {
                        fromId = Long.parseLong(from);
                    } catch (NumberFormatException ignored) {}
                }

                if (to != null) {
                    try {
                        toId = Long.parseLong(to);
                    } catch (NumberFormatException ignored) {}
                }

                if (single) {
                    toId = fromId;
                }
            }
        }

        mMessageListViewModel.load(new MessageFilter(channel, message, name, fromDate, toDate, fromId, toId));
    }
    
    private boolean checkFilters() {
        boolean valid = true;

        // id
        if (mBinding.databaseIdCheckbox.isChecked()) {
            boolean idValid = checkId(mBinding.databaseIdEditText.getText().toString());
            valid = idValid;
            ViewUtils.setError(mBinding.databaseIdEditText, !idValid);
        } else {
            ViewUtils.setError(mBinding.databaseIdEditText, false);
        }

        return valid;
    }

    private boolean checkId(String str) {
        Pattern pattern = Pattern.compile("(\\d+)?-?(\\d+)?");
        Matcher matcher = pattern.matcher(str);

        if (matcher.matches()) {
            String from = matcher.group(1);
            String to = matcher.group(2);

            if (from == null && to == null) {
                return false;
            }

            try {
                if (from != null) Long.parseLong(from);
                if (to != null) Long.parseLong(to);
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }
}
