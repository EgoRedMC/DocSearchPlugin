import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class OpenDocumentation extends AnAction {

    private static ArrayList<String> simpleImports;
    private static ArrayList<String> multiImports;

    /**
     * main program logic
     */
    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
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
        } catch (Exception ignored) {
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
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        if (res == null) {
            for (String imp : multiImports) {
                boolean correct = true;
                try {
                    res = Class.forName(imp.substring(0, imp.length() - 1) + word);
                } catch (ClassNotFoundException e) {
                    correct = false;
                }
                if (correct)
                    break;
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
        return imp.length() >= word.length() && imp.substring(imp.length() - word.length()).equals(word);
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
            if (imp.charAt(imp.length() - 1) == '*')
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
            if (!line.contains("import ")) {
                if (containsNonWhitespaces(line))
                    break;
                else
                    continue;
            }
            char[] word = new char[line.length() - "import ".length()];
            int index = 0;

            for (int i = "import ".length() + skipWhitespaces(line); i < line.length(); i++) {
                if (line.charAt(i) == ' ') continue;
                word[index++] = line.charAt(i);
            }
            imports.add(new String(word, 0, index));
        }
        return imports;
    }

    /**
     * @return number of whitespaces at the beginning of input String
     */
    private int skipWhitespaces(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) return i;
        }
        return -1;
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