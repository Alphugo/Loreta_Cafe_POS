package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying transactions in RecyclerView with section headers
 */
public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_FOOTER = 2;

    private List<Object> items; // Mix of String (headers) and Transaction objects
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(OnTransactionClickListener listener) {
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Set transactions and group them by section
     */
    public void setTransactions(List<Transaction> transactions) {
        items.clear();

        if (transactions == null || transactions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Group transactions by section
        LinkedHashMap<String, List<Transaction>> groupedTransactions = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            String section = transaction.getSection();
            if (!groupedTransactions.containsKey(section)) {
                groupedTransactions.put(section, new ArrayList<>());
            }
            groupedTransactions.get(section).add(transaction);
        }

        // Add headers and items
        for (Map.Entry<String, List<Transaction>> entry : groupedTransactions.entrySet()) {
            items.add(entry.getKey()); // Add section header
            items.addAll(entry.getValue()); // Add transactions
        }

        // Add "End of history" footer
        items.add("END_OF_HISTORY");

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof String) {
            String str = (String) item;
            if (str.equals("END_OF_HISTORY")) {
                return VIEW_TYPE_FOOTER;
            }
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section_header, parent, false);
            return new FooterViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String header = (String) items.get(position);
            ((HeaderViewHolder) holder).bind(header);
        } else if (holder instanceof TransactionViewHolder) {
            Transaction transaction = (Transaction) items.get(position);
            ((TransactionViewHolder) holder).bind(transaction, listener);
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder for section headers
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionHeader;

        HeaderViewHolder(View itemView) {
            super(itemView);
            tvSectionHeader = itemView.findViewById(R.id.tvSectionHeader);
        }

        void bind(String header) {
            tvSectionHeader.setText(header);
            tvSectionHeader.setTextColor(0xFF000000);
            tvSectionHeader.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
    }

    // ViewHolder for footer
    static class FooterViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionHeader;

        FooterViewHolder(View itemView) {
            super(itemView);
            tvSectionHeader = itemView.findViewById(R.id.tvSectionHeader);
        }

        void bind() {
            tvSectionHeader.setText("End of history");
            tvSectionHeader.setTextColor(0xFFCCCCCC);
            tvSectionHeader.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvSectionHeader.setTextSize(12);
        }
    }

    // ViewHolder for transaction items
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName;
        TextView tvAmount;
        TextView tvDateTime;
        TextView tvPaymentMethod;

        TransactionViewHolder(View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
        }

        void bind(Transaction transaction, OnTransactionClickListener listener) {
            tvCustomerName.setText(transaction.getCustomerName());
            tvAmount.setText(transaction.getFormattedAmount());
            tvDateTime.setText(transaction.getDateTime());
            tvPaymentMethod.setText(transaction.getPaymentMethod());

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            });
        }
    }
}