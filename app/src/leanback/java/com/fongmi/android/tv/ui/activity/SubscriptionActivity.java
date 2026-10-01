package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.subscription.SubscriptionLoader;
import com.fongmi.android.tv.bean.Subscription;
import com.fongmi.android.tv.databinding.ActivitySubscriptionBinding;
import com.fongmi.android.tv.databinding.AdapterSubscriptionBinding;
import com.fongmi.android.tv.setting.SubscriptionStore;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.SubscriptionEditDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.utils.Prefers;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;

public class SubscriptionActivity extends BaseActivity implements SubscriptionEditDialog.Listener {

    private ActivitySubscriptionBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SubscriptionActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySubscriptionBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initEvent() {
        mBinding.refresh.setOnClickListener(v -> update());
        mBinding.vodAdd.setOnClickListener(v -> SubscriptionEditDialog.create(0).show(this));
        mBinding.liveAdd.setOnClickListener(v -> SubscriptionEditDialog.create(1).show(this));
        mBinding.wallAdd.setOnClickListener(v -> SubscriptionEditDialog.create(2).show(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onSubscription(Subscription item) {
        refresh();
    }

    private void refresh() {
        SubscriptionStore.get().seedDefaults();
        List<Subscription> items = SubscriptionStore.get().getAll();
        mBinding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        fill(mBinding.vodList, items, 0);
        fill(mBinding.liveList, items, 1);
        fill(mBinding.wallList, items, 2);
    }

    private void fill(LinearLayoutCompat container, List<Subscription> items, int type) {
        container.removeAllViews();
        String active = Prefers.getString("config_" + type);
        for (Subscription item : items) {
            if (item.getType() != type) continue;
            boolean defaultItem = SubscriptionStore.get().isDefault(item);
            AdapterSubscriptionBinding binding = AdapterSubscriptionBinding.inflate(getLayoutInflater(), container, false);
            binding.name.setText(item.getDisplayName());
            binding.url.setText(item.getUrl());
            binding.active.setVisibility(SubscriptionStore.get().isActive(type, item.getUrl(), active) ? View.VISIBLE : View.GONE);
            binding.pull.setOnClickListener(v -> SubscriptionItemActivity.start(this, item));
            binding.getRoot().setOnClickListener(v -> {
                if (defaultItem) Notify.show(R.string.subscription_edit_locked);
                else SubscriptionEditDialog.edit(item).show(this);
            });
            container.addView(binding.getRoot());
        }
    }

    private void update() {
        SubscriptionStore store = SubscriptionStore.get();
        List<Subscription> items = store.getAll();
        if (items.isEmpty()) {
            Notify.show(R.string.subscription_empty);
            return;
        }
        Notify.show(R.string.subscription_updating);
        CompletionService<Integer> service = new ExecutorCompletionService<>(Task.largeExecutor());
        for (Subscription item : items) {
            service.submit(() -> {
                try {
                    store.cacheItems(item.getType(), item.getUrl(), SubscriptionLoader.load(item.getUrl()));
                    return 1;
                } catch (Exception e) {
                    return 0;
                }
            });
        }
        Task.execute(() -> {
            int success = 0;
            for (int i = 0; i < items.size(); i++) {
                try {
                    success += service.take().get();
                } catch (Exception ignored) {
                }
            }
            int count = success;
            App.post(() -> Notify.show(ResUtil.getString(R.string.subscription_update_done, count, items.size())));
        });
    }
}
