import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;
import java.io.*;
import java.net.*;
import java.util.*;

public class Compile {
    private static final String COMPILE_OUTPUT_DIR = "output";  

    public static String runCodeAndHandleExceptions(String code) {
        StringBuilder output = new StringBuilder();

        try {
            File outputDir = new File(COMPILE_OUTPUT_DIR);
            if (!outputDir.exists()) outputDir.mkdir();

            File outputFile = new File(outputDir, "Main.class");
            if (outputFile.exists()) outputFile.delete();

            boolean success = compileCode(code);
            if (success) {
                output.append(executeCode(outputFile));
            } else {
                output.append("Compilation failed.");
            }
        } catch (Exception e) {
            output.append(handleException(e));
        }

        return output.toString();
    }

    private static boolean compileCode(String code) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(COMPILE_OUTPUT_DIR)));
            Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(new JavaSourceFromString("Main", code));
            boolean success = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits).call();
            fileManager.close();
            return success;
        } catch (Exception e) {
            return false;
        }
    }

    private static String executeCode(File outputFile) throws Exception {
        StringBuilder output = new StringBuilder();
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{outputFile.getParentFile().toURI().toURL()});
        Class<?> cls = Class.forName("Main", true, classLoader);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream originalOut = System.out;
        
        try {
            System.setOut(ps);
            cls.getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[0]);
            System.out.flush();
            output.append("Program Output: \n").append(baos.toString());
        } catch (Exception e) {
            output.append(handleException(e));
        } finally {
            System.setOut(originalOut);
            ps.close();
            baos.close();
        }

        return output.toString();
    }

    private static String handleException(Exception e) {
        String errorMessage = "Error: " + e.getClass().getSimpleName() + "Message: " + e.getMessage() + "";
        return errorMessage;
    }

    // Inner class for in-memory Java source representation
    static class JavaSourceFromString extends SimpleJavaFileObject {
        private final String code;

        protected JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
