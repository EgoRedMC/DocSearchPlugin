import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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
        if (editor == null)
            return;
        sortImports(readImports(editor.getDocument()));
        //
        for (Caret c : editor.getCaretModel().getAllCarets()) {
            makeSelection(c);
            openPage(findClass(c.getSelectedText()));
            c.removeSelection();
        }
    }

    /**
     * selects word/classname where caret is at the moment
     * !! WARNING: if selecting full classname, it also selects one more character from the left
     */
    private void makeSelection(Caret caret) {
        caret.selectWordAtCaret(false);
        caret.setSelection(
                getStartOffset(caret.getEditor().getDocument().getText(), caret.getSelectionStart()),
                caret.getSelectionEnd());
    }

    private int getStartOffset(String text, int start) {
        int begin = start - 1, res = start;
        boolean lastIsLetter = true;
        //
        try {
            while (true) {
                while (Character.isWhitespace(text.charAt(begin)))
                    begin--;
                if (lastIsLetter) {
                    if (text.charAt(begin) == '.')
                        lastIsLetter = false;
                    else
                        break;
                    begin--;
                } else {
                    if (Character.isLetterOrDigit(text.charAt(begin))) {
                        begin = skipWord(text, begin);
                        res = begin;
                        lastIsLetter = true;
                    } else break;
                }
            }
        } catch (IndexOutOfBoundsException ignored) {
            return 0;
        }
        //
        return res;
    }

    private int skipWord(String text, int begin) {
        while (Character.isLetterOrDigit(text.charAt(begin)))
            begin--;
        return begin;
    }

    /**
     * opens Class documentation in browser
     * if Class == null does nothing
     *
     * @param c Class, which documentation should be opened
     */
    private void openPage(Class c) {
        if (c == null) {
            Notifications.Bus.notify(
                    new Notification("", "DocSearchPlugin warning",
                            "No such class found", NotificationType.WARNING));
            return;
        }
        if (isNotGoodClass(c)) {
            Notifications.Bus.notify(
                    new Notification("", "DocSearchPlugin warning",
                            "No supported documentation for this class found", NotificationType.WARNING));
            return;
        }
        String page = "https://docs.oracle.com/javase/7/docs/api/" +
                c.getCanonicalName().replace('.', '/') + ".html";
        try {
            Desktop.getDesktop().browse(new URI(page));
        } catch (URISyntaxException | IOException ignored) {
        }
    }

    /**
     * tells if class is supported
     */
    @SuppressWarnings("PointlessBooleanExpression")
    private boolean isNotGoodClass(Class c) {
        return ((!c.getPackage().getName().contains("java.") && !c.getPackage().getName().contains("javax.")) ||
                false);//place for increasing available documentation
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
        if (word.contains(".")) {
            try {
                return Class.forName(removeWhitespacesInClassname(word));
            } catch (ClassNotFoundException ignored) {
                return null;
            }
        }
        //
        for (String imp : simpleImports) {
            if (imp.endsWith(word)) {
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
     * removes all whitespaces in classname
     * !! WARNING: also deletes first character (fix for makeSelection() function)
     *
     * @param word classname with whitespaces
     * @return classname without whitespaces
     */
    private String removeWhitespacesInClassname(String word) {
        word = word.substring(1);
        StringBuilder sb = new StringBuilder(word.length());
        for (char c : word.toCharArray())
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        return sb.toString();
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
            if (!line.contains("import ") && !line.contains("package")) {
                if (containsNonWhitespaces(line))
                    break;
                else
                    continue;
            }
            imports.add(removeWhitespacesInImport(line));
        }
        return imports;
    }

    /**
     * put import into right condition
     * Example: "import java.util.       List" -> "java.util.List"
     *
     * @param line line, which contains import
     * @return ready import
     */
    private String removeWhitespacesInImport(String line) {
        char[] word = new char[line.length() - "import ".length() + 2];
        int index = 0;
        int start;
        if (isComment(line))
            start = startingCommentLengthCount(line);
        else
            start = startingWhitespacesCount(line);
        start += (line.contains("import") ? "import ".length() : "package ".length());
        for (int i = start; i < line.length(); i++) {
            if (Character.isWhitespace(line.charAt(i)))
                continue;
            word[index++] = line.charAt(i);
        }
        if (line.contains("package")) {
            word[index++] = '.';
            word[index++] = '*';
        }
        return new String(word, 0, index);
    }

    /**
     * tells if line starts with comment
     *
     * @param line String to check
     * @return true if after whitespaces String has "//" or "/*"
     * false otherwise
     */
    private boolean isComment(String line) {
        return line.trim().startsWith("//") || line.trim().startsWith("/*");
    }

    /**
     * @return number of symbols which are representing comment
     */
    private int startingCommentLengthCount(String line) {
        for (int i = line.length() - 2; i >= 0; i--) {
            if (line.charAt(i) == '\n')
                return i;
        }
        return 0;
    }

    /**
     * @return number of whitespaces at the beginning of input String
     */
    private int startingWhitespacesCount(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i)))
                return i;
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
            if (!Character.isWhitespace(line.charAt(i)))
                return true;
        }
        return false;
    }

    //happens after action performing
    @Override
    public void actionPerformed(AnActionEvent e) {
    }
}