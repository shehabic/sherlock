package com.shehabic.sherlock.plugin;

import com.android.build.gradle.BaseExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

class SherlockPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        BaseExtension androidExt = (BaseExtension) project.getExtensions().getByName("android");

        if (project.getPlugins().findPlugin("com.android.application") != null) {
            project.getExtensions().create("SherlockExtension", SherlockExtension.class);
            androidExt.registerTransform(new SherlockTransformer(project));
        } else if (project.getPlugins().findPlugin("com.android.library") != null) {
            throw new RuntimeException("Sherlock cannot be applied to library project", null);
        } else {
            throw new RuntimeException("Need android application plugin to be applied first", null);
        }
    }
}
