package io.github.dmlloyd.jdkpa;

import static java.lang.constant.ConstantDescs.CD_Object;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import io.github.dmlloyd.classfile.ClassBuilder;
import io.github.dmlloyd.classfile.ClassElement;
import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;
import io.github.dmlloyd.classfile.ClassTransform;
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag;

public final class Main {
    public static void main(String[] args) throws IOException {
        String virtualThreadClass = "java/lang/VirtualThread.class";
        URL url = Thread.class.getResource("VirtualThread.class");
        if (url == null) {
            throw new IllegalStateException("No resource found for VirtualThread.class");
        }
        byte[] bytes;
        try (InputStream is = url.openStream()) {
            bytes = is.readAllBytes();
        }
        ClassFile classFile = ClassFile.of();
        ClassModel virtualThread = classFile.parse(bytes);
        bytes = classFile.transformClass(virtualThread, new ClassTransform() {
            public void accept(final ClassBuilder builder, final ClassElement element) {
                builder.accept(element);
            }

            public void atEnd(final ClassBuilder builder) {
                builder.withField("_attachment", CD_Object, fb -> {
                    fb.withFlags(AccessFlag.SYNTHETIC);
                });
                ClassTransform.super.atEnd(builder);
            }
        });
        // now produce the JAR
        try (OutputStream os = Files.newOutputStream(Path.of("vt-patch.jar"))) {
            try (JarOutputStream jos = new JarOutputStream(os)) {
                JarEntry je = new JarEntry("META-INF/");
                jos.putNextEntry(je);
                jos.closeEntry();
                je = new JarEntry("META-INF/MANIFEST.MF");
                je.setMethod(ZipEntry.STORED);
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
                byte[] manifestBytes;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    manifest.write(baos);
                    manifestBytes = baos.toByteArray();
                }
                je.setMethod(ZipEntry.STORED);
                je.setSize(manifestBytes.length);
                je.setCompressedSize(manifestBytes.length);
                je.setCrc(crc(manifestBytes));
                jos.putNextEntry(je);
                jos.write(manifestBytes);
                jos.closeEntry();
                je = new JarEntry(virtualThreadClass);
                je.setMethod(ZipEntry.STORED);
                je.setSize(bytes.length);
                je.setCompressedSize(bytes.length);
                je.setCrc(crc(bytes));
                jos.putNextEntry(je);
                jos.write(bytes);
                jos.closeEntry();
            }
        }
        System.out.println("Done!");
    }

    private static long crc(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }
}
