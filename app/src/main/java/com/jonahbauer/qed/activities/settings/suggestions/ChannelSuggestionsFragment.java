package com.jonahbauer.qed.activities.settings.suggestions;

import androidx.annotation.NonNull;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.settings.ChatPreferenceFragment;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.Set;

public class ChannelSuggestionsFragment extends SuggestionFragment {

    public ChannelSuggestionsFragment() {
        super(R.plurals.suggestion_channel_deleted, R.string.message_clip_label_channel);
    }

    @Override
    protected @NonNull Set<String> getValues() {
        return Preferences.getChat().getRecentChannels();
    }

    @Override
    protected void setValues(@NonNull Set<String> values) {
        Preferences.getChat().setRecentChannels(values);
    }

    @Override
    protected @NonNull ViewUtils.ChipItem createChip(@NonNull String value) {
        return ChatPreferenceFragment.createChannelChip(requireContext(), value);
    }
}
