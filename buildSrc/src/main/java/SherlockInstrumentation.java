import com.android.build.api.transform.Status;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.io.FileUtils;

public class SherlockInstrumentation {
    ClassLoader classLoader;

    public SherlockInstrumentation(ClassLoader cl) {
        this.classLoader = cl;
    }

    public void instrumentClassesInDir(
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
                        this.instrumentClassesInDir(
                            file,
                            depth,
                            (
                                new StringBuilder(1 +
                                    String.valueOf(outDir).length() +
                                    String.valueOf(fileName).length())
                            ).append(outDir).append(fileName).append("/").toString(),
                            changed
                        );
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

    public void instrumentClassFile(
        final File f,
        String outDir,
        Map<File, Status> changed
    ) throws IOException {
        String filename = String.valueOf(f);
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
        byte[] fileBytes = ByteStreams.toByteArray(is);
        is.close();

        try {
            byte[] out = this.instrument(fileBytes);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(out);
            fos.close();
        } catch (Exception e1) {
            System.out.print("Error here ----------->");
            e1.printStackTrace();
        }

    }

    public void instrumentClassesInJar(File inJarFile, File outDir) throws IOException {
        JarInputStream jis = new JarInputStream(new BufferedInputStream(new FileInputStream(
            inJarFile)));

        JarEntry inEntry;
        while ((inEntry = jis.getNextJarEntry()) != null) {
            String name = inEntry.getName();
            byte[] entryBytes = ByteStreams.toByteArray(jis);
            jis.closeEntry();
            if (name.endsWith(".class") && this.checkIfInstrumentable(name)) {
                try {
                    entryBytes = this.instrument(entryBytes);
                } catch (Exception e) {

                    e.printStackTrace();
                }
            }

            if (!inEntry.isDirectory()) {
                String var8 = String.valueOf(outDir);
                File outFile = new File((
                    new StringBuilder(1 +
                        String.valueOf(var8).length() +
                        String.valueOf(name).length())
                ).append(var8).append("/").append(name).toString());
                outFile.getParentFile().mkdirs();
                outFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(outFile);
                fos.write(entryBytes);
                fos.close();
            }
        }

        jis.close();
    }

    public boolean checkIfInstrumentable(String name) {
        return name.startsWith("okhttp3/Call");
    }

    public byte[] instrument(final byte[] in) {
        SherlockClassWriter cw = new SherlockClassWriter();

        return cw.instrument(in);
    }
}
