import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.ui.Messages;

public class OpenDocumentation extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        for (Caret c : editor.getCaretModel().getAllCarets()) {
            c.selectWordAtCaret(false);
            makeMainStuff(c.getSelectedText());
            c.removeSelection();
        }
        System.out.println(2);
    }

    //main logic
    private void makeMainStuff(String word) {

    }

    //happens after action performing
    @Override
    public void actionPerformed(AnActionEvent e) {
    }
}
