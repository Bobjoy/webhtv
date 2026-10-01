package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogGithubProxyBinding;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.update.GithubProxy;
import com.fongmi.android.tv.update.UpdateUrl;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** GitHub 加速设置的 TV 端口：选一个偏好地址，其余预设由 {@link GithubProxy#chain} 自动兜底。 */
public class GithubProxyDialog extends BaseAlertDialog {

    private DialogGithubProxyBinding binding;
    private String proxy;

    public static GithubProxyDialog create() {
        return new GithubProxyDialog();
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogGithubProxyBinding.inflate(getLayoutInflater());
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
        return LightDialog.create(requireContext(), getString(R.string.setting_github_proxy), binding.getRoot(), getString(R.string.dialog_positive), view -> onPositive(), getString(R.string.dialog_negative), null);
    }

    @Override
    protected void initView() {
        proxy = Setting.getUpdateGithubProxy();
        binding.custom.setText(Setting.getUpdateGithubProxyUrl());
        render();
    }

    @Override
    protected void initEvent() {
        binding.proxy.setOnClickListener(v -> choose());
        binding.custom.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onPositive();
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.proxy.requestFocus();
    }

    private void choose() {
        GithubProxy.Preset[] presets = GithubProxy.presets();
        CharSequence[] labels = new CharSequence[presets.length];
        int selected = 0;
        for (int i = 0; i < presets.length; i++) {
            labels[i] = label(presets[i]);
            if (presets[i].id.equals(proxy)) selected = i;
        }
        ChoiceDialog.showSingle(this, R.string.setting_github_proxy, labels, selected, which -> {
            proxy = presets[which].id;
            render();
        });
    }

    private void render() {
        GithubProxy.Preset preset = GithubProxy.find(proxy);
        boolean custom = GithubProxy.CUSTOM.equals(preset.id);
        binding.proxy.setText(label(preset));
        binding.custom.setVisibility(custom ? View.VISIBLE : View.GONE);
        if (custom) binding.custom.setSelection(binding.custom.getText().length());
    }

    private String label(GithubProxy.Preset preset) {
        if (GithubProxy.DIRECT.equals(preset.id)) return getString(R.string.update_proxy_direct);
        if (GithubProxy.CUSTOM.equals(preset.id)) return getString(R.string.update_proxy_custom);
        return preset.label;
    }

    private void onPositive() {
        String value = binding.custom.getText().toString().trim();
        if (GithubProxy.CUSTOM.equals(proxy)) {
            try {
                UpdateUrl.requireHttpsOrigin(value);
            } catch (Exception e) {
                Notify.show(R.string.update_proxy_invalid);
                return;
            }
        }
        Setting.putUpdateGithubProxy(proxy);
        Setting.putUpdateGithubProxyUrl(value);
        Notify.show(R.string.github_proxy_saved);
        dismiss();
    }
}
