package com.loretacafe.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.data.remote.dto.SalesSummaryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple adapter to show summary rows such as top products.
 * For now, it displays total gross, profit and orders per section.
 * Can be extended later to match detailed wireframe tables.
 */
public class SalesSummaryAdapter extends RecyclerView.Adapter<SalesSummaryAdapter.ViewHolder> {

    private final List<String> rows = new ArrayList<>();

    public void submit(SalesSummaryDto dto, double averageOrder) {
        rows.clear();
        if (dto == null) {
            notifyDataSetChanged();
            return;
        }
        double gross = dto.getTotalSales() != null ? dto.getTotalSales().doubleValue() : 0.0;
        rows.add("Gross Sales: ₱ " + String.format("%,.2f", gross));
        rows.add("Total Orders: " + dto.getTotalOrders());
        rows.add("Total Items Sold: " + dto.getTotalItems());
        rows.add("Average Order: ₱ " + String.format("%,.2f", averageOrder));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.text.setText(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(android.R.id.text1);
        }
    }
}


