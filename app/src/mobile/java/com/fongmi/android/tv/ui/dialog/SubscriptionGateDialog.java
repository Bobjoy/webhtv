package com.fongmi.android.tv.ui.dialog;

import android.content.Intent;
import android.view.inputmethod.EditorInfo;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.subscription.SubscriptionCodes;
import com.fongmi.android.tv.api.subscription.SubscriptionLoader;
import com.fongmi.android.tv.databinding.DialogSubscriptionGateBinding;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

/** 订阅门禁：输入 4 位授权码，命中 webhtv-sub/codes.txt 即写入解锁标记并重启（ADR-0007）。 */
public class SubscriptionGateDialog extends BaseAlertDialog {

    private static final long RESTART_DELAY = 1500;

    private DialogSubscriptionGateBinding binding;

    public static SubscriptionGateDialog create() {
        return new SubscriptionGateDialog();
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSubscriptionGateBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return new MaterialAlertDialogBuilder(requireActivity(), R.style.ThemeOverlay_WebHTV_LightDialog).setView(getBinding().getRoot());
    }

    @Override
    protected void initEvent() {
        binding.negative.setOnClickListener(v -> dismiss());
        binding.positive.setOnClickListener(v -> verify());
        binding.code.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) verify();
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.code.requestFocus();
    }

    private void verify() {
        String code = binding.code.getText().toString().trim();
        if (code.length() != 4) {
            Notify.show(R.string.subscription_gate_code_required);
            return;
        }
        binding.positive.setEnabled(false);
        Task.largeExecutor().execute(() -> {
            boolean passed;
            int result;
            try {
                passed = SubscriptionCodes.check(SubscriptionLoader.text(SubscriptionCodes.URL), code);
                result = passed ? 0 : R.string.subscription_gate_wrong_code;
            } catch (Exception e) {
                passed = false;
                result = R.string.subscription_gate_unreachable;
            }
            if (passed) {
                Setting.putSubscriptionUnlocked();
                App.post(this::onPass);
                return;
            }
            int toast = result;
            App.post(() -> {
                Notify.show(toast);
                enable();
            });
        });
    }

    private void onPass() {
        FragmentActivity activity = requireActivity();
        Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        Notify.show(R.string.subscription_gate_unlocked);
        dismiss();
        // 不能用 restartApplication(activity, config)：它读 config.getRestartActivityClass()，而 Startup 的 CaocConfig 没设这一项（只有崩溃流程会填）。
        App.post(() -> CustomActivityOnCrash.restartApplicationWithIntent(activity, intent, CustomActivityOnCrash.getConfig()), RESTART_DELAY);
    }

    private void enable() {
        if (isAdded()) binding.positive.setEnabled(true);
    }
}
