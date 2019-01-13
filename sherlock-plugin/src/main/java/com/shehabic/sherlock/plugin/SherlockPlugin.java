package com.shehabic.sherlock.plugin;

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

class SherlockPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Object ext = project.getExtensions().findByName("android");
        BaseAppModuleExtension androidExt = (BaseAppModuleExtension) ext;
        if (androidExt != null) {
            androidExt.registerTransform(new SherlockTransformer(project));
            project.getExtensions().create("SherlockExtension", SherlockExtension.class);
        } else {
        }
    }
}
