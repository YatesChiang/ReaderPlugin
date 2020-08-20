package com.iamyours.reader.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public class ShowMain extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getProject() != null) {
            // 将项目对象，ToolWindow的id传入，获取控件对象
            ToolWindow toolWindow = ToolWindowManager.getInstance(e.getProject()).getToolWindow("jRun");
            if (toolWindow != null) {
                // 无论当前状态为关闭/打开，进行强制打开ToolWindow
                toolWindow.show(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        }
    }
}
