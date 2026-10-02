package com.fongmi.android.tv.api.subscription;

import com.github.catvod.utils.Prefers;

/**
 * 启动口令门禁的编排：口令（{@code gate_code}）与最近一次拉到的码表原文（{@code gate_codes}）都缓存在本地，
 * 冷启动只用两者离线比对；联网只在缓存不含该口令时发生（ADR-0008 修订）。
 */
public class Gate {

    public enum Result { PASSED, WRONG, UNREACHABLE }

    private static final String CODE = "gate_code";
    private static final String CODES = "gate_codes";

    private static volatile boolean mUnlocked;

    public static boolean isUnlocked() {
        return mUnlocked;
    }

    /** 离线比对缓存口令与缓存码表，通过即解锁。 */
    public static boolean verifyCached() {
        return mUnlocked = matches(Prefers.getString(CODES, ""), Prefers.getString(CODE, ""));
    }

    /** 缓存码表命中就直接过，不联网；缓存不含该口令才拉最新码表，拉不到算网络不可用。 */
    public static Result submit(String code) {
        if (matches(Prefers.getString(CODES, ""), code)) return pass(code);
        String fresh = refreshCodes();
        if (fresh == null) return Result.UNREACHABLE;
        return matches(fresh, code) ? pass(code) : Result.WRONG;
    }

    /**
     * @return 最新码表原文，拉取失败或一行可用码都没有时返回 {@code null}，让调用方把「拉不到」与「口令不对」分开提示。
     */
    public static String refreshCodes() {
        try {
            String codes = SubscriptionLoader.text(SubscriptionCodes.URL);
            SubscriptionCodes.check(codes, "");
            Prefers.put(CODES, codes);
            return codes;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean matches(String codes, String code) {
        try {
            return codes != null && SubscriptionCodes.check(codes, code);
        } catch (Exception e) {
            return false;
        }
    }

    private static Result pass(String code) {
        Prefers.put(CODE, code.trim());
        mUnlocked = true;
        return Result.PASSED;
    }
}
