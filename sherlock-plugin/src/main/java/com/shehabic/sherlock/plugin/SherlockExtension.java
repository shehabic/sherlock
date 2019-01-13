package com.shehabic.sherlock.plugin;

public class SherlockExtension {

    public boolean enabled = true;

    public SherlockExtension() {
    }

    public void setInstrumentationEnabled(boolean val) {
        this.enabled = val;
    }

    public boolean isInstrumentationEnabled() {
        return this.enabled;
    }

}
