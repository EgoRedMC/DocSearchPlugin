import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;

import java.util.HashSet;
import java.util.Objects;

public class Sout extends AnAction {
    //happens after action performing
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;
        //
        HashSet<Integer> lines = new HashSet<>(editor.getCaretModel().getCaretCount());
        for (Caret c : editor.getCaretModel().getAllCarets()) {
            if (lines.contains(c.getLogicalPosition().line))
                continue;
            lines.add(c.getLogicalPosition().line);
            Document d = editor.getDocument();
            selectAllWordsInLine(c);
            String text = c.getSelectedText();
            if (text != null)
                if (text.charAt(text.length() - 1) == ';') {
                    text = text.substring(0, text.length() - 1);
                }
            final String rep = wrapText("System.out.println(", text, ");");
            WriteCommandAction.runWriteCommandAction(e.getProject(), () ->
                    d.replaceString(c.getSelectionStart(), c.getSelectionEnd(), rep)
            );
            c.removeSelection();
        }
    }

    /**
     * function which selects line with caret without whitespaces on both sides
     */
    static void selectAllWordsInLine(Caret c) {
        c.selectLineAtCaret();
        char[] line;
        try {
            line = Objects.requireNonNull(c.getSelectedText()).toCharArray();
        } catch (NullPointerException ignored) {
            c.removeSelection();
            return;
        }
        int start = c.getSelectionStart(), end = c.getSelectionEnd();
        for (int i = 0; i < line.length; i++) {
            if (!Character.isWhitespace(line[i])) {
                start += i;
                break;
            }
        }
        for (int i = 0; i < line.length; i++) {
            if (!Character.isWhitespace(line[line.length - i - 1])) {
                end -= i;
                break;
            }
        }
        c.removeSelection();
        c.setSelection(start, end);
    }

    public static String wrapText(final String s, String text, final String s1) {
        return s + text + s1;
    }
}
