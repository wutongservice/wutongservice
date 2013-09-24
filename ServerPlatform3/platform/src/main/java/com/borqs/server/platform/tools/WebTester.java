package com.borqs.server.platform.tools;


import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.web.AbstractHttpClient;
import com.borqs.server.platform.web.HttpApiClient;
import com.borqs.server.platform.web.HttpClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class WebTester extends JFrame {

    public static void main(String[] args) {
        System.out.println(BASE_DIR);
        WebTester webTester = new WebTester();
        webTester.setVisible(true);

    }

    private static final String BASE_DIR = System.getProperty("user.home") + "/.WebTester";

    private AppConfig config;

    // GUI
    private JComboBox urlComboBox;
    private JLabel profileLabel;
    private JButton goButton;
    private JButton copyUrlButton;
    private JTextArea paramTextArea;
    private JLabel responseCodeLabel;
    private JButton copyRespButton;
    private JTextArea responseTextArea;

    public WebTester() {
        config = AppConfig.load();
        init();
        refreshProfile();
    }

    private void init() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                config.setBounds(getBounds());
                config.save();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                config.setBounds(getBounds());
                config.save();
            }
        });
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                this_onClosed();
            }
        });


        this.setBounds(config.getBounds());

        urlComboBox = new JComboBox(getInitUrls());
        profileLabel = new JLabel("", JLabel.LEFT);
        goButton = new JButton("GO");
        copyUrlButton = new JButton("Copy URL");
        paramTextArea = new JTextArea();
        responseCodeLabel = new JLabel();
        copyRespButton = new JButton("Copy response");
        responseTextArea = new JTextArea();

        // size
        urlComboBox.setPreferredSize(new Dimension(-1, 20));
        profileLabel.setPreferredSize(new Dimension(250, -1));
        paramTextArea.setPreferredSize(new Dimension(250, -1));

        // misc
        urlComboBox.setFont(new Font("Dialog.plain", Font.BOLD, 18));
        urlComboBox.setEditable(true);
        Font codeFont = new Font("DialogInput.plain", Font.BOLD, 15);
        paramTextArea.setFont(codeFont);
        responseTextArea.setFont(codeFont);
        responseTextArea.setEditable(false);


        JPanel rootPane = new JPanel(new BorderLayout());

        // top pane
        JPanel topPane = new JPanel(new BorderLayout());
        topPane.setPreferredSize(new Dimension(-1, 80));
        //topPane.setBackground(Color.BLUE);
        topPane.add(wrapTitleBorder(profileLabel, "Profile info"), BorderLayout.WEST);
        topPane.add(wrapMargin(urlComboBox, 25, 10), BorderLayout.CENTER);
        topPane.add(createButtonsPane(), BorderLayout.EAST);


        // center pane
        JSplitPane centerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerPane.setBorder(BorderFactory.createEtchedBorder());
        centerPane.setLeftComponent(wrapTitleBorder(wrapScroll(paramTextArea), "Parameters"));
        centerPane.setRightComponent(createResponsePanel());

        rootPane.add(topPane, BorderLayout.NORTH);
        rootPane.add(centerPane, BorderLayout.CENTER);
        setContentPane(rootPane);

        // menus
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = createMenu("File", 'F');

        JMenuItem menuItem = createMenuItem("Profiles", 'p', menu);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                profilesMenu_onClick();
            }
        });

        menu.addSeparator();


        menuItem = createMenuItem("Exit", 'x', menu);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitMenu_onClick();
            }
        });

        menuBar.add(menu);

        setJMenuBar(menuBar);

        // events
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goButton_onClick();
            }
        });

        copyUrlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyUrlButton_onClick();
            }
        });

        copyRespButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyRespButton_onClick();
            }
        });

        urlComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    urlOnChanging(e.getItem().toString());
                } else if (e.getStateChange() == ItemEvent.SELECTED) {
                    urlOnChanged(e.getItem().toString());
                }
            }
        });
    }

    private String[] getInitUrls() {
        ArrayList<String> l = new ArrayList<String>();
        l.add("");
        try {
            HashSet<String> set = new HashSet<String>();
            HttpClient client = new HttpClient(config.getCurrentProfile().getNormalizeHost());
            client.setTimeout(1000);
            AbstractHttpClient.Response resp = client.get("$", new HashMap<String, Object>());
            if (resp.getStatusCode() == 200) {
                String lines = resp.getText();
                for (String line : StringUtils.split(lines, "\n")) {
                    String urls = StringUtils.substringAfter(line, " ");
                    Collections.addAll(set, StringHelper.splitArray(urls, "|", true));
                }
            }
            l.addAll(set);
        } catch (Exception ignored) {
            System.err.println("Get url list error");
        }

        Collections.sort(l);
        return l.toArray(new String[l.size()]);
    }

    private void urlOnChanging(String url) {
        if (StringUtils.isNotEmpty(url)) {
            System.out.println("Save params for " + url);
            saveParams(url);
        }
    }

    private void urlOnChanged(String url) {
        if (StringUtils.isNotEmpty(url)) {
            System.out.println("Load params for " + url);
            loadParams(url);
        }
    }

    private void exitMenu_onClick() {
        setVisible(false);
        dispose();
        System.exit(0);
    }

    private void this_onClosed() {
        urlOnChanging(ObjectUtils.toString(urlComboBox.getSelectedItem()));
    }


    void profilesMenu_onClick() {
        ProfileDialog dlg = null;
        try {
            dlg = new ProfileDialog();
            dlg.display();
            config.save();
            refreshProfile();
        } finally {
            if (dlg != null) {
                dlg.dispose();
            }
        }
    }

    private void copyToClipboard(String s) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new StringSelection(s), null);
    }

    private void copyRespButton_onClick() {
        copyToClipboard(ObjectUtils.toString(responseTextArea.getText()));
    }

    private HttpApiClient createClient() {
        Profile prof = config.getCurrentProfile();
        HttpApiClient client = new HttpApiClient();
        client.setTimeout(60 * 1000);

        client.setHost(prof.getNormalizeHost());
        if (prof.appId != 0) {
            client.setAppId(prof.appId);
            client.setAppSecret(prof.appSecret);
        }
        if (StringUtils.isNotBlank(prof.ticket)) {
            client.setTicket(prof.ticket);
        }
        if (StringUtils.isNotBlank(prof.userAgent)) {
            client.setUserAgent(prof.userAgent);
        }
        return client;
    }

    private void goButton_onClick() {
        Params params;
        try {
            params = Params.create(paramTextArea.getText());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Params format error", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        HttpApiClient client = createClient();
        String url = StringUtils.strip(urlComboBox.getSelectedItem().toString());
        AbstractHttpClient.Response resp = null;
        try {
            boolean multipartPost = false;
            for (Object v : params.values()) {
                if (v instanceof File) {
                    multipartPost = true;
                    break;
                }
            }
            if (multipartPost)
                resp = client.multipartPost(url, params);
            else
                resp = client.get(url, params);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Call error: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (resp != null) {
            int code = resp.getStatusCode();
            responseCodeLabel.setText("<html>Status code: <font color=" + (code == 200 ? "blue" : "red") + ">" + Integer.toString(resp.getStatusCode()) + "</font></html>");
            responseTextArea.setText(resp.getText());
        } else {
            responseCodeLabel.setText("<html><font color=red>Error</font></html>");
            responseTextArea.setText("");
        }
    }

    private void copyUrlButton_onClick() {
        JOptionPane.showMessageDialog(this, "Unsupported now", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveParams(String url) {
        String s = paramTextArea.getText();
        if (StringUtils.isNotBlank(s)) {
            String profName = config.getCurrentProfile().name;
            String f = makeParamFileName(profName, url);
            writeFile(f, s);
        }
    }

    private void loadParams(String url) {
        String f = makeParamFileName(config.getCurrentProfile().name, url);
        String s = readFile(f);
        paramTextArea.setText(s);
    }

    private static String makeParamFileName(String profName, String url) {
        String s = "params/" + profName + "__" + StringUtils.replace(url, "/", "_S_") + ".txt";
        return StringUtils.replace(s, ":", "");
    }

    private JPanel createButtonsPane() {
        JPanel pane = new JPanel(new BorderLayout());
        pane.add(wrapMargin(copyUrlButton, 2), BorderLayout.NORTH);
        pane.add(wrapMargin(goButton, 2), BorderLayout.CENTER);
        return pane;
    }

    private JPanel createResponsePanel() {
        JPanel pane = new JPanel(new BorderLayout());
        JPanel topPane = new JPanel(new BorderLayout());
        topPane.add(wrapMargin(responseCodeLabel, 10), BorderLayout.CENTER);
        topPane.add(wrapMargin(copyRespButton, 2), BorderLayout.EAST);
        pane.add(topPane, BorderLayout.NORTH);
        pane.add(wrapTitleBorder(wrapScroll(responseTextArea), "Response"), BorderLayout.CENTER);
        return pane;
    }

    private static JComponent wrapMargin(JComponent comp, int top, int left, int bottom, int right) {
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(top, left, bottom, right);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 1.0;
        pane.add(comp, c);
        return pane;
    }

    private static JComponent wrapMargin(JComponent comp, int topAndBottom, int leftAndRight) {
        return wrapMargin(comp, topAndBottom, leftAndRight, topAndBottom, leftAndRight);
    }

    private static JComponent wrapMargin(JComponent comp, int all) {
        return wrapMargin(comp, all, all, all, all);
    }

    private static JComponent wrapScroll(JComponent comp) {
        return new JScrollPane(comp);
    }

    private static JComponent wrapLabel(JComponent comp, String label) {
        JPanel pane = new JPanel(new BorderLayout());
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(100, -1));
        pane.add(l, BorderLayout.WEST);
        pane.add(comp, BorderLayout.CENTER);
        return pane;
    }

    private static JComponent wrapTitleBorder(JComponent comp, String title) {
        comp.setBorder(BorderFactory.createTitledBorder(title));
        return comp;
    }

    private static JMenu createMenu(String title, char mnemonic) {
        JMenu menu = new JMenu(title);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private static JMenuItem createMenuItem(String title, char mnemonic, JMenu menu) {
        JMenuItem item = new JMenuItem(title);
        item.setMnemonic(mnemonic);
        if (menu != null)
            menu.add(item);
        return item;
    }


    private static final String PROFILE_TEMPLATE = "<html>" +
            "<p><b><font color=blue size=3>%s</font><b/></p>" +
            "<p><font size=2>Ticket:<i>%s</i></font></p>" +
            "</html>";

    private void refreshProfile() {
        Profile prof = config.getCurrentProfile();
        setTitle("GUI Http Test tool (Profile:" + ObjectUtils.toString(prof.name) + ")");
        profileLabel.setText(String.format(PROFILE_TEMPLATE, prof.host, prof.ticket));
    }

    private static String readFile(String file) {
        String path = FilenameUtils.concat(BASE_DIR, file);
        try {
            return FileUtils.readFileToString(new File(path));
        } catch (IOException ignored) {
            System.err.println("Read file error '" + path + "'");
            return "";
        }
    }

    private static void writeFile(String file, String text) {
        String path = FilenameUtils.concat(BASE_DIR, file);
        try {
            String dir = FilenameUtils.getFullPathNoEndSeparator(path);
            FileUtils.forceMkdir(new File(dir));
            FileUtils.writeStringToFile(new File(path), text, Charsets.DEFAULT);
        } catch (IOException e) {
            System.err.println("Write file error '" + path + "'");
        }
    }

    static class AppConfig {
        private static final String FILE = "config.properties";

        private Rectangle bounds;
        private String currentProfileName;
        private java.util.List<Profile> profiles = new ArrayList<Profile>();

        public void setBounds(Rectangle rect) {
            bounds = rect;
        }

        public Rectangle getBounds() {
            return bounds;
        }


        private boolean addProfile(Profile p) {
            for (Profile prof : profiles) {
                if (StringUtils.equals(p.name, prof.name))
                    return false;
            }
            profiles.add(p);
            return true;
        }

        public void removeProfile(String name) {
            Profile p = null;
            for (Profile prof : profiles) {
                if (StringUtils.equals(name, prof.name)) {
                    p = prof;
                    break;
                }
            }
            if (p != null) {
                profiles.remove(p);
            }
        }

        public Profile getCurrentProfile() {
            for (Profile prof : profiles) {
                if (StringUtils.equals(currentProfileName, prof.name))
                    return prof;
            }

            Profile prof = Profile.newDefault();
            addProfile(prof);
            currentProfileName = prof.name;
            return prof;
        }


        public static AppConfig load() {
            AppConfig conf = new AppConfig();
            String s = readFile(FILE);
            if (StringUtils.isEmpty(s)) {
                conf.bounds = new Rectangle(0, 0, 640, 480);
                conf.getCurrentProfile();
                conf.save();
            } else {
                Properties props = new Properties();
                try {
                    props.load(new StringReader(s));
                } catch (IOException ignored) {
                }
                // bounds
                int[] a = StringHelper.splitIntArray(props.getProperty("bounds", "0,0,640,480"), ",");
                Rectangle rect = new Rectangle();
                rect.setLocation(a[0], a[1]);
                rect.setSize(a[2], a[3]);
                conf.bounds = rect;

                // profiles
                for (String key : props.stringPropertyNames()) {
                    if (key.startsWith("profile.")) {
                        String profName = StringUtils.removeStart(key, "profile.");
                        String profVal = props.getProperty(key);
                        Profile prof = Profile.parse(profName, profVal);
                        conf.profiles.add(prof);
                    }
                }

                // currentProfile
                conf.currentProfileName = props.getProperty("currentProfile", "Default");
            }
            return conf;
        }


        public void save() {
            Properties props = new Properties();
            props.setProperty("bounds", String.format("%s,%s,%s,%s", (int) bounds.getX(), (int) bounds.getY(), (int) bounds.getWidth(), (int) bounds.getHeight()));
            props.setProperty("currentProfile", currentProfileName);
            for (Profile prof : profiles) {
                props.setProperty("profile." + prof.name, prof.toString());
            }
            try {
                StringWriter w = new StringWriter();
                props.store(w, "Gui Http Test configuration file");
                writeFile(FILE, w.toString());
            } catch (IOException ignored) {
            }
        }
    }

    static class Profile {
        String name;
        String host;
        String ticket;
        int appId;
        String appSecret;
        String userAgent;

        Profile(String name, String host, String ticket, int appId, String appSecret, String userAgent) {
            this.name = name;
            this.host = host;
            this.ticket = ticket;
            this.appId = appId;
            this.appSecret = appSecret;
            this.userAgent = userAgent;
        }

        public String getNormalizeHost() {
            String host = this.host;
            if (!host.startsWith("http://") || !host.startsWith("https://"))
                host = "http://" + host;
            return host;
        }

        public static Profile newDefault() {
            return new Profile("Default", "apitest.user.com:9995", " ", 0, " ", " ");
        }

        @Override
        public String toString() {
            return String.format("%s|%s|%s|%s|%s", host, ticket, appId, appSecret, userAgent);
        }

        public static Profile parse(String name, String s) {
            String[] ss = StringUtils.split(s, "|");
            return new Profile(name, ss[0], ss[1], Integer.parseInt(ss[2]), ss[3], ss[4]);
        }
    }

    static class Params extends LinkedHashMap<String, Object> {

        public static Params create(String s) throws IOException {
            Params params = new Params();
            Properties p = new Properties();
            p.load(new StringReader(s));
            for (String key : p.stringPropertyNames()) {
                parseParam(params, key, p.getProperty(key));
            }
            return params;
        }


        private static void parseParam(Params params, String key, String val) throws IOException{
            String k = key;
            Object v = val;
            if (StringUtils.contains(key, "#")) {
                k = StringUtils.substringBefore(key, "#");
                String flag = StringUtils.substringAfter(key, "#");
                if (StringUtils.equalsIgnoreCase(flag, "md5hex") || StringUtils.equalsIgnoreCase(flag, "md5"))
                    v = Encoders.md5Hex(val);
                else if (StringUtils.equalsIgnoreCase(flag, "md5base64"))
                    v = Encoders.md5Base64(val);
                else if (StringUtils.equalsIgnoreCase(flag, "base64"))
                    v = Encoders.toBase64(val);
                else if (StringUtils.equalsIgnoreCase(flag, "file"))
                    v = new File(val);
                else
                    throw new IOException("Unknown flag for params " + flag);
            }
            params.put(k, v);
        }
    }

    class ProfileDialog extends JDialog {

        private JTextField hostTextField;
        private JTextField ticketTextField;
        private JTextField appIdTextField;
        private JTextField appSecretTextField;
        private JTextField userAgentField;

        public ProfileDialog() {
            super(WebTester.this, "Profiles", true);
            init();
        }

        void init() {
            setResizable(false);
            setSize(400, 300);
            setLocation(WebTester.this.getX() + 100, WebTester.this.getY() + 100);

            hostTextField = new JTextField();
            ticketTextField = new JTextField();
            appIdTextField = new JTextField();
            appSecretTextField = new JTextField();
            userAgentField = new JTextField();

            JPanel rootPane = new JPanel(new BorderLayout());
            JPanel inputPane = new JPanel(new GridLayout(5, 1, 4, 4));
            inputPane.add(wrapLabel(hostTextField, "Host"));
            inputPane.add(wrapLabel(ticketTextField, "Ticket"));
            inputPane.add(wrapLabel(appIdTextField, "AppID"));
            inputPane.add(wrapLabel(appSecretTextField, "App Secret"));
            inputPane.add(wrapLabel(userAgentField, "User Agent"));
            rootPane.add(wrapMargin(inputPane, 20), BorderLayout.CENTER);

            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
            buttonPanel.add(wrapMargin(okButton, 20));
            buttonPanel.add(wrapMargin(cancelButton, 20));
            rootPane.add(buttonPanel, BorderLayout.SOUTH);
            setContentPane(rootPane);

            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onOK();
                    setVisible(false);
                }
            });
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });

            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    displayParams();
                }
            });
        }

        void displayParams() {
            Profile prof = config.getCurrentProfile();
            hostTextField.setText(prof.host);
            ticketTextField.setText(prof.ticket);
            appIdTextField.setText(Integer.toString(prof.appId));
            appSecretTextField.setText(prof.appSecret);
            userAgentField.setText(prof.userAgent);
        }

        void onOK() {
            Profile prof = config.getCurrentProfile();
            prof.host = stripToBlank(hostTextField.getText());
            prof.ticket = stripToBlank(ticketTextField.getText());
            prof.appId = stripAppId(appIdTextField.getText());
            prof.appSecret = stripToBlank(appSecretTextField.getText());
            prof.userAgent = stripToBlank(userAgentField.getText());
        }

        private String stripToBlank(String s) {
            return StringUtils.isEmpty(s) ? " " : s;
        }

        private int stripAppId(String s) {
            try {
                return Integer.parseInt(StringUtils.strip(s));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void display() {
            setVisible(true);
        }


    }
}
