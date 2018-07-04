import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;


public class ParseInt extends AnAction {
    //happens after action performing
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;
        //
        for (Caret c : editor.getCaretModel().getAllCarets()) {
            Document d = editor.getDocument();
            String text = c.getSelectedText();
            final String rep = Sout.wrapText("Integer.parseInt(", text, ")");
            WriteCommandAction.runWriteCommandAction(e.getProject(), () ->
                    d.replaceString(c.getSelectionStart(), c.getSelectionEnd(), rep)
            );
            c.removeSelection();
        }
    }
}
