package com.shehabic.sherlock.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;

import org.gradle.api.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.android.build.api.transform.QualifiedContent.DefaultContentType;
import com.android.build.api.transform.QualifiedContent.Scope;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.AppExtension;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.util.Map;

class SherlockTransformer extends Transform {

    private final Project project;
    private final Set<QualifiedContent.ContentType> typeClasses;
    private final Set<QualifiedContent.Scope> scopes;
    private AppExtension appExt;
    private boolean sherlockEnabled = true;
    private SherlockInstrumentation instrumentation;

    SherlockTransformer(final Project project) {
        this.project = project;
        this.typeClasses = Sets.newHashSet();
        this.typeClasses.add(DefaultContentType.CLASSES);
        this.scopes = ImmutableSet.of(Scope.EXTERNAL_LIBRARIES, Scope.PROJECT, Scope.SUB_PROJECTS);
    }

    @Override
    public String getName() {
        return "SherlockPlugin";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return typeClasses;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return scopes;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    public void transform(TransformInvocation invocation) throws IOException {
        sherlockEnabled = project.getExtensions().getByType(SherlockExtension.class).enabled;
        if (!sherlockEnabled) return;
        Collection<TransformInput> inputs = invocation.getInputs();
        Collection<TransformInput> referencedInputs = invocation.getReferencedInputs();
        TransformOutputProvider outputProvider = invocation.getOutputProvider();
        boolean incremental = invocation.isIncremental();
        this.appExt = (AppExtension)this.project.getExtensions().findByName("android");
        List<URL> runtimeCP = this.buildRuntimeClasspath(inputs, referencedInputs);

        try (URLClassLoader cl = new URLClassLoader(runtimeCP.toArray(new URL[0]), null)) {
            if (!incremental) {
                outputProvider.deleteAll();
            }
            this.instrumentation = new SherlockInstrumentation(cl);
            Iterator iterator = inputs.iterator();

            while (iterator.hasNext()) {
                TransformInput ti = (TransformInput) iterator.next();
                this.transformDirectoryInputs(ti, incremental, outputProvider);
                this.transformJarInputs(ti, incremental, outputProvider);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void transformJarInputs(TransformInput ti, boolean incremental, TransformOutputProvider outputProvider) throws IOException {
        Iterator jars = ti.getJarInputs().iterator();

        while(true) {
            File jar;
            File outDir;
            label233:
            do {
                while(true) {
                    while(jars.hasNext()) {
                        JarInput jarInput = (JarInput) jars.next();
                        jar = jarInput.getFile();
                        String jarName = jar.getName();
                        int lastIndex = jarName.lastIndexOf(46);
                        String uniqueName;
                        String md5;
                        String baseName;
                        if (lastIndex != -1) {
                            md5 = DigestUtils.md5Hex(jar.getPath());
                            baseName = jarName.substring(0, lastIndex);
                            uniqueName = md5 + "-" + baseName;
                        } else {
                            md5 = DigestUtils.md5Hex(jar.getPath());
                            uniqueName = md5 + "-" + jarName;
                        }

                        outDir = outputProvider.getContentLocation(uniqueName, jarInput.getContentTypes(), jarInput
                            .getScopes(), Format.DIRECTORY);
                        boolean doXForm = !incremental || jarInput.getStatus() == Status.ADDED || jarInput
                            .getStatus() == Status.CHANGED;
                        if (doXForm) {
                            if (this.sherlockEnabled) {
                                continue label233;
                            }

                            File outJar = outputProvider.getContentLocation(uniqueName, jarInput.getContentTypes(), jarInput
                                .getScopes(), Format.JAR);
                            if (!outJar.getParentFile().mkdirs() && !outJar.getParentFile().isDirectory()) {
                                String outputDir = String.valueOf(outDir);
                                throw new IOException("Couldn't create transformExt output " + outputDir);
                            }

                            FileInputStream fis = new FileInputStream(jarInput.getFile());

                            try {
                                FileOutputStream fos = new FileOutputStream(outJar);

                                try {
                                    IOUtils.copy(fis, fos);
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    throw t;
                                } finally {
                                    fos.close();
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                                throw e;
                            } finally {
                                fis.close();
                            }
                        } else if (jarInput.getStatus() == Status.REMOVED) {
                            FileUtils.deleteQuietly(outDir);
                        } else {
                            String filename = String.valueOf(jarInput.getFile());
                        }
                    }

                    return;
                }
            } while(!outDir.mkdirs() && !outDir.isDirectory());

            this.instrumentation.instrumentClassesInJar(jar, outDir);
        }
    }

    private void transformDirectoryInputs(TransformInput ti, boolean incremental, TransformOutputProvider outputProvider) throws IOException {
        Iterator dirIterators = ti.getDirectoryInputs().iterator();

        while(true) {
            DirectoryInput di;
            File outDir;
            Map changed;

            do {
                if (!dirIterators.hasNext()) {
                    return;
                }

                di = (DirectoryInput) dirIterators.next();
                String uniqueName = DigestUtils.md5Hex(di.getFile().getPath());
                outDir = outputProvider.getContentLocation(uniqueName, di.getContentTypes(), di.getScopes(), Format.DIRECTORY);
                changed = null;
                if (incremental) {
                    changed = di.getChangedFiles();
                }
            } while(!outDir.mkdirs() && !outDir.isDirectory());

            if (this.sherlockEnabled) {
                instrumentation.instrumentClassesInDir(di.getFile(), 0, String.valueOf(outDir.toString()).concat("/"), changed);
            } else {
                FileUtils.deleteDirectory(outDir);
                FileUtils.moveDirectory(di.getFile(), outDir);
            }
        }
    }

    private List<URL> buildRuntimeClasspath(Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs) {
        ArrayList<File> cp = new ArrayList<>(this.appExt.getBootClasspath());

        for (Object o : Arrays.asList(inputs, referencedInputs)) {
            Collection<TransformInput> tis = (Collection<TransformInput>) o;

            for (TransformInput ti : tis) {
                List<Collection<? extends QualifiedContent>> allQC = Arrays.asList(
                    ti.getDirectoryInputs(),
                    ti.getJarInputs()
                );

                for (Object anAllQC : allQC) {
                    Collection<? extends QualifiedContent> qcs = (Collection) anAllQC;
                    Iterator iterator = qcs.iterator();

                    while (iterator.hasNext()) {
                        QualifiedContent qc = (QualifiedContent) iterator.next();
                        cp.add(qc.getFile());
                    }
                }
            }
        }

        return Lists.transform(cp, file -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

}
