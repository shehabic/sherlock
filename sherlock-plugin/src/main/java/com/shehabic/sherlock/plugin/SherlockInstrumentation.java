package com.shehabic.sherlock.plugin;

import com.android.build.api.transform.Status;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

class SherlockInstrumentation {

    private static final String CLASS_TO_INSTRUMENT = "okhttp3/OkHttpClient$Builder";

    SherlockInstrumentation(ClassLoader cl) {
    }

    void instrumentClassesInDir(
        final File dir,
        int depth,
        String outDir,
        Map<File, Status> changed
    ) throws IOException {
        String[] names = dir.list();
        if (names == null) {
            throw new IOException("File list was unexpectedly null");
        } else {
            for (String dirName : names) {
                if (depth == 0 && dirName.equals("android")) {
                    FileUtils.copyDirectory(dir, new File(outDir));
                } else {
                    File file = new File(dir, dirName);
                    if (file.isDirectory()) {
                        ++depth;
                        String fileName = file.getName();
                        this.instrumentClassesInDir(file, depth, outDir + fileName + "/", changed);
                    } else if (dirName.endsWith(".class")) {
                        String outputDir = String.valueOf(outDir);
                        String fileName = dirName;
                        if (fileName.length() != 0) {
                            outputDir = outputDir.concat(fileName);
                        } else {
                            fileName = outputDir;
                            outputDir = fileName;
                        }

                        this.instrumentClassFile(file, outputDir, changed);
                    }
                }
            }

        }
    }

    private void instrumentClassFile(final File f, String outDir, Map<File, Status> changed) throws IOException {
        File outFile = new File(outDir);
        outFile.getParentFile().mkdirs();
        outFile.createNewFile();
        if (changed != null) {
            Status status = changed.get(f);
            if (status == null || status == Status.NOTCHANGED || status == Status.REMOVED) {
                Files.copy(f, outFile);
                return;
            }
        }

        InputStream is = new BufferedInputStream(new FileInputStream(f));
//        byte[] fileBytes = ByteStreams.toByteArray(is);
        is.close();

        Files.copy(f, outFile);
//        try {
//            byte[] out = this.instrument(fileBytes);
//            FileOutputStream fos = new FileOutputStream(outFile);
//            fos.write(out);
//            fos.close();
//        } catch (Exception e) {
//            System.out.print("[Sherlock Plugin Error]: " + e.getLocalizedMessage());
//            e.printStackTrace();
//            Files.copy(f, outFile);
//        }

    }

    void instrumentClassesInJar(File inJarFile, File outDir) throws IOException {
        JarInputStream jis = new JarInputStream(new BufferedInputStream(new FileInputStream(inJarFile)));

        JarEntry inEntry;
        while ((inEntry = jis.getNextJarEntry()) != null) {
            String name = inEntry.getName();
            byte[] entryBytes = ByteStreams.toByteArray(jis);
            jis.closeEntry();
            if (name.endsWith(".class") && this.isInstrumentable(name)) {
                try {
                    entryBytes = this.instrument(entryBytes);
                } catch (Exception e) {
                    System.out.println("[Sherlock Plugin Error]: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }

            if (!inEntry.isDirectory()) {
                String var8 = String.valueOf(outDir);
                File outFile = new File(var8 + "/" + name);
                outFile.getParentFile().mkdirs();
                outFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(entryBytes);
                fos.close();
            }
        }

        jis.close();
    }

    private boolean isInstrumentable(String name) {
        return name.startsWith(CLASS_TO_INSTRUMENT);
    }

    private byte[] instrument(final byte[] in) {
        SherlockClassWriter cw = new SherlockClassWriter();

        return cw.instrument(in);
    }
}
