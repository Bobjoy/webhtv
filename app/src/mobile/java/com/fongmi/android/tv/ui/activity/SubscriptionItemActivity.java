package com.fongmi.android.tv.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.ConfigActivator;
import com.fongmi.android.tv.api.subscription.SubscriptionLoader;
import com.fongmi.android.tv.api.subscription.SubscriptionParser;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Subscription;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.databinding.ActivitySubscriptionItemBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.setting.SubscriptionStore;
import com.fongmi.android.tv.ui.adapter.SubscriptionItemAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.utils.Prefers;

import java.util.List;

/** 一条订阅拉取出来的候选资源地址列表；点一条即成为生效配置。 */
public class SubscriptionItemActivity extends BaseActivity implements SubscriptionItemAdapter.OnClickListener {

    private static final String TYPE = "type";
    private static final String URL = "url";
    private static final String TITLE = "title";

    private ActivitySubscriptionItemBinding mBinding;
    private SubscriptionItemAdapter mAdapter;
    private String url;
    private int type;

    public static void start(Context context, Subscription subscription) {
        Intent intent = new Intent(context, SubscriptionItemActivity.class);
        intent.putExtra(TYPE, subscription.getType());
        intent.putExtra(URL, subscription.getUrl());
        intent.putExtra(TITLE, subscription.getDisplayName());
        context.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySubscriptionItemBinding.inflate(getLayoutInflater());
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        type = getIntent().getIntExtra(TYPE, 0);
        url = getIntent().getStringExtra(URL);
        setSupportActionBar(mBinding.toolbar);
        mBinding.toolbar.setTitle(getIntent().getStringExtra(TITLE));
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recycler.setAdapter(mAdapter = new SubscriptionItemAdapter(this));
        mAdapter.setActive(Prefers.getString("config_" + type));
    }

    @Override
    protected void initEvent() {
        load(url);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackInvoked();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(SubscriptionParser.Item item) {
        Config config = AppDatabase.get().getConfigDao().find(item.getUrl(), type);
        if (config == null) {
            config = Config.create(type).url(item.getUrl()).name(item.getName());
            config.setLogo(item.getLogo());
        }
        config.update();
        SubscriptionStore.get().putActive(type, url, item.getUrl());
        ConfigActivator.activate(config, getCallback());
        finish();
    }

    private void load(String url) {
        List<SubscriptionParser.Item> cached = SubscriptionStore.get().getCachedItems(type, url);
        if (cached.isEmpty()) mBinding.progressLayout.showProgress();
        else show(cached);
        new Thread(() -> {
            try {
                List<SubscriptionParser.Item> items = SubscriptionLoader.load(url);
                SubscriptionStore.get().cacheItems(type, url, items);
                runOnUiThread(() -> show(items));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Notify.show(Notify.getError(R.string.subscription_load_failed, e));
                    if (cached.isEmpty()) finish();
                });
            }
        }).start();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        };
    }

    private void show(List<SubscriptionParser.Item> items) {
        if (isFinishing() || isDestroyed()) return;
        mAdapter.setItems(items);
        mBinding.progressLayout.showContent(true, items.size());
    }
}
