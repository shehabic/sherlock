package com.shehabic.sherlock.plugin;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.ASM6;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * @author shehabic
 */
class SherlockClassWriter {

    byte[] instrument(byte[] contents) {
        ClassReader reader = new ClassReader(contents);
        ClassWriter writer = new ClassWriter(reader, COMPUTE_FRAMES);
        reader.accept(new SherlockClassVisitor(writer), new Attribute[]{}, SKIP_FRAMES);
        return writer.toByteArray();
    }

    public static class ClassInfo {
        org.objectweb.asm.Type type;
        String[] interfaces;

        ClassInfo() { }
    }


    public class SherlockClassVisitor extends ClassVisitor {

        private final ClassInfo mClassInfo = new ClassInfo();

        public void visit(
            int version,
            int access,
            String className,
            String signature,
            String superName,
            String[] interfaces
        ) {
            super.visit(version, access, className, signature, superName, interfaces);

            mClassInfo.type = Type.getObjectType(className);
            mClassInfo.interfaces = interfaces;
        }


        SherlockClassVisitor(ClassVisitor cv) {
            super(ASM6, cv);
            this.cv = cv;
        }

        @Override
        public MethodVisitor visitMethod(
            int access,
            String name,
            String desc,
            String signature,
            String[] exceptions
        ) {
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            return new SherlockMethodVisitor(api, mv, access, name, desc);
        }

        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            super.visitInnerClass(name, outerName, innerName, access);
        }

        public void visitEnd() {
            super.visitEnd();
        }
    }

    static class SherlockMethodVisitor extends AdviceAdapter {

        SherlockMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            switch (name) {
                case "execute":
                    mv.visitMethodInsn(
                        INVOKESTATIC,
                        "com/shehabic/sherlock/interceptors/SherlockOkHttpInterceptor",
                        "execute",
                        "(Lokhttp3/Call;)Lokhttp3/Response;",
                        false
                    );
                    break;
                case "enqueue":
                    mv.visitMethodInsn(
                        INVOKESTATIC,
                        "com/shehabic/sherlock/interceptors/SherlockOkHttpInterceptor",
                        "enqueue",
                        "(Lokhttp3/Call;Lokhttp3/Callback;)V",
                        false
                    );
                    break;
                default:
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    break;
            }
        }
    }
}
