package com.xin;

import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.gotoByName.ChooseByNameItemProvider;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.ChooseByNameModelEx;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static com.intellij.ide.util.gotoByName.ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY;

/**
 * @author linxixin@cvte.com
 */
public class GotoProjectAction extends GotoActionBase implements DumbAware {
    public static final String ID = "GotoFile";

    @Override
    public void gotoActionPerformed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) return;

        final GotoProjectModel gotoFileModel = new GotoProjectModel(project);
        GotoActionBase.GotoActionCallback<IdeFrameImpl> callback = new GotoActionCallback<IdeFrameImpl>() {

            @Override
            public void elementChosen(final ChooseByNamePopup popup, final Object element) {

                if (element instanceof JFrameNavigate) {
                    ((JFrameNavigate) element).getIdeFrame().toFront();
                    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                        IdeFocusManager.getGlobalInstance().requestFocus(((JFrameNavigate) element).getIdeFrame(), true);
                    });

                }
            }
        };

        boolean mayRequestOpenInCurrentWindow = gotoFileModel.willOpenEditor() && FileEditorManagerEx.getInstanceEx(project).hasSplitOrUndockedWindows();
        Pair<String, Integer> start = getInitialText(true, e);
        ChooseByNamePopup popup = myCreatePopup(project, gotoFileModel, ChooseByNameModelEx.getItemProvider(gotoFileModel, getPsiContext(e)), start.first,
                                                mayRequestOpenInCurrentWindow,
                                                start.second);


        showNavigationPopup(callback, "跳转到projectWindow",
                            popup, false);

    }

    private ChooseByNamePopup myCreatePopup(final Project project,
                                            @NotNull final ChooseByNameModel model,
                                            @NotNull ChooseByNameItemProvider provider,
                                            @Nullable final String predefinedText,
                                            boolean mayRequestOpenInCurrentWindow,
                                            final int initialIndex) {
        final ChooseByNamePopup oldPopup = project == null ? null : project.getUserData(CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY);
        if (oldPopup != null) {
            oldPopup.close(false);
        }
        ChooseByNamePopup newPopup = new ChooseByNamePopup(project, model, provider, oldPopup, predefinedText, mayRequestOpenInCurrentWindow, initialIndex) {
            @Override
            protected void initUI(Callback callback, ModalityState modalityState, boolean allowMultipleSelection) {
                super.initUI(callback, modalityState, allowMultipleSelection);
                myTextField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (myList.getSelectedValue() == EXTRA_ELEM) {
                            return;
                        }
                        if (!e.isAltDown()) {
                            return;
                        }
                        int keyCode = e.getKeyCode();
                        switch (keyCode) {
                            case KeyEvent.VK_RIGHT:
                                JFrameNavigate elementAt = (JFrameNavigate) myList.getModel().getElementAt(getSelectedIndex());
                                ProjectManagerImpl.getInstance().closeProject(elementAt.getIdeFrame().getProject());
                                rebuildList(false);
                                break;
                        }
                    }
                });
            }
        };

        if (project != null) {
            project.putUserData(CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, newPopup);
        }
        return newPopup;
    }
}
