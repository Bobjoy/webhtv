package com.fongmi.android.tv.ui.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogGithubProxyBinding;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.update.GithubProxy;
import com.fongmi.android.tv.update.UpdateUrl;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** GitHub 加速设置的手机端口：选一个偏好地址，其余预设由 {@link GithubProxy#chain} 自动兜底。 */
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
        return new MaterialAlertDialogBuilder(requireActivity(), R.style.ThemeOverlay_WebHTV_LightDialog).setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        proxy = Setting.getUpdateGithubProxy();
        binding.custom.setText(Setting.getUpdateGithubProxyUrl());
        render();
    }

    @Override
    protected void initEvent() {
        binding.negative.setOnClickListener(v -> dismiss());
        binding.positive.setOnClickListener(v -> onPositive());
        binding.proxy.setOnClickListener(v -> choose());
    }

    @Override
    public void onStart() {
        super.onStart();
        configureWindow();
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
        binding.customLayout.setVisibility(custom ? View.VISIBLE : View.GONE);
        if (custom) binding.custom.setSelection(binding.custom.getText().length());
    }

    private String label(GithubProxy.Preset preset) {
        if (GithubProxy.DIRECT.equals(preset.id)) return getString(R.string.update_proxy_direct);
        if (GithubProxy.CUSTOM.equals(preset.id)) return getString(R.string.update_proxy_custom);
        return preset.label;
    }

    private void onPositive() {
        String value = binding.custom.getText() == null ? "" : binding.custom.getText().toString().trim();
        binding.customLayout.setError(null);
        if (GithubProxy.CUSTOM.equals(proxy)) {
            try {
                UpdateUrl.requireHttpsOrigin(value);
            } catch (Exception e) {
                binding.customLayout.setError(getString(R.string.update_proxy_invalid));
                return;
            }
        }
        Setting.putUpdateGithubProxy(proxy);
        Setting.putUpdateGithubProxyUrl(value);
        Notify.show(R.string.github_proxy_saved);
        dismiss();
    }

    private void configureWindow() {
        if (getDialog() == null || getDialog().getWindow() == null) return;
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        boolean land = ResUtil.isLand(requireContext());
        int width = Math.min(Math.round(ResUtil.getScreenWidth(requireContext()) * (land ? 0.5f : 0.9f)), ResUtil.dp2px(480));
        params.width = Math.max(width, ResUtil.dp2px(300));
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.getDecorView().setPadding(0, 0, 0, 0);
        window.setAttributes(params);
        window.setLayout(params.width, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
