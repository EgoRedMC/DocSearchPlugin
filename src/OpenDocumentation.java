import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.IOException;


public class OpenDocumentation extends AnAction {

    private static ArrayList<String> simpleImports;
    private static ArrayList<String> multiImports;

    /**
     * main program logic
     */
    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;
        sortImports(readImports(editor.getDocument()));
        //
        for (Caret c : editor.getCaretModel().getAllCarets()) {
            c.selectWordAtCaret(false);
            openPage(findClass(c.getSelectedText()));
            c.removeSelection();
        }
    }

    /**
     * opens Class documentation in browser
     * if Class == null does nothing
     *
     * @param c Class, which documentation should be opened
     */
    private void openPage(Class c) {
        if (c == null) return;
        String page = "https://docs.oracle.com/javase/7/docs/api/"
                + c.getCanonicalName().replace('.', '/')
                + ".html";
        try {
            Desktop.getDesktop().browse(new URI(page));
        } catch (URISyntaxException | IOException ignored) {
        }
    }

    /**
     * searching class by its name and imports
     *
     * @param word class name
     * @return Class, which exists in imported packages and has this name
     * or null if class with this name does not exists
     */
    private Class findClass(String word) {
        Class res = null;
        for (String imp : simpleImports) {
            if (contains(imp, word)) {
                try {
                    res = Class.forName(imp);
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        if (res == null) {
            for (String imp : multiImports) {
                try {
                    res = Class.forName(imp.substring(0, imp.length() - 1) + word);
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return res;
    }

    /**
     * tells if this import corresponds to that Class name
     *
     * @param imp  current import
     * @param word Class name
     */
    private boolean contains(String imp, String word) {
        return imp.endsWith(word);
    }

    /**
     * splits imports into simple (java.util.ArrayList) and multi (java.io.*)
     *
     * @param imports ArrayList of imports represented as String's
     */
    private void sortImports(ArrayList<String> imports) {
        simpleImports = new ArrayList<>(imports.size() / 4);
        multiImports = new ArrayList<>(imports.size() / 4);
        for (String imp : imports) {
            if (imp.endsWith("*"))
                multiImports.add(imp);
            else
                simpleImports.add(imp);
        }
    }

    /**
     * reads all the imports in input Document
     *
     * @return ArrayList which contains all imports represented as String's
     */
    private ArrayList<String> readImports(Document document) {
        ArrayList<String> imports = new ArrayList<>(16);
        imports.add("java.lang.*");
        //
        String text = document.getText();
        StringTokenizer tokenizer = new StringTokenizer(text, ";");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if ((!line.contains("import ")) && (!line.contains("package"))) {
                if (containsNonWhitespaces(line))
                    break;
                else
                    continue;
            }
            char[] word = new char[line.length() - "import ".length() + 2];
            int index = 0;
            int start;
            if (isComment(line))
                start = startingCommentLengthCount(line);
            else
                start = startingWhitespacesCount(line);
            for (int i = (line.contains("import") ? "import ".length() : "package ".length()) + start; i < line.length(); i++) {
                if (Character.isWhitespace(line.charAt(i)))
                    continue;
                word[index++] = line.charAt(i);
            }
            if (line.contains("package")) {
                word[index++] = '.';
                word[index++] = '*';
            }
            imports.add(new String(word, 0, index));
        }
        return imports;
    }

    private boolean isComment(String line) {
        int a = startingWhitespacesCount(line);
        return line.substring(a, a + 2).equals("//") || line.substring(a, a + 2).equals("/*");
    }

    private int startingCommentLengthCount(String line) {
        for (int i = line.length() - 2; i >= 0; i--) {
            if (line.charAt(i) == '\n') return i;
        }
        return 0;
    }

    /**
     * @return number of whitespaces at the beginning of input String
     */
    private int startingWhitespacesCount(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) return i;
        }
        return 0;
    }

    /**
     * tells if String contains any non-whitespace character
     *
     * @param line String to check
     * @return true if input is null or contains non-whitespace characters
     * otherwise false
     */
    private boolean containsNonWhitespaces(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) return true;
        }
        return false;
    }

    //happens after action performing
    @Override
    public void actionPerformed(AnActionEvent e) {
    }
}
//TODO: если вызов выглядит как java.util.List прямо в строке