package com.jonahbauer.qed.activities.settings.suggestions;

import androidx.annotation.NonNull;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.settings.ChatPreferenceFragment;
import com.jonahbauer.qed.util.Preferences;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.Set;

public class NameSuggestionsFragment extends SuggestionFragment {

    public NameSuggestionsFragment() {
        super(R.plurals.suggestion_name_deleted, R.string.message_clip_label_name);
    }

    @Override
    protected @NonNull Set<String> getValues() {
        return Preferences.getChat().getRecentNames();
    }

    @Override
    protected void setValues(@NonNull Set<String> values) {
        Preferences.getChat().setRecentNames(values);
    }

    @Override
    protected @NonNull ViewUtils.ChipItem createChip(@NonNull String value) {
        return ChatPreferenceFragment.createNameChip(requireContext(), value);
    }
}
