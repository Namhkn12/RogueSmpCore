package com.roguesmp.block.transfer;

public enum SideMode {
    NONE(false, false, false),
    PULL(true, false, false),
    SMART_PULL(true, true, false),
    PUSH(false, false, true),
    PULL_PUSH(true, false, true),
    SMART_PULL_PUSH(true, true, true);

    private final boolean pulls;
    private final boolean smart;
    private final boolean pushes;

    SideMode(boolean pulls, boolean smart, boolean pushes) {
        this.pulls = pulls;
        this.smart = smart;
        this.pushes = pushes;
    }

    public boolean pulls() {
        return pulls;
    }

    public boolean isSmart() {
        return smart;
    }

    public boolean pushes() {
        return pushes;
    }
}
