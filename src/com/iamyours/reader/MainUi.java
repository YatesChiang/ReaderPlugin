package com.iamyours.reader;

import com.iamyours.reader.util.FileUtil;
import com.iamyours.reader.vo.ChapterVO;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainUi implements ToolWindowFactory, ActionListener, KeyListener {

    @Override
    public void init(ToolWindow window) {

    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public boolean isDoNotActivateOnStart() {
        return false;
    }

    private PersistentState persistentState = PersistentState.getInstance();


    JButton addBtn = new JButton("Open");
    JButton nextBtn = new JButton("Next Step");
    JTextArea textArea = new JTextArea();
    JTextArea fileNameText = new JTextArea();

    JScrollPane chapterPanel = new JScrollPane();
    private ArrayList<ChapterVO> chapterList = new ArrayList<>();
    private String content;
    private int currentIndex = 0;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            // 初始化当前行数

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());    //为Frame窗口设置布局为BorderLayout

            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            fileNameText.setLineWrap(true);
            fileNameText.setWrapStyleWord(true);
            fileNameText.setEditable(false);
            fileNameText.setSize(200, 100);
            Box leftPanel = Box.createHorizontalBox();
            JPanel optPanel = new JPanel();
            optPanel.setLayout(new BorderLayout());
            optPanel.add(addBtn, BorderLayout.NORTH);
            optPanel.add(fileNameText, BorderLayout.CENTER);
            optPanel.setBounds(0, 0, 60, 500);
            leftPanel.add(optPanel);
            textArea.setMargin(new Insets(10, 10, 10, 10));
            addBtn.addActionListener(e -> {
                String path = openFile();
                addBook(path);
            });
            nextBtn.addActionListener(e -> readNext());
            nextBtn.addKeyListener(this);
            textArea.addKeyListener(this);
            initList();

            leftPanel.add(chapterPanel);


            Box box = Box.createHorizontalBox();
            Box leftBox = Box.createVerticalBox();
            leftBox.add(nextBtn);
            leftBox.add(new JLabel("  or press 'N'"));
            leftBox.add(Box.createVerticalGlue());
            box.add(leftBox);
            box.add(textArea);
            mainPanel.add(leftPanel, BorderLayout.WEST);
            mainPanel.add(box, BorderLayout.CENTER);

            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            Content content = contentFactory.createContent(mainPanel, "Debug", false);
            toolWindow.getContentManager().addContent(content);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void readNext() {
        if (currentChapterIndex == -1) currentChapterIndex = 0;
        textArea.setText(readContent());
        checkCurrentChapter();
    }

    private boolean noNeedSelect = false;

    private void checkCurrentChapter() {
        if (currentChapterIndex < chapterList.size() - 2) {
            ChapterVO next = chapterList.get(currentChapterIndex + 1);
            if (currentIndex > next.index) {
                noNeedSelect = true;
                chapterJList.setSelectedIndex(currentChapterIndex + 1);
                chapterJList.ensureIndexIsVisible(chapterJList.getSelectedIndex());
            }
        }
    }

    private int maxLineCount = 4;

    private String readContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLineCount; ) {
            String line = readLine();
            if ("".equals(StringUtils.trim(line))) continue;
            sb.append(line);
            i++;
        }
        return sb.toString();
    }


    private void addBook(String path) {
        if (path == null) return;
        File file = new File(path);
        fileCharset = FileUtil.getFileCharset(file);
        fileNameText.setText(path);
        model.removeAllElements();
        model.addElement("loading chapters...");
        new Thread(() -> {
            try {
                loadChapters(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    DefaultListModel<String> model;
    private int currentChapterIndex = -1;
    private JList chapterJList;

    private void initList() {
        model = new DefaultListModel<>();
        chapterJList = new JList(model);
        chapterJList.addKeyListener(this);
        chapterJList.setFixedCellWidth(220);
        chapterJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chapterJList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = chapterJList.getSelectedIndex();
                if (index != currentChapterIndex) {
                    currentChapterIndex = index;
                    if (noNeedSelect) {
                        noNeedSelect = false;
                        return;
                    }
                    selectChapter();
                }
            }
        });
        chapterPanel = new JScrollPane(chapterJList);
    }

    private void selectChapter() {
        if (currentChapterIndex == -1) return;
        if (currentChapterIndex >= chapterList.size()) return;
        ChapterVO vo = chapterList.get(currentChapterIndex);
        currentIndex = vo.index;
        readNext();
    }

    private String readLine() {
        if (currentIndex + 1 >= content.length()) return "";
        int index = content.indexOf("\n", currentIndex + 1);
        if (index == -1) return "";
        String result = content.substring(currentIndex, index);
        currentIndex = index;
        return result;
    }

    private static String openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(new JFrame());
        if (JFileChooser.APPROVE_OPTION == result) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private static final String CHARSET = "ISO-8859-1";

    private String fileCharset;


    //定时事件
    @Override
    public void actionPerformed(ActionEvent actionEvent) {

    }


    // 查询总行数
    public void loadChapters(File file) throws IOException {
        long t = System.currentTimeMillis();
        content = IOUtils.toString(new FileReader(file));
        Matcher match = chapterPattern.matcher(content);
        chapterList.clear();
        while (match.find()) {
            chapterList.add(new ChapterVO(match.group(1), match.start()));
        }
        long time = System.currentTimeMillis() - t;
        System.out.println("load " + chapterList.size() + " chapters in " + time + "ms");
        SwingUtilities.invokeLater(() -> {
            model.removeAllElements();
            for (ChapterVO c : chapterList) {
                model.addElement(c.name);
            }
        });

    }

    private static Pattern chapterPattern = Pattern.compile("(第\\S+章\\s\\S+\\s)");

    private String getChapterTitle(String line) {
        if (chapterPattern.matcher(line).matches()) return line;
        return null;
    }


    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {


    }

    @Override
    public void keyReleased(KeyEvent e) {
        String str = e.getKeyChar() + "";
        if ("N".equals(str.toUpperCase())) {
            readNext();
        }
    }

}