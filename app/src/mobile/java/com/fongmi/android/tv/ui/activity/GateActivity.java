package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.subscription.Gate;
import com.fongmi.android.tv.databinding.ActivityGateBinding;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Task;

public class GateActivity extends BaseActivity {

    private ActivityGateBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, GateActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityGateBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        if (Gate.verifyCached()) enter();
    }

    @Override
    protected void initEvent() {
        mBinding.positive.setOnClickListener(v -> submit());
        mBinding.code.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            submit();
            return true;
        });
    }

    private void submit() {
        String code = mBinding.code.getText().toString().trim();
        if (code.length() != 4) {
            Notify.show(R.string.gate_code_required);
            return;
        }
        mBinding.positive.setEnabled(false);
        Task.largeExecutor().execute(() -> {
            Gate.Result result = Gate.submit(code);
            runOnUiThread(() -> {
                mBinding.positive.setEnabled(true);
                if (result == Gate.Result.PASSED) enter();
                else Notify.show(result == Gate.Result.WRONG ? R.string.gate_wrong_code : R.string.gate_unreachable);
            });
        });
    }

    private void enter() {
        Task.largeExecutor().execute(Gate::refreshCodes);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
