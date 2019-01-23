package com.shehabic.sherlock.plugin;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;

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

    public class SherlockClassVisitor extends ClassVisitor {
        SherlockClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public MethodVisitor visitMethod(
            int access,
            String name,
            String desc,
            String signature,
            String[] exceptions
        ) {
            if (name.equals("build")) {
                System.out.println("Will do the builder here ... ");
                return new SherlockMethodVisitor(super.visitMethod(
                    access,
                    name,
                    desc,
                    signature,
                    exceptions
                ));
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }


    private static class SherlockMethodVisitor extends MethodVisitor {
        SherlockMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        // This method will be called before almost all instructions
        @Override
        public void visitCode() {
            // Puts 'this' on top of the stack. If your method is static just delete it
            visitVarInsn(Opcodes.ALOAD, 0);
            // Takes instance of class "the/full/name/of/your/Class" from top of the stack and put value of field interceptors
            // "Ljava/util/List;" is just internal name of java.util.List
            visitFieldInsn(Opcodes.GETFIELD, "okhttp3/OkHttpClient$Builder", "interceptors", "Ljava/util/List;");
            // Before we call add method of list we have to put target value on top of the stack
            visitTypeInsn(Opcodes.NEW, "com/shehabic/sherlock/interceptors/SherlockOkHttpInterceptor");
            visitInsn(Opcodes.DUP);
            // We have to call classes constructor
            // Internal name of constructor - <init>
            // ()V - signature of method. () - method doesn't have parameters. V - method returns void
            visitMethodInsn(Opcodes.INVOKESPECIAL, "com/shehabic/sherlock/interceptors/SherlockOkHttpInterceptor", "<init>", "()V", false);
            // So on top of the stack we have initialized instance of com/shehabic/sherlock/interceptors/SherlockOkHttpInterceptor
            // Now we can put it into list
            visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
        }
    }
}
