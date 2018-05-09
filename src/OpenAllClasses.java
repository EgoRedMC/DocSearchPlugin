import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenAllClasses extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            Desktop.getDesktop().browse(new URI("https://docs.oracle.com/javase/7/docs/api/allclasses-noframe.html"));
        } catch (URISyntaxException | IOException ignored) {
        }
    }
}
