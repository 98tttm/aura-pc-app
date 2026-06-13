package com.example.aura_pc_app.ui.home;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class HomeSearchSuggestionAdapter extends RecyclerView.Adapter<HomeSearchSuggestionAdapter.SuggestionViewHolder> {
    interface SuggestionClickListener {
        void onSuggestionClick(String keyword);
    }

    private final List<String> suggestions = new ArrayList<>();
    private final SuggestionClickListener listener;
    private String highlightQuery = "";

    HomeSearchSuggestionAdapter(SuggestionClickListener listener) {
        this.listener = listener;
    }

    void submitList(List<String> newSuggestions, String query) {
        suggestions.clear();
        if (newSuggestions != null) {
            suggestions.addAll(newSuggestions);
        }
        highlightQuery = query == null ? "" : query.trim();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        holder.text.setText(highlight(suggestion, highlightQuery, holder));
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(suggestion));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    private CharSequence highlight(String text, String query, SuggestionViewHolder holder) {
        if (query == null || query.trim().isEmpty()) {
            return text;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);
        int start = lowerText.indexOf(lowerQuery);
        if (start < 0) {
            return text;
        }

        int end = start + lowerQuery.length();
        SpannableString spannable = new SpannableString(text);
        int color = ContextCompat.getColor(holder.itemView.getContext(), R.color.aura_orange);
        spannable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.suggestionText);
        }
    }
}
