package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.api.subscription.SubscriptionParser;
import com.fongmi.android.tv.databinding.AdapterSubscriptionItemBinding;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionItemAdapter extends RecyclerView.Adapter<SubscriptionItemAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<SubscriptionParser.Item> mItems = new ArrayList<>();
    private String active;

    public SubscriptionItemAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {

        void onItemClick(SubscriptionParser.Item item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setActive(String url) {
        active = url == null ? "" : url;
    }

    public void setItems(List<SubscriptionParser.Item> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSubscriptionItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubscriptionParser.Item item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        holder.binding.remark.setVisibility(TextUtils.isEmpty(item.getRemark()) ? View.GONE : View.VISIBLE);
        holder.binding.remark.setText(item.getRemark());
        holder.binding.active.setVisibility(item.getUrl().equals(active) ? View.VISIBLE : View.GONE);
        holder.binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        ImgUtil.load(item.getName(), item.getLogo(), holder.binding.logo, false);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSubscriptionItemBinding binding;

        ViewHolder(@NonNull AdapterSubscriptionItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
