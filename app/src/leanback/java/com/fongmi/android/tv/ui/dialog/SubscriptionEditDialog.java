package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Subscription;
import com.fongmi.android.tv.databinding.DialogSubscriptionBinding;
import com.fongmi.android.tv.setting.SubscriptionStore;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SubscriptionEditDialog extends BaseAlertDialog {

    private DialogSubscriptionBinding binding;
    private Subscription origin;
    private int type;

    public interface Listener {

        void onSubscription(Subscription item);
    }

    public static SubscriptionEditDialog create(int type) {
        SubscriptionEditDialog dialog = new SubscriptionEditDialog();
        Bundle args = new Bundle();
        args.putInt("type", type);
        dialog.setArguments(args);
        return dialog;
    }

    public static SubscriptionEditDialog edit(Subscription item) {
        SubscriptionEditDialog dialog = create(item.getType());
        Bundle args = dialog.getArguments();
        args.putString("originName", item.getName());
        args.putString("originUrl", item.getUrl());
        return dialog;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSubscriptionBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        getBinding();
        initView();
        initEvent();
        String neutral = origin == null ? null : getString(R.string.setting_delete);
        return LightDialog.create(requireContext(), getTitle(), binding.getRoot(), getString(origin == null ? R.string.subscription_add : R.string.dialog_edit), view -> onPositive(), getString(R.string.dialog_negative), null, neutral, view -> onDelete());
    }

    @Override
    protected void initView() {
        Bundle args = requireArguments();
        type = args.getInt("type");
        String originUrl = args.getString("originUrl");
        origin = originUrl == null ? null : new Subscription(args.getString("originName"), originUrl, type);
        binding.name.setText(origin == null ? "" : origin.getName());
        binding.url.setText(originUrl == null ? "" : originUrl);
        binding.url.setSelection(binding.url.getText().length());
    }

    @Override
    protected void initEvent() {
        binding.url.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onPositive();
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.url.requestFocus();
    }

    private CharSequence getTitle() {
        return getString(R.string.setting_config_dialog_title, getString(origin == null ? R.string.subscription_add : R.string.remote_trust_config_edit), getString(getTypeName()));
    }

    private void onPositive() {
        Subscription item = new Subscription(binding.name.getText().toString(), binding.url.getText().toString(), type);
        if (item.isEmpty()) {
            Notify.show(R.string.subscription_url_required);
            binding.url.requestFocus();
            return;
        }
        boolean saved = origin == null ? SubscriptionStore.get().add(item) : SubscriptionStore.get().update(origin, item);
        if (!saved) {
            Notify.show(R.string.subscription_duplicated);
            return;
        }
        ((Listener) requireActivity()).onSubscription(item);
        dismiss();
    }

    private void onDelete() {
        SubscriptionStore.get().delete(origin);
        ((Listener) requireActivity()).onSubscription(origin);
        dismiss();
    }

    private int getTypeName() {
        return switch (type) {
            case 0 -> R.string.setting_vod;
            case 1 -> R.string.setting_live;
            case 2 -> R.string.setting_wall;
            default -> R.string.remote_trust_config_type;
        };
    }
}
