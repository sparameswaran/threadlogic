/**
 * Copyright (c) 2012 egross, sabha.
 *
 * ThreadLogic - parses thread dumps and provides analysis/guidance
 * It is based on the popular TDA tool.  Thank you!
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.oracle.ateam.threadlogic;

import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.categories.Category;
import com.oracle.ateam.threadlogic.categories.TreeCategory;
import com.oracle.ateam.threadlogic.dialogs.CustomCategoriesDialog;
import com.oracle.ateam.threadlogic.dialogs.FilterDialog;
import com.oracle.ateam.threadlogic.dialogs.HelpOverviewDialog;
import com.oracle.ateam.threadlogic.dialogs.InfoDialog;
import com.oracle.ateam.threadlogic.dialogs.LongThreadDialog;
import com.oracle.ateam.threadlogic.dialogs.PreferencesDialog;
import com.oracle.ateam.threadlogic.dialogs.SearchDialog;
import com.oracle.ateam.threadlogic.filter.FilterChecker;
import com.oracle.ateam.threadlogic.filter.HealthLevelFilter;
import com.oracle.ateam.threadlogic.jconsole.MBeanDumper;
import com.oracle.ateam.threadlogic.jedit.JEditTextArea;
import com.oracle.ateam.threadlogic.jedit.PopupMenu;
import com.oracle.ateam.threadlogic.parsers.AbstractDumpParser;
import com.oracle.ateam.threadlogic.parsers.DumpParser;
import com.oracle.ateam.threadlogic.parsers.DumpParserFactory;
import com.oracle.ateam.threadlogic.parsers.FallbackParser;
import com.oracle.ateam.threadlogic.utils.AppInfo;
import com.oracle.ateam.threadlogic.utils.Browser;
import com.oracle.ateam.threadlogic.utils.CustomLogger;
import com.oracle.ateam.threadlogic.utils.HistogramTableModel;
import com.oracle.ateam.threadlogic.utils.MonitorComparator;
import com.oracle.ateam.threadlogic.utils.PrefManager;
import com.oracle.ateam.threadlogic.utils.ResourceManager;
import com.oracle.ateam.threadlogic.utils.StatusBar;
import com.oracle.ateam.threadlogic.utils.SwingWorker;
import com.oracle.ateam.threadlogic.utils.TableSorter;
import com.oracle.ateam.threadlogic.utils.ThreadDiffsTableModel;
import com.oracle.ateam.threadlogic.utils.ThreadsTableModel;
import com.oracle.ateam.threadlogic.utils.ThreadsTableModel.ThreadData;
import com.oracle.ateam.threadlogic.utils.ThreadsTableSelectionModel;
import com.oracle.ateam.threadlogic.utils.TipOfDay;
import com.oracle.ateam.threadlogic.utils.TreeRenderer;
import com.oracle.ateam.threadlogic.utils.ViewScrollPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDropEvent;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.io.IOException;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.ItemSelectable;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * main class of the Thread Dump Analyzer. Start using static main method.
 *
 * @author irockel
 */
public class ThreadLogic extends JPanel implements ListSelectionListener, TreeSelectionListener, ActionListener, MenuListener {

  private static JFileChooser fc;
  private static JFileChooser sessionFc;
  private static int DIVIDER_SIZE = 10;
  public static JFrame frame;
  private static String dumpFile;
  private static String defaultTDumpDir;
  private static String loggcFile;
  private static int fontSizeModifier = 0;
  private static ThreadLogic myThreadLogic = null;

  private JEditorPane htmlPane;
  private PopupFactory popupFactory = PopupFactory.getSharedInstance();
  private Popup popup;
  private JToolTip toolTip;
  private JEditTextArea jeditPane;
  public JTree tree;
  protected DefaultTreeModel treeModel;
  private JSplitPane splitPane;
  public JSplitPane topSplitPane;
  protected JSplitPane bottomSplitPane;
  private DumpStore dumpStore;
  private Vector topNodes;
  private ViewScrollPane htmlView;
  private ViewScrollPane tableView;
  private ViewScrollPane dumpView;
  private JTextField filter;
  private JCheckBox checkCase;
  private PreferencesDialog prefsDialog;
  private FilterDialog filterDialog;
  private CustomCategoriesDialog categoriesDialog;
  private JTable histogramTable;
  private JMenuItem showDumpMenuItem;
  private DefaultMutableTreeNode logFile;
  private MainMenu pluginMainMenu;
  private boolean isFoundClassHistogram = false;
  private DropTarget dt = null;
  private DropTarget hdt = null;
  private int dumpCounter;
  boolean runningAsJConsolePlugin;
  boolean runningAsVisualVMPlugin;
  private MBeanDumper mBeanDumper;
  private StatusBar statusBar;
  private SearchDialog searchDialog;
  private static final Vector<File> tempFileList = new Vector<File>();
  // Placeholder to initialize ThreadLogic AppInfo ahead of everything else...
  private static AppInfo appInfoInitializer = new AppInfo();

  private static Logger theLogger = CustomLogger.getLogger(ThreadLogic.class.getSimpleName());

  /*
  static {
    theLogger.addHandler(LogHandler.getHandler());
  }
   *
   */


  /**
   * singleton access method for ThreadLogic
   */
  public static ThreadLogic get(boolean setLF) {
    if (myThreadLogic == null) {
      myThreadLogic = new ThreadLogic(setLF);
    }

    return (myThreadLogic);
  }

  public static ThreadLogic get() {
    if (myThreadLogic == null) {
      myThreadLogic = new ThreadLogic(false);
    }
    return (myThreadLogic);
  }

  /**
   * constructor (needs to be public for plugin)
   */
  public ThreadLogic(boolean setLF) {
    super(new BorderLayout());

    if (System.getProperty("threadlogic.advisories") == null) {
      theLogger.fine("Customized Advisories!!\nUse -Dthreadlogic.advisories=directory... command line argument to pick your own set of custom \nadvisories from a specific directory to be used in ADDITION TO the ones packaged within Threadlogic jar file.\nExtract the AdvisoryMap.xml from com/oracle/ateam/threadlogic/resources package in the jar \nand use that as a template to create custom advisories. Use unique keywords and names to avoid conflicts.\n\tExample:  -Dthreadlogic.advisories=/user/tlogic/advisories\n\n");
    }

    if (System.getProperty("threadlogic.groups") == null) {
      theLogger.fine("Customized Grouping!!\nUse -Dthreadlogic.groups=directory... command line argument to pick your customized set of group \ndefinitions from a specific directory INSTEAD OF ones packaged within Threadlogic jar file. \nExtract the NonWLSGroups.xml & WLSGroups.xml from com/oracle/ateam/threadlogic/resources package \nin the jar and use that as a template to customize or create custom Groups. \nEnsure the names for the files are retained as its needed for forming WLS & NonWLS parent groups.\n\tExample:  -Dthreadlogic.groups=/user/tlogic/groups\n\n");
    }

    if (setLF) {
      // init L&F
      setupLookAndFeel();
    }

    if (myThreadLogic == null) {
      myThreadLogic = this;
    }
  }

  /**
   * constructor (needs to be public for plugin)
   */
  public ThreadLogic(boolean setLF, MBeanDumper mBeanDumper) {
    this(setLF);
    this.mBeanDumper = mBeanDumper;
  }

  public ThreadLogic(boolean setLF, String dumpFile) {
    this(setLF);
    ThreadLogic.dumpFile = dumpFile;
  }

  /**
   * initializes ThreadLogic panel
   */
  public void init() {
    init(false, false);
  }

  /**
   * initializes ThreadLogic panel
   *
   * @param asPlugin
   *          specifies if ThreadLogic is running as plugin
   */
  public void init(boolean asJConsolePlugin, boolean asVisualVMPlugin) {

    // init everything
    System.setProperty("awt.useSystemAAFontSettings", "on");

    // Adding back support for JConsolePlugin to run inside JMC
    runningAsJConsolePlugin = asJConsolePlugin;
    runningAsVisualVMPlugin = asVisualVMPlugin;

    tree = new JTree();
    addTreeListener(tree);

    // Create the HTML viewing pane.
    InputStream is = ThreadLogic.class.getResourceAsStream("doc/welcome.html");

    if (!this.runningAsVisualVMPlugin && !this.runningAsJConsolePlugin) {

      htmlPane = new JEditorPane();
      String welcomeText = parseWelcomeURL(is);
      htmlPane.setContentType("text/html");
      htmlPane.setText(welcomeText);
      toolTip = htmlPane.createToolTip();

      // Enable use of custom set fonts


    } else if (asJConsolePlugin) {
      htmlPane = new JEditorPane("text/html", "<html><body bgcolor=\"ffffff\"><br><br>"
              + "<img border=0 src=\"" + ThreadLogic.class.getResource("icons/logo-threadlogic-banner.png") + "\"><br><br>"
              + "Version: " + AppInfo.getVersion() + " on " + AppInfo.getBuildDate() + "<br>"
              + "<a href=http://java.net/projects/threadlogic>http://java.net/projects/threadlogic</a><br><br><br>"
              + "<i>Press 'Request a Thread Dump' button above to request a thread dump.</i>"
              + "</body></html>");
      toolTip = htmlPane.createToolTip();
    } else {
      htmlPane = new JEditorPane("text/html", "<html><body bgcolor=\"ffffff\"></body></html>");
      toolTip = htmlPane.createToolTip();
    }

    htmlPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    htmlPane.setFont(new Font("Arial", Font.BOLD, 13));
    htmlPane.setEditable(false);

    if (!asJConsolePlugin && !asVisualVMPlugin) {
      hdt = new DropTarget(htmlPane, new FileDropTargetListener());
    }

    JEditorPane emptyPane = new JEditorPane("text/html", "");
    emptyPane.setEditable(false);

    htmlPane.addHyperlinkListener(new HyperlinkListener() {

      public void hyperlinkUpdate(final HyperlinkEvent evt) {
        // if a link was clicked
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          if (evt.getDescription().startsWith("monitor")) {
            navigateToMonitor(evt.getDescription());
          } else if (evt.getDescription().startsWith("dump")) {
            navigateToDump();
          } else if (evt.getDescription().startsWith("wait")) {
            navigateToChild("Threads waiting");
          } else if (evt.getDescription().startsWith("sleep")) {
            navigateToChild("Threads sleeping");
          } else if (evt.getDescription().startsWith("dead")) {
            navigateToChild("Deadlocks");
          } else if (evt.getDescription().startsWith("threadgroups")) {
            navigateToChild("Thread Groups Summary");
          } else if (evt.getDescription().startsWith("openlogfile") && !evt.getDescription().endsWith("//")) {
            File[] files = {new File(evt.getDescription().substring(14))};
            openFiles(files, false);
          } else if (evt.getDescription().startsWith("openlogfile")) {
            chooseFile();
          } else if (evt.getDescription().startsWith("opensession") && !evt.getDescription().endsWith("//")) {
            File file = new File(evt.getDescription().substring(14));
            openSession(file, true);
          } else if (evt.getDescription().startsWith("opensession")) {
            openSession();
          } else if (evt.getDescription().startsWith("preferences")) {
            showPreferencesDialog();
          } else if (evt.getDescription().startsWith("filters")) {
            showFilterDialog();
          } else if (evt.getDescription().startsWith("categories")) {
            showCategoriesDialog();
          } else if (evt.getDescription().startsWith("overview")) {
            showHelp();
          } else if (evt.getURL() != null) {
            try {
              // launch a browser with the appropriate URL
              Browser.open(evt.getURL().toString());
            } catch (InterruptedException e) {
              theLogger.warning("Error launching external browser.");
            } catch (IOException e) {
              theLogger.warning("I/O error launching external browser." + e.getMessage());
              e.printStackTrace();
            }
          }
        } else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
          if (evt.getDescription().startsWith("advisory")) {
            // SwingUtilities.getWindowAncestor(htmlPane).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String advisory = evt.getDescription().substring("advisory://".length());
            ThreadAdvisory sa = ThreadAdvisory.lookupThreadAdvisory(advisory);
            toolTip.setTipText(getAdvisoryDetails(advisory));
            toolTip.setBackground(sa.getHealth().getColor());
            Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
            // SwingUtilities.convertPointFromScreen(mouseLocation, htmlPane);
            popup = popupFactory.getPopup(htmlPane, toolTip, mouseLocation.x + 10, mouseLocation.y + 10);
            popup.show();
          }
        } else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
          if (evt.getDescription().startsWith("advisory")) {
            if (popup != null) {
              popup.hide();
            }
          }
        }
      }
    });

    htmlView = new ViewScrollPane(htmlPane, runningAsVisualVMPlugin);
    ViewScrollPane emptyView = new ViewScrollPane(emptyPane, runningAsVisualVMPlugin);

    // create the top split pane
    topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    topSplitPane.setLeftComponent(emptyView);
    topSplitPane.setDividerSize(DIVIDER_SIZE);
    topSplitPane.setContinuousLayout(true);
    topSplitPane.setOneTouchExpandable(true);

    // Add the scroll panes to a split pane.
    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPane.setBottomComponent(htmlView);
    splitPane.setTopComponent(topSplitPane);
    splitPane.setDividerSize(DIVIDER_SIZE);
    splitPane.setContinuousLayout(true);

    if (this.runningAsVisualVMPlugin) {
      setOpaque(true);
      setBackground(Color.WHITE);
      setBorder(BorderFactory.createEmptyBorder(6, 0, 3, 0));
      topSplitPane.setBorder(BorderFactory.createEmptyBorder());
      topSplitPane.setOpaque(false);
      topSplitPane.setBackground(Color.WHITE);
      htmlPane.setBorder(BorderFactory.createEmptyBorder());
      htmlPane.setOpaque(false);
      htmlPane.setBackground(Color.WHITE);
      splitPane.setBorder(BorderFactory.createEmptyBorder());
      splitPane.setOpaque(false);
      splitPane.setBackground(Color.WHITE);
    }

    Dimension minimumSize = new Dimension(200, 50);
    htmlView.setMinimumSize(minimumSize);
    emptyView.setMinimumSize(minimumSize);

    // Add the split pane to this panel.
    add(htmlView, BorderLayout.CENTER);

    statusBar = new StatusBar(!(asJConsolePlugin || asVisualVMPlugin));
    add(statusBar, BorderLayout.SOUTH);

    firstFile = true;
    setFileOpen(false);

    if (!runningAsVisualVMPlugin) {
      setShowToolbar(PrefManager.get().getShowToolbar());
    }

    if (firstFile) {
      // init filechooser
      fc = new JFileChooser();
      fc.setMultiSelectionEnabled(true);
      fc.setCurrentDirectory(PrefManager.get().getSelectedPath());
    }
  }

  private String getAdvisoryDetails(String advisory) {
    ThreadAdvisory ta = ThreadAdvisory.lookupThreadAdvisory(advisory);
    StringBuffer sb = new StringBuffer();

    //Derek Kam:  Fixed to show multiple keywords if it is available.
    StringBuffer keywordlist = new StringBuffer();
    if (ta.getKeywordList().length>0){
    	keywordlist.append( ta.getKeywordList()[0] );
	    for (int i = 1; i < ta.getKeywordList().length; i++) {
	    	keywordlist.append( ", " );
	    	keywordlist.append( ta.getKeywordList()[i] );
	    }
    }
    sb.append("<html><font face=System size=-1>Advisory: ").append(ta.getPattern());
    sb.append("<br>&nbsp;&nbsp;&nbsp;Keyword: ").append(keywordlist.toString());
    sb.append("<br>&nbsp;&nbsp;&nbsp;Denotes: ").append(ta.getDescrp());
    sb.append("<br>&nbsp;&nbsp;&nbsp;HealthLevel: ").append(ta.getHealth());
    sb.append("<br>&nbsp;&nbsp;&nbsp;Suggested Advice: ").append(ta.getAdvice());
    sb.append("</font></html>");
    return sb.toString();
  }

  private void getLogfileFromClipboard() {
    Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    String text = null;

    try {
      if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        text = (String) t.getTransferData(DataFlavor.stringFlavor);
        System.out.println("Got text: " + text);
      }
    } catch (UnsupportedFlavorException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      System.out.println("Got Exception: " + ex);
      theLogger.warning("Error with reading from clipboard: " + ex.getMessage());
      ex.printStackTrace();
    }

    if (text != null) {
      if (firstFile || topNodes == null ) {
        initDumpDisplay(text);
        firstFile = false;
      } else {
        // root nodes are moved down.
        setRootNodeLevel(1);
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        String dumpDate = sdfDate.format(new Date());

        System.out.println("Calling addDumpStreamForClipboard");

        addDumpStreamForClipboard(text, "Clipboard at " + dumpDate);
        /*
        addDumpStream(new ByteArrayInputStream(text.getBytes()),
        "Clipboard at " + new Date(System.currentTimeMillis()), false);
        addToLogfile(text);
         *
         */
        if (this.getRootPane() != null) {
          System.out.println("Calling revalidate");
          this.getRootPane().revalidate();
        }
        System.out.println("Calling displayContent");
        displayContent(null);
      }

      if (!this.runningAsVisualVMPlugin) {
        getMainMenu().getExpandButton().setEnabled(true);
        getMainMenu().getCollapseButton().setEnabled(true);
      }
    }
  }

  private String parseWelcomeURL(InputStream is) {
	LineNumberReader br = null;
    String resultString = null;

    StringBuffer result = new StringBuffer();

    try {
      br = new LineNumberReader(new InputStreamReader(is));
      while (br.ready()) {
        result.append(br.readLine());
        result.append("\n");
      }
      resultString = result.toString();
      resultString = resultString.replaceFirst("./important.png", ThreadLogic.class.getResource("doc/important.png").toString());
      resultString = resultString.replaceFirst("./logo-threadlogic.png", ThreadLogic.class.getResource("doc/logo-threadlogic-banner.png").toString());
      resultString = resultString.replaceFirst("./fileopen.png", ThreadLogic.class.getResource("doc/fileopen.png").toString());
      resultString = resultString.replaceFirst("./settings.png", ThreadLogic.class.getResource("doc/settings.png").toString());
      resultString = resultString.replaceFirst("./help.png", ThreadLogic.class.getResource("doc/help.png").toString());
      resultString = resultString.replaceFirst("<!-- ##tipofday## -->", TipOfDay.getTipOfDay());
      resultString = resultString.replaceFirst("<!-- ##recentlogfiles## -->",
              getAsTable("openlogfile://", PrefManager.get().getRecentFiles()));
      resultString = resultString.replaceFirst("<!-- ##recentsessions## -->",
              getAsTable("opensession://", PrefManager.get().getRecentSessions()));
    } catch (IllegalArgumentException ex) {
      // hack to prevent crashing of the app because off unparsed replacer.
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      try {
        if (br != null) {
          br.close();
          is.close();
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
    // remove unparsed replacers.
    resultString = resultString.replaceFirst("<!-- ##tipofday## -->", "");
    resultString = resultString.replaceFirst("<!-- ##recentlogfiles## -->", "");
    resultString = resultString.replaceFirst("<!-- ##recentsessions## -->", "");
    return (resultString);
  }

  /**
   * convert the given elements into a href-table to be included into the
   * welcome page. Only last four elements are taken.
   *
   * @param prefix
   *          link prefix to use
   * @param elements
   *          list of elements.
   * @return given elements as table.
   */
  private String getAsTable(String prefix, String[] elements) {
    StringBuffer result = new StringBuffer();
    int from = elements.length > 4 ? elements.length - 4 : 0;

    for (int i = from; i < elements.length; i++) {
      if (elements[i].trim().length() > 0) {
        // remove backslashes as they confuse the html display.
        String elem = elements[i].replaceAll("\\\\", "/");
        result.append("<tr><td width=\"20px\"></td><td><a href=\"");
        result.append(prefix);
        result.append(elem);
        result.append("\">");
        result.append(cutLink(elem, 80));
        result.append("</a></td></tr>\n");
      }
    }

    return (result.toString());
  }

  /**
   * cut the given link string to the specified length + three dots.
   *
   * @param link
   * @param len
   * @return cut link or original link if link.length() <= len
   */
  private String cutLink(String link, int len) {
    if (link.length() > len) {
      String cut = link.substring(0, len / 2) + "..." + link.substring(link.length() - (len / 2));
      return (cut);
    }

    return (link);
  }

  /**
   * request jmx dump
   */
  public LogFileContent addMXBeanDump() {

    String dump = mBeanDumper.threadDump();
    String locks = mBeanDumper.findDeadlock();
    String serverInfo = mBeanDumper.getMBeanServerInfo();
    String dumpDate = mBeanDumper.getDumpDate();

    // if deadlocks were found, append them to dump output.
    if (locks != null && !"".equals(locks)) {
      dump += "\n" + locks;
    }

    if (firstFile || topNodes == null) {
      initDumpDisplay(null);
      firstFile = false;
    } else {
      // root nodes are moved down.
      setRootNodeLevel(1);
    }

    addDumpStreamForClipboard(dump, "JMX Thread Dump of " + serverInfo + " at " + dumpDate);

    dumpCounter++;
    LogFileContent lfc = addToLogfile(dump);

    if (this.getRootPane() != null) {
      this.getRootPane().revalidate();
    }
    tree.setShowsRootHandles(false);
    displayContent(null);

    if (!this.runningAsVisualVMPlugin) {
      getMainMenu().getExpandButton().setEnabled(true);
      getMainMenu().getCollapseButton().setEnabled(true);
    }
    return (lfc);
  }

  private LogFileContent addToLogfile(String dump) {
    try {
      InputStream bis = createTempFileFromClipboard(dump.getBytes());
    } catch (Exception e) {
      theLogger.warning("Error in creating temporary file with clipboard content:" + e.getMessage());
    }

    ((LogFileContent) logFile.getUserObject()).appendToContentBuffer(dump);
    return (((LogFileContent) logFile.getUserObject()));
  }

  /**
   * create file filter for session files.
   *
   * @return file filter instance.
   */
  private static FileFilter getSessionFilter() {
    FileFilter filter = new FileFilter() {

      public boolean accept(File arg0) {
        return (arg0 != null && (arg0.isDirectory() || arg0.getName().endsWith("tsf")));
      }

      public String getDescription() {
        return ("ThreadLogic Session Files");
      }
    };
    return (filter);
  }

  /**
   * initializes session file chooser, if not already done.
   */
  private static void initSessionFc() {

    sessionFc = new JFileChooser();
    sessionFc.setMultiSelectionEnabled(true);
    sessionFc.setCurrentDirectory(PrefManager.get().getSelectedPath());
    if ((PrefManager.get().getPreferredSizeFileChooser().height > 0)) {
      sessionFc.setPreferredSize(PrefManager.get().getPreferredSizeFileChooser());
    }
    sessionFc.setFileFilter(getSessionFilter());

    sessionFc.setSelectedFile(null);
  }

  /**
   * expand all dump nodes in the root tree
   *
   * @param expand
   *          true=expand, false=collapse.
   */
  public void expandAllDumpNodes(boolean expand) {
    TreeNode root = (TreeNode) tree.getModel().getRoot();
    expandAll(tree, new TreePath(root), expand);
  }

  /**
   * expand all nodes of the currently selected category, only works for tree
   * categories.
   */
  private void expandAllCatNodes(boolean expand) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
    JTree catTree = (JTree) ((TreeCategory) node.getUserObject()).getCatComponent(this);
    if (expand) {
      for (int i = 0; i < catTree.getRowCount(); i++) {
        catTree.expandRow(i);
      }
    } else {
      for (int i = 0; i < catTree.getRowCount(); i++) {
        catTree.collapseRow(i);
      }
    }
  }

  /**
   * show help dialog.
   */
  private void showHelp() {
    HelpViewer.show(getFrame());
  }

  /**
   * sort monitors by thread amount
   */
  private void sortCatByThreads() {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
    ((TreeCategory) node.getUserObject()).sort(new MonitorComparator());
    displayCategory(node.getUserObject());
  }

  /**
   * expand or collapse all nodes of the specified tree
   *
   * @param tree
   *          the tree to expand all/collapse all
   * @param parent
   *          the parent to start with
   * @param expand
   *          expand=true, collapse=false
   */
  private void expandAll(JTree catTree, TreePath parent, boolean expand) {
    // Traverse children
    TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      for (Enumeration e = node.children(); e.hasMoreElements();) {
        TreeNode n = (TreeNode) e.nextElement();
        TreePath path = parent.pathByAddingChild(n);
        expandAll(catTree, path, expand);
      }
    }

    if (parent.getPathCount() > 1) {
      // Expansion or collapse must be done bottom-up
      if (expand) {
        catTree.expandPath(parent);
      } else {
        catTree.collapsePath(parent);
      }
    }
  }

  private void saveSession() {
    initSessionFc();
    int returnVal = sessionFc.showSaveDialog(this.getRootPane());
    sessionFc.setPreferredSize(sessionFc.getSize());

    PrefManager.get().setPreferredSizeFileChooser(sessionFc.getSize());

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = sessionFc.getSelectedFile();
      // check if file has a suffix
      if (file.getName().indexOf(".") < 0) {
        file = new File(file.getAbsolutePath() + ".tsf");
      }
      int selectValue = 0;
      if (file.exists()) {
        Object[] options = {"Overwrite", "Cancel"};
        selectValue = JOptionPane.showOptionDialog(null,
                "<html><body>File exists<br><b>" + file + "</b></body></html>", "Confirm overwrite",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
      }
      if (selectValue == 0) {
        ObjectOutputStream oos = null;
        try {
          oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));

          oos.writeObject(dumpFile);
          oos.writeObject(topNodes);
          oos.writeObject(dumpStore);
        } catch (IOException ex) {
          ex.printStackTrace();
        } finally {
          try {
            oos.close();
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
        PrefManager.get().addToRecentSessions(file.getAbsolutePath());
      }
    }
  }

  private void openSession() {
    initSessionFc();

    int returnVal = sessionFc.showOpenDialog(this.getRootPane());
    sessionFc.setPreferredSize(sessionFc.getSize());
    PrefManager.get().setPreferredSizeFileChooser(sessionFc.getSize());

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = sessionFc.getSelectedFile();
      int selectValue = 0;
      if ((selectValue == 0) && (file.exists())) {
        openSession(file, false);
      }
    }
  }

  /**
   * open the specified session
   *
   * @param file
   */
  private void openSession(File file, boolean isRecent) {
    try {
      loadSession(file, isRecent);
    } catch (FileNotFoundException ex) {
      JOptionPane.showMessageDialog(this.getRootPane(), "Error opening " + ex.getMessage() + ".",
              "Error opening session", JOptionPane.ERROR_MESSAGE);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this.getRootPane(), "Error opening " + ex.getMessage() + ".",
              "Error opening session", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void loadSession(File file, boolean isRecent) throws IOException {
    final ObjectInputStream ois = new ObjectInputStream(new ProgressMonitorInputStream(this, "Opening session " + file,
            new GZIPInputStream(new FileInputStream(file))));

    resetMainPanel();
    setFileOpen(true);
    firstFile = false;
    initDumpDisplay(null);


    final SwingWorker worker = new SwingWorker() {

      public Object construct() {
        synchronized (syncObject) {
          try {
            dumpFile = (String) ois.readObject();
            topNodes = (Vector) ois.readObject();
            dumpStore = (DumpStore) ois.readObject();
            ois.close();
          } catch (IOException ex) {
            ex.printStackTrace();
          } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
          } finally {
            try {
              ois.close();
            } catch (IOException ex) {
              ex.printStackTrace();
            }
          }
          createTree();
        }

        return null;
      }
    };
    worker.start();
    if (!isRecent) {
      PrefManager.get().addToRecentSessions(file.getAbsolutePath());
    }
  }

  private void setShowToolbar(boolean state) {
    if (state) {
      add(getMainMenu().getToolBar(), BorderLayout.PAGE_START);
    } else {
      remove(getMainMenu().getToolBar());
    }
    revalidate();
    PrefManager.get().setShowToolbar(state);
  }

  /**
   * tries the native look and feel on mac and windows and metal on unix (gtk
   * still isn't looking that nice, even in 1.6)
   */
  private void setupLookAndFeel() {
    try {
      // --- set the desired preconfigured plaf ---
      UIManager.LookAndFeelInfo currentLAFI = null;

      // retrieve plaf param.
      String plaf = "Nimbus,Mac,Windows,Metal";

      if (PrefManager.get().isUseGTKLF()) {
        plaf = "Nimbus,GTK,Mac,Windows,Metal";
      }

      // this line needs to be implemented in order to make L&F work properly
      UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());

      // query list of L&Fs
      UIManager.LookAndFeelInfo[] plafs = UIManager.getInstalledLookAndFeels();

      if ((plaf != null) && (!"".equals(plaf))) {

        String[] instPlafs = plaf.split(",");
        search:
        for (int i = 0; i < instPlafs.length; i++) {
          for (int j = 0; j < plafs.length; j++) {
            currentLAFI = plafs[j];
            if (currentLAFI.getName().startsWith(instPlafs[i])) {
              UIManager.setLookAndFeel(currentLAFI.getClassName());
              // setup font
              setUIFont(new FontUIResource("SansSerif", Font.PLAIN, 11));
              break search;
            }
          }
        }
      }

      if (plaf.startsWith("GTK")) {
        setFontSizeModifier(2);
      }

    } catch (Exception except) {
      // setup font
      setUIFont(new FontUIResource("SansSerif", Font.PLAIN, 11));
    }
  }

  private String getInfoText() {
    StringBuffer info = new StringBuffer("<html><body bgcolor=\"ffffff\"><font face=\"System\" size=+2><b>");
    info.append("<img border=0 src=\"" + ThreadLogic.class.getResource("icons/ThreadLogic.gif") + "\">" + AppInfo.getAppInfo());
    info.append("</b></font><hr fgcolor=\"#cccccc\"><font face=\"System\"><p>");
    info.append("(C)opyright ");
    info.append(AppInfo.getCopyright());
    info.append(" - Ingo Rockel<br>");
    info.append("Version: <b>");
    info.append(AppInfo.getVersion());
    info.append("</b><p>");

    if (runningAsJConsolePlugin || runningAsVisualVMPlugin) {
      info.append("<a href=\"threaddump://\">Request Thread Dump...</a>");
    } else {
      info.append("Select File/Open to open your log file with thread dumps to start analyzing these thread dumps.<p>See Help/Overview for information on how to obtain a thread dump from your VM.</p></font></body></html>");
    }
    return (info.toString());
  }

  /**
   * init the basic display for showing dumps
   *
   * @param content
   *          initial logfile content may also be parsed, can also be null. only
   *          used for clipboard operations.
   */
  public void initDumpDisplay(String content) {
    // clear tree
    dumpStore = new DumpStore();

    topNodes = new Vector();

    if (!runningAsVisualVMPlugin) { // && !runningAsJConsolePlugin ) {
      //getMainMenu().getLongMenuItem().setEnabled(true);
      getMainMenu().getSaveSessionMenuItem().setEnabled(true);
      getMainMenu().getExpandButton().setEnabled(true);
      getMainMenu().getCollapseButton().setEnabled(true);
      //getMainMenu().getFindLRThreadsToolBarButton().setEnabled(true);
      getMainMenu().getCloseAllMenuItem().setEnabled(true);
      getMainMenu().getExpandAllMenuItem().setEnabled(true);
      getMainMenu().getCollapseAllMenuItem().setEnabled(true);
    }

    //if (!runningAsJConsolePlugin && (dumpFile != null)) {
    if ((dumpFile != null)) {
      addDumpFile();
    } else if (content != null) {
      SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
      String dumpDate = sdfDate.format(new Date());
      addDumpStreamForClipboard(content, "Clipboard at " + dumpDate);
      /*
      addDumpStream(new ByteArrayInputStream(content.getBytes()),
      "Clipboard at " + new Date(System.currentTimeMillis()), false);
      addToLogfile(content);
       *
       */
    }

    if (runningAsJConsolePlugin || runningAsVisualVMPlugin || isFileOpen()) {
      if (topSplitPane.getDividerLocation() <= 0) {
        topSplitPane.setDividerLocation(200);
      }

      // change from html view to split pane
      remove(0);
      revalidate();
      htmlPane.setText("");
      splitPane.setBottomComponent(htmlView);
      add(splitPane, BorderLayout.CENTER);
      if (PrefManager.get().getDividerPos() > 0) {
        splitPane.setDividerLocation(PrefManager.get().getDividerPos());
      } else {
        // set default divider location
        splitPane.setDividerLocation(100);
      }
      revalidate();
    }
  }

  /**
   * add the set dumpFileStream to the tree
   */
  private void addDumpFile() {
    addDumpFile(dumpFile);
  }

  /**
   * add the set dumpFileStream to the tree
   */
  public void addDumpFile(String filePath) {
    String[] file = new String[1];
    file[0] = filePath;
    addDumpFiles(file);
  }

  private boolean isLogfileSizeOk(String fileName) {
    File file = new File(fileName);
    return (file.isFile() && ((PrefManager.get().getMaxLogfileSize() == 0) || (file.length() <= (PrefManager.get().getMaxLogfileSize() * 1024))));
  }
  /**
   * sync object is needed to synchronize opening of multiple files.
   */
  private static Object syncObject = new Object();

  /**
   * add the set dumpFileStream to the tree
   */
  private void addDumpFiles(String[] files) {
    for (int i = 0; i < files.length; i++) {

      dumpCounter = 1;
      try {
        addDumpStream(new BufferedInputStream(new FileInputStream(files[i])), files[i], true);
      } catch (IOException ex) {

        JOptionPane.showMessageDialog(this.getRootPane(), "Error opening " + ex.getMessage() + ".",
                "Error opening file", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void addDumpStreamForClipboard(String content, String msg) {

    try {
      InputStream bis = createTempFileFromClipboard(content.getBytes());

      addDumpStream(bis, msg, false);
    } catch (IOException ex) {

      JOptionPane.showMessageDialog(this.getRootPane(), "Error reading from Clipboard content: " + ex.getMessage() + ".",
              "Error reading clipboard content", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void addDumpStream(InputStream inputStream, String file, boolean withLogfile) {
    final InputStream parseFileStream = new ProgressMonitorInputStream(this, "Parsing " + file, inputStream);

    Logfile logFileInstance = new Logfile(file);
    // Save the reference to the newly created dump file for clipboard content within logFileInstance
    if (!withLogfile) {
      logFileInstance.setTempFileLocation(dumpFile);
    }

    //Create the nodes.
    // if(!runningAsJConsolePlugin || topNodes.size() == 0) {
    topNodes.add(new DefaultMutableTreeNode(logFileInstance));

    final DefaultMutableTreeNode top = (DefaultMutableTreeNode) topNodes.get(topNodes.size() - 1);
    logFile = new DefaultMutableTreeNode(new LogFileContent(file));
    setFileOpen(true);

    /*
    if ((!withLogfile && logFile == null) || isLogfileSizeOk(file)) {
    logFile = new DefaultMutableTreeNode(new LogFileContent(file));
    }
    setFileOpen(true);
     *
     */


    final SwingWorker worker = new SwingWorker() {

      public Object construct() {
        synchronized (syncObject) {
          int divider = topSplitPane.getDividerLocation();
          addThreadDumps(top, parseFileStream);
          createTree();
          tree.expandRow(1);

          topSplitPane.setDividerLocation(divider);
        }

        return null;
      }
    };
    worker.start();
  }

  public void createTree() {
    if (topNodes.size() == 1) {
      treeModel = new DefaultTreeModel((DefaultMutableTreeNode) topNodes.get(0));
      tree = new JTree(treeModel);
      tree.setRootVisible(!runningAsVisualVMPlugin);
      addTreeListener(tree);
      // Don't change the title, let it stay the same...ThreadLogic byline
      /*
      if (runningAsJConsolePlugin) {
        // When running as plugin, we cannot expect JFrame, just Awt frame
        getAwtFrame().setTitle("ThreadLogic - Thread Dumps of " + dumpFile);
      } else if (!runningAsVisualVMPlugin) {
        getFrame().setTitle("ThreadLogic - Thread Dumps of " + dumpFile);
      }
       *
       */
    } else {
      DefaultMutableTreeNode root = new DefaultMutableTreeNode("Thread Dump Nodes");
      treeModel = new DefaultTreeModel(root);
      for (int i = 0; i < topNodes.size(); i++) {
        root.add((DefaultMutableTreeNode) topNodes.get(i));
      }
      tree = new JTree(root);
      tree.setRootVisible(false);
      addTreeListener(tree);
      /*
      if (!runningAsVisualVMPlugin && !runningAsJConsolePlugin) {
        if (!frame.getTitle().endsWith("...")) {
          frame.setTitle(frame.getTitle() + " ...");
        }
      }
       *
       */
    }

    tree.setShowsRootHandles(true);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

    tree.setCellRenderer(new TreeRenderer());

    // Create the scroll pane and add the tree to it.
    ViewScrollPane treeView = new ViewScrollPane(tree, runningAsVisualVMPlugin);

    topSplitPane.setLeftComponent(treeView);

    Dimension minimumSize = new Dimension(200, 150);
    treeView.setMinimumSize(minimumSize);

    // Listen for when the selection changes.
    tree.addTreeSelectionListener(this);

    if (!runningAsVisualVMPlugin) {
      dt = new DropTarget(tree, new FileDropTargetListener());
    }

    createPopupMenu();

  }

  /**
   * add a tree listener for enabling/disabling menu and toolbar icons.
   *
   * @param tree
   */
  private void addTreeListener(JTree tree) {
    tree.addTreeSelectionListener(new TreeSelectionListener() {

      ViewScrollPane emptyView = null;

      public void valueChanged(TreeSelectionEvent e) {
        getMainMenu().getCloseMenuItem().setEnabled(e.getPath() != null);
        if (getMainMenu().getCloseToolBarButton() != null) {
          getMainMenu().getCloseToolBarButton().setEnabled(e.getPath() != null);
        }
        // reset right pane of the top view:

        if (emptyView == null) {
          JEditorPane emptyPane = new JEditorPane("text/html", "<html><body bgcolor=\"ffffff\">   </body></html>");
          emptyPane.setEditable(false);

          emptyView = new ViewScrollPane(emptyPane, runningAsVisualVMPlugin);
        }

        if (e.getPath() == null
                || !(((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject() instanceof Category)) {
          resetPane();
        }
      }

      private void resetPane() {
        int dividerLocation = topSplitPane.getDividerLocation();
        topSplitPane.setRightComponent(emptyView);
        topSplitPane.setDividerLocation(dividerLocation);
      }
    });
  }
  private boolean threadDisplay = false;

  private void setThreadDisplay(boolean value) {
    threadDisplay = value;
    /*
     * if(!value) { // clear thread pane topSplitPane.setRightComponent(null); }
     */
  }

  public boolean isThreadDisplay() {
    return (threadDisplay);
  }

  /**
   * Required by TreeSelectionListener interface.
   */
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

    if (node == null) {
      return;
    }

    Object nodeInfo = node.getUserObject();
    if (nodeInfo instanceof ThreadInfo) {
      displayThreadInfo(nodeInfo);
      setThreadDisplay(true);
    } else if (nodeInfo instanceof ThreadDumpInfo) {
      displayThreadDumpInfo(nodeInfo);
    } else if (nodeInfo instanceof HistogramInfo) {
      HistogramInfo tdi = (HistogramInfo) nodeInfo;
      displayTable((HistogramTableModel) tdi.content);
      setThreadDisplay(false);
    } else if (nodeInfo instanceof LogFileContent) {
      displayLogFileContent(nodeInfo);
    } else if (nodeInfo instanceof Logfile) {
      if (((String) ((Logfile) nodeInfo).getContent()).startsWith("Thread Dumps")) {
        displayLogFile();
        setThreadDisplay(false);
      } else {
        displayLogFileOverview(nodeInfo);
        setThreadDisplay(false);
      }
    } else if (nodeInfo instanceof Category) {
      displayCategory(nodeInfo);
      setThreadDisplay(true);
    } else {
      setThreadDisplay(false);
      displayContent(null);
    }
  }

  /**
   * process table selection events (thread display)
   *
   * @param e
   *          the event to process.
   */
  public void valueChanged(ListSelectionEvent e) {
    // displayCategory(e.getFirstIndex());
    ThreadsTableSelectionModel ttsm = (ThreadsTableSelectionModel) e.getSource();
    JTable table = ttsm.getTable();
    TableSorter ts = (TableSorter) table.getModel();

    int advisoryColmn = 0;
    for (advisoryColmn = 0; advisoryColmn < table.getColumnCount(); advisoryColmn++) {
      String colName = table.getColumnName(advisoryColmn);
      if (colName.equals("Advisories")) {
        break;
      }
    }

    int[] rows = table.getSelectedRows();
    int[] cols = table.getSelectedColumns();
    StringBuffer sb = new StringBuffer();
    ThreadsTableModel threadsModel = (ThreadsTableModel) ts.getTableModel();
    for (int i = 0; i < rows.length; i++) {
      int index = ts.modelIndex(rows[i]);
      appendThreadInfo(sb, threadsModel.getInfoObjectAtRow(index));
    }

    String htmlContent = sb.toString();
    displayContent(htmlContent);

    // If the ThreadModel is for the Merge/ThreadDiffs and user has selected thread progress columns,
    // then try to highlight the thread stack content belonging to the selected dump
    if ((cols.length > 0) && (threadsModel instanceof ThreadDiffsTableModel) && (cols[cols.length - 1] > advisoryColmn)) {

      // There can be cases when we are doing diff between log files
      // and every dump is tagged as "Dump No. 1"
      int diffColumnOffset = cols[0] - advisoryColmn;

      // Highlight the content that occurs between two "Dump " with occurence
      // indicated by diffColumnOffset

      highlightTextBetweenMarkers(AbstractDumpParser.DUMP_MARKER,
              AbstractDumpParser.DUMP_MARKER, diffColumnOffset);

    } else if (searchDialog != null && searchDialog.getSearchText() != null) {
      // Highlight in case of search also
      this.highlightSearchData(searchDialog.getSearchText());
    }

    setThreadDisplay(true);
  }

  private void displayThreadInfo(Object nodeInfo) {
    StringBuffer sb = new StringBuffer("");
    appendThreadInfo(sb, nodeInfo);
    displayContent(sb.toString());
  }

  public static void appendHealth(StringBuffer sb, ThreadInfo ti) {
    String color = ti.getHealth().getBackgroundRGBCode();
    sb.append("<font color=" + color + "\"><b>" + ti.getHealth() + "</b></font>&nbsp;&nbsp;");
  }

  public static void appendAdvisoryLink(StringBuffer sb, ThreadAdvisory advisory) {
    String color = advisory.getHealth().getBackgroundRGBCode();
    // sb.append("<p style=\"background-color:" + color +
    // ";\"><font face=System size=-1>");
    sb.append("<a style=\"color:" + color + "\" href=\"advisory://" + advisory.getKeyword() + "\">"
            + advisory.getPattern() + "</a>&nbsp;&nbsp;");
  }

  private void appendThreadInfo(StringBuffer sb, Object data) {
    // Modified to handle ThreadAdvisory in addition to ThreadInfo
    Object nodeInfo = data;
    if (data instanceof ThreadData) {
      nodeInfo = ((ThreadData) data).getAssocThreadInfo();
    }

    if (nodeInfo instanceof ThreadInfo) {
      ThreadInfo ti = (ThreadInfo) nodeInfo;
      if (ti.getAdvisories() != null && ti.getAdvisories().size() > 0) {
        sb.append("<font size=5>Advisories: ");
        for (Iterator<ThreadAdvisory> iter = ti.getAdvisories().iterator(); iter.hasNext();) {
          ThreadAdvisory adv = iter.next();
          appendAdvisoryLink(sb, adv);
        }
        sb.append("</font><br><br>");
      }

      //Display Thread Context Data if available
      if (ti.getCtxData() != null) {
        sb.append("<font size=4>Context Data: </font><font size=3>");

        String[] ctxDataSet = ti.getCtxData().split(ThreadInfo.CONTEXT_DATA_SEPARATOR);
        for (String contextData : ctxDataSet) {
          sb.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;" + contextData);
        }
        sb.append("</font><br><br>");
      }

      if (ti.getInfo() != null) {
        sb.append(ti.getInfo());
        sb.append(ti.getContent());
      } else {
        sb.append(ti.getContent());
      }
    } else if (nodeInfo instanceof ThreadAdvisory) {
      ThreadAdvisory adv = (ThreadAdvisory) nodeInfo;
      sb.append(adv.getOverview());

    } else if (nodeInfo instanceof ThreadGroup) {
      ThreadGroup tg = (ThreadGroup) nodeInfo;
      sb.append(tg.getOverview());

    }
  }

  private void appendThreadAdvisoryInfo(StringBuffer sb, ThreadInfo nodeInfo) {
    ThreadInfo ti = (ThreadInfo) nodeInfo;
    if (ti.getInfo() != null) {
      sb.append(ti.getInfo());
      sb.append(ti.getContent());
    } else {
      sb.append(ti.getContent());
    }
  }

  /**
   * display thread dump information for the give node object.
   *
   * @param nodeInfo
   */
  private void displayThreadDumpInfo(Object nodeInfo) {
    ThreadDumpInfo ti = (ThreadDumpInfo) nodeInfo;
    displayContent(ti.getOverview());
  }

  private void displayLogFile() {
    if (splitPane.getBottomComponent() != htmlView) {
      splitPane.setBottomComponent(htmlView);
    }
    htmlPane.setContentType("text/html");
    htmlPane.setText("");
    htmlPane.setCaretPosition(0);
    threadDisplay = false;
    statusBar.setInfoText(AppInfo.getStatusBarInfo());
  }

  private void displayLogFileContent(Object nodeInfo) {
    int dividerLocation = splitPane.getDividerLocation();
    if (splitPane.getBottomComponent() != jeditPane) {
      if (jeditPane == null) {
        initJeditView();
      }
      splitPane.setBottomComponent(jeditPane);
    }

    LogFileContent lfc = (LogFileContent) nodeInfo;
    jeditPane.setText(lfc.getContent());
    jeditPane.setCaretPosition(0);
    splitPane.setDividerLocation(dividerLocation);
    statusBar.setInfoText(AppInfo.getStatusBarInfo());
  }

  private void displayLogFileOverview(Object nodeInfo) {
    Logfile logFile = (Logfile) nodeInfo;
    ArrayList<ThreadDumpInfo> tdumpsList = logFile.getThreadDumps();

    StringBuffer statData = new StringBuffer("<font face=System "
            + "><table border=0 width='50%'><tr bgcolor=\"#dddddd\" ><td><font face=System "
            + ">Log File</td><td></td><td colspan=3><b><font face=System>");
    statData.append(logFile.getName());
    statData.append("</b></td></tr>\n");
    statData.append("<tr bgcolor=\"#eeeeee\"><td><font face=System "
            + ">Number of dumps found</td><td></td><td colspan=3><b><font face=System>");
    statData.append(tdumpsList.size());

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#ffffff\"><td></td></tr></table>");

    statData.append(ThreadDumpInfo.getThreadDumpsOverview(tdumpsList));
    displayContent(statData.toString());
  }

  /**
   * initialize the base components needed for the jedit view of the log file
   */
  private void initJeditView() {
    jeditPane = new JEditTextArea();
    jeditPane.setEditable(false);
    jeditPane.setCaretVisible(false);
    jeditPane.setCaretBlinkEnabled(false);
    jeditPane.setRightClickPopup(new PopupMenu(jeditPane, this, runningAsVisualVMPlugin));
    jeditPane.getInputHandler().addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0),
            (ActionListener) jeditPane.getRightClickPopup());
    jeditPane.getInputHandler().addKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK),
            (ActionListener) jeditPane.getRightClickPopup());
  }

  /**
   * display selected category in upper right frame
   */
  private void displayCategory(Object nodeInfo) {
    Category cat = ((Category) nodeInfo);
    Dimension size = null;
    ((JScrollPane) topSplitPane.getLeftComponent()).setPreferredSize(topSplitPane.getLeftComponent().getSize());
    boolean needDividerPos = false;

    if (topSplitPane.getRightComponent() != null) {
      size = topSplitPane.getRightComponent().getSize();
    } else {
      needDividerPos = true;
    }
    setThreadDisplay(true);
    if (cat.getLastView() == null) {
      JComponent catComp = cat.getCatComponent(this);
      if (cat.getName().startsWith("Monitors") || cat.getName().startsWith("Threads blocked by Monitors")
              || cat.getName().startsWith("Holding Locks")) {
        catComp.addMouseListener(getMonitorsPopupMenu());
      } else {
        catComp.addMouseListener(getCatPopupMenu());
      }
      dumpView = new ViewScrollPane(catComp, runningAsVisualVMPlugin);
      if (size != null) {
        dumpView.setPreferredSize(size);
      }

      topSplitPane.setRightComponent(dumpView);
      cat.setLastView(dumpView);
    } else {
      if (size != null) {
        cat.getLastView().setPreferredSize(size);
      }
      topSplitPane.setRightComponent(cat.getLastView());
    }

    // For Category with multiple nodes (like Thread groups), continue to display the summary info instead of selected object at displayCategory level
    // unless the user clicks on specific child entity again which will be let the selected entry's info get displayed anyway
    if ((cat.getNodeCount() <= 1) && cat.getCurrentlySelectedUserObject() != null) {
      displayThreadInfo(cat.getCurrentlySelectedUserObject());
    } else {
      displayContent(cat.getInfo());
    }
    if (needDividerPos) {
      topSplitPane.setDividerLocation(PrefManager.get().getTopDividerPos());
    }
    if (cat.howManyFiltered() > 0) {
      statusBar.setInfoText("Filtered " + cat.howManyFiltered() + " elements in this category. Showing remaining "
              + cat.showing() + " elements.");
    } else {
      statusBar.setInfoText(AppInfo.getStatusBarInfo());
    }

  }

  private void displayContent(String text) {
    if (splitPane.getBottomComponent() != htmlView) {
      splitPane.setBottomComponent(htmlView);
    }
    if (text != null) {
      htmlPane.setContentType("text/html");
      htmlPane.setText("<html><body bgcolor=\"#ffffff\">" + text + "</body></html>");
      htmlPane.setCaretPosition(0);
    } else {
      htmlPane.setText("");
    }
  }

  private void highlightSearchData(String pattern) {

    if (pattern != null && !pattern.equals("")) {

      try {
        // Handle case insensitive search by changing to all lower case for both pattern and content
        String htmlContent = htmlPane.getDocument().getText(0, htmlPane.getDocument().getLength()).toLowerCase();

        int beginIndex, endIndex;
        beginIndex = endIndex = 0;

        // Find the matching pattern and keep going forward till we hit the occurenceCount or ran out of match
        beginIndex = htmlContent.indexOf(pattern.toLowerCase(), beginIndex + 1);

        if (beginIndex >= 0) {

          int spaceIndex = htmlContent.indexOf(" ", beginIndex + pattern.length());
          int packageIndex = htmlContent.indexOf(".", beginIndex + pattern.length());
          endIndex = (spaceIndex < packageIndex) ? spaceIndex : packageIndex;

          htmlPane.getHighlighter().addHighlight(beginIndex, endIndex,
                  new DefaultHighlighter.DefaultHighlightPainter(Color.yellow));

          htmlPane.setCaretPosition(beginIndex);
        }

      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }
  }

  private void highlightTextBetweenMarkers(String beginPattern, String endPattern, int occurenceCount) {

    if (beginPattern != null && !beginPattern.equals("")) {

      try {
        int searchCount = 0;
        // Handle case insensitive search by changing to all lower case for both pattern and content
        String htmlContent = htmlPane.getDocument().getText(0, htmlPane.getDocument().getLength()).toLowerCase();

        int beginIndex, endIndex;
        beginIndex = endIndex = -1;

        // Find the matching pattern and keep going forward till we hit the occurenceCount or ran out of match
        do {
          beginIndex = htmlContent.indexOf(beginPattern.toLowerCase(), beginIndex + 1);

        } while ((beginIndex >= 0) && (occurenceCount > ++searchCount));

        if (beginIndex >= 0) {

          if (endPattern != null) {
            endIndex = htmlContent.indexOf(endPattern.toLowerCase(), beginIndex + 5);
          } else {
            int spaceIndex = htmlContent.indexOf(" ", beginIndex + 1);
            int packageIndex = htmlContent.indexOf(".", beginIndex + 1);
            endIndex = (spaceIndex < packageIndex) ? spaceIndex : packageIndex;
          }

          htmlPane.getHighlighter().addHighlight(beginIndex, endIndex,
                  new DefaultHighlighter.DefaultHighlightPainter(Color.yellow));

          if (beginIndex > 100) {
            htmlPane.setCaretPosition(beginIndex + 100);
          }
        }

      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }
  }

  private void displayTable(HistogramTableModel htm) {
    setThreadDisplay(false);

    htm.setFilter("");
    htm.setShowHotspotClasses(PrefManager.get().getShowHotspotClasses());

    TableSorter ts = new TableSorter(htm);
    histogramTable = new JTable(ts);
    ts.setTableHeader(histogramTable.getTableHeader());
    histogramTable.getColumnModel().getColumn(0).setPreferredWidth(700);
    tableView = new ViewScrollPane(histogramTable, runningAsVisualVMPlugin);

    JPanel histogramView = new JPanel(new BorderLayout());
    JPanel histoStatView = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
    Font font = new Font("SansSerif", Font.PLAIN, 10);
    JLabel infoLabel = new JLabel(NumberFormat.getInstance().format(htm.getRowCount()) + " classes and base types");
    infoLabel.setFont(font);
    histoStatView.add(infoLabel);
    infoLabel = new JLabel(NumberFormat.getInstance().format(htm.getBytes()) + " bytes");
    infoLabel.setFont(font);
    histoStatView.add(infoLabel);
    infoLabel = new JLabel(NumberFormat.getInstance().format(htm.getInstances()) + " live objects");
    infoLabel.setFont(font);
    histoStatView.add(infoLabel);
    if (htm.isOOM()) {
      infoLabel = new JLabel("<html><b>OutOfMemory found!</b>");
      infoLabel.setFont(font);
      histoStatView.add(infoLabel);
    }
    if (htm.isIncomplete()) {
      infoLabel = new JLabel("<html><b>Class Histogram is incomplete! (broken logfile?)</b>");
      infoLabel.setFont(font);
      histoStatView.add(infoLabel);
    }
    JPanel filterPanel = new JPanel(new FlowLayout());
    infoLabel = new JLabel("Filter-Expression");
    infoLabel.setFont(font);
    filterPanel.add(infoLabel);

    filter = new JTextField(30);
    filter.setFont(font);
    filter.addCaretListener(new FilterListener(htm));
    filterPanel.add(infoLabel);
    filterPanel.add(filter);
    checkCase = new JCheckBox();
    checkCase.addChangeListener(new CheckCaseListener(htm));
    infoLabel = new JLabel("Ignore Case");
    infoLabel.setFont(font);
    filterPanel.add(infoLabel);
    filterPanel.add(checkCase);
    histoStatView.add(filterPanel);
    histogramView.add(histoStatView, BorderLayout.SOUTH);
    histogramView.add(tableView, BorderLayout.CENTER);

    histogramView.setPreferredSize(splitPane.getBottomComponent().getSize());

    splitPane.setBottomComponent(histogramView);
  }

  private class FilterListener implements CaretListener {

    HistogramTableModel htm;
    String currentText = "";

    FilterListener(HistogramTableModel htm) {
      this.htm = htm;
    }

    public void caretUpdate(CaretEvent event) {
      if (!filter.getText().equals(currentText)) {
        htm.setFilter(filter.getText());
        histogramTable.revalidate();
      }
    }
  }

  private class CheckCaseListener implements ChangeListener {

    HistogramTableModel htm;

    CheckCaseListener(HistogramTableModel htm) {
      this.htm = htm;
    }

    public void stateChanged(ChangeEvent e) {
      htm.setIgnoreCase(checkCase.isSelected());
      histogramTable.revalidate();
    }
  }

  private void addThreadDumps(DefaultMutableTreeNode top, InputStream dumpFileStream) {
    DumpParser dp = null;
    try {
      String fileName = top.getUserObject().toString();
      Map dumpMap = null;

      if (runningAsVisualVMPlugin) {
        dumpMap = dumpStore.getFromDumpFiles(fileName);
      }

      if (dumpMap == null) {
        dumpMap = new HashMap();
        dumpStore.addFileToDumpFiles(fileName, dumpMap);
      }
      dp = DumpParserFactory.get().getDumpParserForLogfile(dumpFileStream, dumpMap,
              runningAsJConsolePlugin, dumpCounter);
      Logfile logFile = (Logfile) top.getUserObject();
      logFile.setUsedParser(dp);

      while ((dp != null) && dp.hasMoreDumps()) {

        MutableTreeNode node = dp.parseNext();
        top.add(node);

        ThreadDumpInfo tdi = (ThreadDumpInfo) ((DefaultMutableTreeNode) node).getUserObject();
        logFile.addThreadDump(tdi);
        tdi.setLogFile(logFile);

        if (!isFoundClassHistogram) {
          isFoundClassHistogram = dp.isFoundClassHistograms();
        }

        // Try to switch parsers if we have determined the native jvm vendor
        if (dp instanceof FallbackParser && ((FallbackParser) dp).determinedJvmVendor()) {
          dp = ((FallbackParser) dp).recreateParserBasedOnVendor();
          logFile.setUsedParser(dp);
        }

      }
    } finally {
      if (dp != null) {
        try {
          dp.close();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * navigate to the currently selected dump in logfile
   */
  private void navigateToDumpInLogfile() {
    Object userObject = ((DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject();
    if (userObject instanceof ThreadDumpInfo) {
      ThreadDumpInfo ti = (ThreadDumpInfo) userObject;
      int lineNumber = ti.getLogLine();
      Logfile logFile = ti.getLogFile();

      /*
      // find log file node.
      TreePath lastSavedPath, selPath;
      lastSavedPath = selPath = tree.getSelectionPath();

      while (selPath != null
      && !checkNameFromNode((DefaultMutableTreeNode) selPath.getLastPathComponent(), File.separator)) {
      lastSavedPath = selPath;
      selPath = selPath.getParentPath();
      }
      if (selPath == null)
      selPath = lastSavedPath;

      tree.setSelectionPath(selPath);
      tree.scrollPathToVisible(selPath);

      Enumeration childs = ((DefaultMutableTreeNode) selPath.getLastPathComponent()).children();
      boolean found = false;
      DefaultMutableTreeNode logfileContent = null;
      while (!found && childs.hasMoreElements()) {
      logfileContent = (DefaultMutableTreeNode) childs.nextElement();
      found = logfileContent.getUserObject() instanceof LogFileContent;
      }

      if (found) {
      TreePath monitorPath = new TreePath(logfileContent.getPath());
      tree.setSelectionPath(monitorPath);
      tree.scrollPathToVisible(monitorPath);
      displayLogFileContent(logfileContent.getUserObject());
      jeditPane.setFirstLine(lineNumber - 1);
      }

       */

      //Logfile could be either referencing a real file or a clipboard content
      //For cases of clipboard, then get reference to the temporary file that carries those content
      String fileLocation = (logFile.getTempFileLocation() == null) ? logFile.getName() : logFile.getTempFileLocation();
      displayLogFileContent(new LogFileContent(fileLocation));
      jeditPane.setFirstLine(lineNumber - 1);

    }
  }

  /**
   * navigate to monitor
   *
   * @param monitorLink
   *          the monitor link to navigate to
   */
  private void navigateToMonitor(String monitorLink) {
    String monitor = monitorLink.substring(monitorLink.indexOf('/') + 2);

    // find monitor node for this thread info
    DefaultMutableTreeNode dumpNode = null;
    if (monitorLink.indexOf("Dump No.") > 0) {
      dumpNode = getDumpRootNode(
              monitorLink.substring(monitorLink.indexOf('/') + 2, monitorLink.indexOf('/', monitorLink.indexOf('/') + 2)),
              (DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
      monitor = monitor.substring(monitor.indexOf('/') + 1);
    } else {
      dumpNode = getDumpRootNode((DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
    }
    Enumeration childs = dumpNode.children();
    DefaultMutableTreeNode monitorNode = null;
    DefaultMutableTreeNode monitorWithoutLocksNode = null;
    while (childs.hasMoreElements()) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) childs.nextElement();
      if (child.getUserObject() instanceof TreeCategory) {
        if (((TreeCategory) child.getUserObject()).getName().startsWith("Monitors (")) {
          monitorNode = child;
        } else if (((TreeCategory) child.getUserObject()).getName().startsWith("Monitors without")) {
          monitorWithoutLocksNode = child;
        }
      }
    }

    // highlight chosen monitor
    JTree searchTree = (JTree) ((TreeCategory) monitorNode.getUserObject()).getCatComponent(this);
    TreePath searchPath = searchTree.getNextMatch(monitor, 0, Position.Bias.Forward);
    if ((searchPath == null) && (monitorWithoutLocksNode != null)) {
      searchTree = (JTree) ((TreeCategory) monitorWithoutLocksNode.getUserObject()).getCatComponent(this);
      searchPath = searchTree.getNextMatch(monitor, 0, Position.Bias.Forward);
      monitorNode = monitorWithoutLocksNode;
    }

    if (searchPath != null) {
      TreePath monitorPath = new TreePath(monitorNode.getPath());
      tree.setSelectionPath(monitorPath);
      tree.scrollPathToVisible(monitorPath);

      displayCategory(monitorNode.getUserObject());

      TreePath threadInMonitor = searchPath.pathByAddingChild(((DefaultMutableTreeNode) searchPath.getLastPathComponent()).getLastChild());
      searchTree.setSelectionPath(threadInMonitor);
      searchTree.scrollPathToVisible(searchPath);
      searchTree.setSelectionPath(searchPath);
    }
  }

  /**
   * navigate to root node of currently active dump
   */
  private void navigateToDump() {
    TreePath currentPath = tree.getSelectionPath();
    tree.setSelectionPath(currentPath.getParentPath());
    tree.scrollPathToVisible(currentPath.getParentPath());
  }

  /**
   * navigate to child of currently selected node with the given prefix in name
   *
   * @param startsWith
   *          node name prefix (e.g. "Threads waiting")
   */
  private void navigateToChild(String startsWith) {
    TreePath currentPath = tree.getSelectionPath();
    DefaultMutableTreeNode dumpNode = (DefaultMutableTreeNode) currentPath.getLastPathComponent();
    Enumeration childs = dumpNode.children();
    if (!childs.hasMoreElements()) {
      childs = dumpNode.getParent().children();
    }

    TreePath searchPath = null;
    while ((searchPath == null) && childs.hasMoreElements()) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) childs.nextElement();
      String name = child.toString();
      if (name != null && name.startsWith(startsWith)) {
        searchPath = new TreePath(child.getPath());
      }
    }

    if (searchPath != null) {
      tree.makeVisible(searchPath);
      tree.setSelectionPath(searchPath);
      tree.scrollPathToVisible(searchPath);
    }
  }

  protected MainMenu getMainMenu() {
    if ((frame != null) && (frame.getJMenuBar() != null)) {
      return ((MainMenu) frame.getJMenuBar());
    } else {
      if (pluginMainMenu == null) {
        pluginMainMenu = new MainMenu(this);
      }
      return (pluginMainMenu);
    }
  }

  public void createPopupMenu() {
    JMenuItem menuItem;

    // Create the popup menu.
    JPopupMenu popup = new JPopupMenu();

    menuItem = new JMenuItem("Diff Selection");
    menuItem.addActionListener(this);
    popup.add(menuItem);

    /*
    menuItem = new JMenuItem("Find long running threads...");
    menuItem.addActionListener(this);
    popup.add(menuItem);
     *
     */

    showDumpMenuItem = new JMenuItem("Show selected Dump in logfile");
    showDumpMenuItem.addActionListener(this);
    showDumpMenuItem.setEnabled(false);

    /*
    popup.addSeparator();
    menuItem = new JMenuItem("Parse loggc-logfile...");
    menuItem.addActionListener(this);
    if (!PrefManager.get().getForceLoggcLoading()) {
    menuItem.setEnabled(!isFoundClassHistogram);
    }
    popup.add(menuItem);
     */

    menuItem = new JMenuItem("Close logfile...");
    menuItem.addActionListener(this);
    popup.add(menuItem);
    popup.addSeparator();
    popup.add(showDumpMenuItem);
    menuItem = new JMenuItem("About ThreadLogic");
    menuItem.addActionListener(this);
    popup.add(menuItem);


    // Add listener to the text area so the popup menu can come up.
    MouseListener popupListener = new PopupListener(popup);
    tree.addMouseListener(popupListener);
  }
  private PopupListener catPopupListener = null;

  /**
   * create a instance of this menu for a category
   */
  private PopupListener getCatPopupMenu() {
    if (catPopupListener == null) {
      JMenuItem menuItem;

      // Create the popup menu.
      JPopupMenu popup = new JPopupMenu();

      menuItem = new JMenuItem("Search...");
      menuItem.addActionListener(this);
      popup.add(menuItem);

      // Add listener to the text area so the popup menu can come up.
      catPopupListener = new PopupListener(popup);
    }

    return (catPopupListener);
  }
  private PopupListener monitorsPopupListener = null;

  /**
   * create a instance of this menu for a category
   */
  private PopupListener getMonitorsPopupMenu() {
    if (monitorsPopupListener == null) {
      JMenuItem menuItem;

      // Create the popup menu.
      JPopupMenu popup = new JPopupMenu();

      menuItem = new JMenuItem("Search...");
      menuItem.addActionListener(this);
      popup.add(menuItem);
      popup.addSeparator();
      menuItem = new JMenuItem("Expand all nodes");
      menuItem.addActionListener(this);
      popup.add(menuItem);
      menuItem = new JMenuItem("Collapse all nodes");
      menuItem.addActionListener(this);
      popup.add(menuItem);
      popup.addSeparator();
      menuItem = new JMenuItem("Sort by thread count");
      menuItem.addActionListener(this);
      popup.add(menuItem);

      // Add listener to the text area so the popup menu can come up.
      monitorsPopupListener = new PopupListener(popup);
    }

    return (monitorsPopupListener);
  }

  class PopupListener extends MouseAdapter {

    JPopupMenu popup;

    PopupListener(JPopupMenu popupMenu) {
      popup = popupMenu;
    }

    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        popup.show(e.getComponent(), e.getX(), e.getY());
        showDumpMenuItem.setEnabled((tree.getSelectionPath() != null)
                && ((DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject() instanceof ThreadDumpInfo);
      }
    }
  }

  /**
   * check menu and button events.
   */
  public void actionPerformed(ActionEvent e) {
    try {
    if (e.getSource() instanceof JMenuItem) {
      JMenuItem source = (JMenuItem) (e.getSource());
      if (source.getText().substring(1).startsWith(":\\") || source.getText().startsWith("/")) {
        if (source.getText().endsWith(".tsf")) {
          try {
            loadSession(new File(source.getText()), true);
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        } else {
          dumpFile = source.getText();
          openFiles(new File[]{new File(dumpFile)}, true);
        }
      } else if ("Open...".equals(source.getText())) {
        chooseFile();
      } else if ("Open loggc file...".equals(source.getText())) {
        openLoggcFile();
      } else if ("Save Logfile...".equals(source.getText())) {
        saveLogFile();
      } else if ("Save Session...".equals(source.getText())) {
        saveSession();
      } else if ("Open Session...".equals(source.getText())) {
        openSession();
      } else if ("Preferences".equals(source.getText())) {
        showPreferencesDialog();
      } else if ("Filters".equals(source.getText())) {
        showFilterDialog();
      } else if ("Categories".equals(source.getText())) {
        showCategoriesDialog();
      } else if ("Get Logfile from clipboard".equals(source.getText())) {
        getLogfileFromClipboard();
      } else if ("Exit ThreadLogic".equals(source.getText())) {
        saveState();
        frame.dispose();
      } else if (ResourceManager.translate("help.contents").equals(source.getText())) {
        showHelp();
      } else if ("Help".equals(source.getText())) {
        showHelp();
      } else if ("Release Notes".equals(source.getText())) {
        showInfoFile("Release Notes", "doc/README", "Document.gif");
      } else if ("License".equals(source.getText())) {
        showInfoFile("License Information", "doc/COPYING", "Document.gif");
      } else if ("Forum".equals(source.getText())) {
        try {
          Browser.open("http://java.net/projects/threadlogic/forums/ThreadLogic");
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(
                  this.getRootPane(),
                  "Error opening ThreadLogic Online Forum\nPlease open http://java.net/projects/threadlogic/forums/ThreadLogic in your browser!",
                  "Error", JOptionPane.ERROR_MESSAGE);
        }
      } else if ("About ThreadLogic".equals(source.getText())) {
        showInfo();
      } else if ("Search...".equals(source.getText())) {
        showSearchDialog();
      } else if ("Parse loggc-logfile...".equals(source.getText())) {
        parseLoggcLogfile();
      } else if (("Close logfile...".equals(source.getText())) || ("Close...".equals(source.getText()))) {
        closeCurrentDump();
      } else if ("Close all...".equals(source.getText())) {
        closeAllDumps();
      } else if ("Diff Selection".equals(source.getText())) {
        TreePath[] paths = tree.getSelectionPaths();
        if ((paths != null) && (paths.length < 2)) {
          JOptionPane.showMessageDialog(this.getRootPane(), "You must select at least two dumps for getting a diff!\n",
                  "Error", JOptionPane.ERROR_MESSAGE);

        } else {
          DefaultMutableTreeNode mergeRoot = fetchTop(tree.getSelectionPath());
          Map dumpMap = dumpStore.getFromDumpFiles(mergeRoot.getUserObject().toString());
          Object logFile = mergeRoot.getUserObject();
          if (logFile instanceof ThreadDumpInfo) {
            logFile = ((ThreadDumpInfo) logFile).getLogFile();
          }

          ((Logfile) logFile).getUsedParser().mergeDumps(mergeRoot, dumpMap, paths, paths.length,
                  null);
          createTree();
          try {
            this.getRootPane().revalidate();
          } catch (Exception ne) {
            // Error when running in plugin mode
          }
        }
      } else if ("Show selected Dump in logfile".equals(source.getText())) {
        navigateToDumpInLogfile();
      } else if ("Show Toolbar".equals(source.getText())) {
        setShowToolbar(((JCheckBoxMenuItem) source).getState());
      } else if ("Request Thread Dump...".equals(source.getText())) {
        addMXBeanDump();
      } else if ("Expand all nodes".equals(source.getText())) {
        expandAllCatNodes(true);
      } else if ("Collapse all nodes".equals(source.getText())) {
        expandAllCatNodes(false);
      } else if ("Sort by thread count".equals(source.getText())) {
        sortCatByThreads();
      } else if ("Expand all Dump nodes".equals(source.getText())) {
        expandAllDumpNodes(true);
      } else if ("Collapse all Dump nodes".equals(source.getText())) {
        expandAllDumpNodes(false);
      }
    } else if (e.getSource() instanceof JButton) {
      JButton source = (JButton) e.getSource();
      if ("Open Logfile".equals(source.getToolTipText())) {
        chooseFile();
      } else if ("Close selected Logfile".equals(source.getToolTipText())) {
        closeCurrentDump();
      } else if ("Get Logfile from clipboard".equals(source.getToolTipText())) {
        getLogfileFromClipboard();
      } else if ("Preferences".equals(source.getToolTipText())) {
        showPreferencesDialog();
      } else if ("Expand all nodes".equals(source.getToolTipText())) {
        expandAllDumpNodes(true);
      } else if ("Collapse all nodes".equals(source.getToolTipText())) {
        expandAllDumpNodes(false);
      } else if ("Filters".equals(source.getToolTipText())) {
        showFilterDialog();
      } else if ("Custom Categories".equals(source.getToolTipText())) {
        showCategoriesDialog();
      } else if ("Request a Thread Dump".equals(source.getToolTipText())) {
        addMXBeanDump();
      } else if ("Help".equals(source.getToolTipText())) {
        showHelp();
      } else if ("About ThreadLogic".equals(source.getToolTipText())) {
        showInfo();
      }
      source.setSelected(false);
    } else if (e.getSource() instanceof JComboBox) {
      ItemSelectable item = (ItemSelectable) e.getSource();
      String health = (String) item.getSelectedObjects()[0];
      HealthLevelFilter healthFilter = (HealthLevelFilter) FilterChecker.getFilterChecker().getFromFilters(
              "Minimum Health Level Filter");
      healthFilter.setHealth(HealthLevel.valueOf(health));
      PrefManager.get().setFilterLastChanged();
      PrefManager.get().setHealthLevel(health);
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
      if (node != null && node.getUserObject() instanceof Category) {
        displayCategory(node.getUserObject());
      }
    }

  } catch(Exception e1) {
  e1.printStackTrace();
  }
  }

  private void showInfo() {
    InfoDialog infoDialog = new InfoDialog(getFrame());
    infoDialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

    // Display the window.
    infoDialog.pack();
    infoDialog.setLocationRelativeTo(getFrame());
    infoDialog.setVisible(true);
  }

  /**
   * set the ui font for all ThreadLogic stuff (needs to be done for create of objects)
   *
   * @param f
   *          the font to user
   */
  private void setUIFont(javax.swing.plaf.FontUIResource f) {
    //
    // sets the default font for all Swing components.
    // ex.
    // setUIFont (new javax.swing.plaf.FontUIResource("Serif",Font.ITALIC,12));
    //
    java.util.Enumeration keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);
      if (value instanceof javax.swing.plaf.FontUIResource) {
        UIManager.put(key, f);
      }
    }
  }

  /**
   * display the specified file in a info window.
   *
   * @param title
   *          title of the info window.
   * @param file
   *          the file to display.
   */
  private void showInfoFile(String title, String file, String icon) {
    HelpOverviewDialog infoDialog = new HelpOverviewDialog(getFrame(), title, file, ThreadLogic.createImageIcon(icon).getImage());
    infoDialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

    // Display the window.
    infoDialog.pack();
    infoDialog.setLocationRelativeTo(getFrame());
    infoDialog.setVisible(true);
  }

  private Frame getAwtFrame() {
    Container owner = this.getParent();
    while (owner != null && !(owner instanceof java.awt.Frame)) {
      owner = owner.getParent();
    }

    return (owner != null ? (Frame) owner : null);
  }

  private JFrame getFrame() {
    Container owner = this.getParent();
    while (owner != null && !(owner instanceof JFrame)) {
      owner = owner.getParent();
    }

    return (owner != null ? (JFrame) owner : null);
  }

  private void showPreferencesDialog() {
    // Create and set up the window.
    if (prefsDialog == null) {
      prefsDialog = new PreferencesDialog(getFrame());
      prefsDialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    /*
     * No Parent Frame when running as JConsolePlugin
     * Commenting off this does not seem to affect the dialog box behavior.
    getFrame().setEnabled(false);
     *
     */

    // Display the window.
    prefsDialog.reset();
    prefsDialog.pack();
    prefsDialog.setLocationRelativeTo(getFrame());
    prefsDialog.setVisible(true);
  }

  public void showFilterDialog() {

    // Create and set up the window.
    if (filterDialog == null) {
      filterDialog = new FilterDialog(getFrame());
      filterDialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    getFrame().setEnabled(false);
    // Display the window.
    filterDialog.reset();
    filterDialog.pack();
    filterDialog.setLocationRelativeTo(getFrame());
    filterDialog.setVisible(true);
  }

  /**
   * display categories settings.
   */
  private void showCategoriesDialog() {
    // Create and set up the window.
    if (categoriesDialog == null) {
      categoriesDialog = new CustomCategoriesDialog(getFrame());
      categoriesDialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    getFrame().setEnabled(false);
    // Display the window.
    categoriesDialog.reset();
    categoriesDialog.pack();
    categoriesDialog.setLocationRelativeTo(getFrame());
    categoriesDialog.setVisible(true);
  }
  /**
   * flag indicates if next file to open will be the first file (so fresh open)
   * or if a add has to be performed.
   */
  private boolean firstFile = true;

  /**
   * save the current logfile (only used in plugin mode)
   */
  public void saveLogFile() {
    if (fc == null) {
      fc = new JFileChooser();
      fc.setMultiSelectionEnabled(true);
      fc.setCurrentDirectory(PrefManager.get().getSelectedPath());
    }
    if (firstFile && (PrefManager.get().getPreferredSizeFileChooser().height > 0)) {
      fc.setPreferredSize(PrefManager.get().getPreferredSizeFileChooser());
    }
    int returnVal = fc.showSaveDialog(this.getRootPane());
    fc.setPreferredSize(fc.getSize());
    PrefManager.get().setPreferredSizeFileChooser(fc.getSize());

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      int selectValue = 0;
      if (file.exists()) {
        Object[] options = {"Overwrite", "Cancel"};
        selectValue = JOptionPane.showOptionDialog(null,
                "<html><body>File exists<br><b>" + file + "</b></body></html>", "Confirm overwrite",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
      }
      if (selectValue == 0) {
        FileOutputStream fos = null;
        try {
          fos = new FileOutputStream(file);
          fos.write(((LogFileContent) logFile.getUserObject()).getContent().getBytes());
          fos.flush();
        } catch (IOException ex) {
          ex.printStackTrace();
        } finally {
          try {
            fos.close();
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * choose a log file.
   *
   * @param addFile
   *          check if a log file should be added or if tree should be cleared.
   */
  private void chooseFile() {
    if (firstFile && (PrefManager.get().getPreferredSizeFileChooser().height > 0)) {
      fc.setPreferredSize(PrefManager.get().getPreferredSizeFileChooser());
    }
    int returnVal = fc.showOpenDialog(this.getRootPane());
    fc.setPreferredSize(fc.getSize());
    PrefManager.get().setPreferredSizeFileChooser(fc.getSize());

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File[] files = fc.getSelectedFiles();
      openFiles(files, false);
    }
  }

  /**
   * open the provided files. If isRecent is set to true, passed files are not
   * added to the recent file list.
   *
   * @param files
   *          the files array to open
   * @param isRecent
   *          true, if passed files are from recent file list.
   */
  private void openFiles(File[] files, boolean isRecent) {

    for (int i = 0; i < files.length; i++) {
      dumpFile = files[i].getAbsolutePath();
      if (dumpFile != null) {
        if (!firstFile) {
          // root nodes are moved down.
          setRootNodeLevel(1);

          // do direct add without re-init.
          addDumpFile();
        } else {
          initDumpDisplay(null);
          firstFile = false;
          setFileOpen(true);
        }
      }

      if (!isRecent) {
        PrefManager.get().addToRecentFiles(files[i].getAbsolutePath());
      }
    }

    if (isFileOpen()) {
      if (this.getRootPane() != null) {
        this.getRootPane().revalidate();
      }
      displayContent(null);
    }
  }

  /**
   * Returns an ImageIcon, or null if the path was invalid.
   */
  public static ImageIcon createImageIcon(String path) {
    java.net.URL imgURL = ThreadLogic.class.getResource("icons/" + path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    }

    imgURL = ThreadLogic.class.getResource("docs/" + path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    }

    System.err.println("Couldn't find file: " + path);
    return null;
  }

  /**
   * search for dump root node of for given node
   *
   * @param node
   *          starting to search for
   * @return root node returns null, if no root was found.
   */
  private DefaultMutableTreeNode getDumpRootNode(DefaultMutableTreeNode node) {
    // search for starting node
    while (node != null && !(node.getUserObject() instanceof ThreadDumpInfo)) {
      node = (DefaultMutableTreeNode) node.getParent();
    }

    return (node);
  }

  /**
   * get the dump with the given name, starting from the provided node.
   *
   * @param dumpName
   * @return
   */
  private DefaultMutableTreeNode getDumpRootNode(String dumpName, DefaultMutableTreeNode node) {
    DefaultMutableTreeNode lastNode = null;
    DefaultMutableTreeNode dumpNode = null;
    // search for starting node
    while (node != null && !(node.getUserObject() instanceof Logfile)) {
      lastNode = node;
      node = (DefaultMutableTreeNode) node.getParent();
    }

    if (node == null) {
      node = lastNode;
    }

    for (int i = 0; i < node.getChildCount(); i++) {
      Object userObject = ((DefaultMutableTreeNode) node.getChildAt(i)).getUserObject();
      if ((userObject instanceof ThreadDumpInfo) && ((ThreadDumpInfo) userObject).getName().startsWith(dumpName)) {
        dumpNode = (DefaultMutableTreeNode) node.getChildAt(i);
        break;
      }
    }

    return (dumpNode);
  }

  /**
   * load a loggc log file based on the current selected thread dump
   */
  private void parseLoggcLogfile() {
    DefaultMutableTreeNode node = getDumpRootNode((DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
    if (node == null) {
      return;
    }

    // get pos of this node in the thread dump hierarchy.
    int pos = node.getParent().getIndex(node);

    ((Logfile) ((DefaultMutableTreeNode) node.getParent()).getUserObject()).getUsedParser().setDumpHistogramCounter(pos);
    openLoggcFile();
  }

  /**
   * close the currently selected dump.
   */
  private void closeCurrentDump() {
    TreePath selPath = tree.getSelectionPath();
    if (selPath == null) {
      return;
    }

    boolean isNotFromFile = isNotFromFile((DefaultMutableTreeNode) selPath.getLastPathComponent());

    while (selPath != null
            && !isNotFromFile
            && !(checkNameFromNode((DefaultMutableTreeNode) selPath.getLastPathComponent(), File.separator)
            || checkNameFromNode((DefaultMutableTreeNode) selPath.getLastPathComponent(), 2, File.separator))) {
      selPath = selPath.getParentPath();
    }

    Object[] options = {"Close File", "Cancel close"};

    String fileName = ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject().toString();
    if (!isNotFromFile) {
      fileName = fileName.substring(fileName.indexOf(File.separator));
    }

    int selectValue = JOptionPane.showOptionDialog(null,
            "<html><body>Are you sure, you want to close the currently selected dump file<br><b>" + fileName
            + "</b></body></html>", "Confirm closing...", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

    // if first option "close file" is selected.
    if (selectValue == 0) {
      // remove stuff from the top nodes

      if (topNodes.remove(selPath.getLastPathComponent()) == false) {
        ((DefaultMutableTreeNode) selPath.getPathComponent(0)).remove((DefaultMutableTreeNode) selPath.getLastPathComponent());
      }

      if (topNodes.size() == 0) {
        // simply do a reinit, as there isn't anything to display
        removeAll();
        revalidate();

        init(runningAsJConsolePlugin, runningAsVisualVMPlugin);
        getMainMenu().getCloseMenuItem().setEnabled(false);
        getMainMenu().getSaveSessionMenuItem().setEnabled(false);
        getMainMenu().getCloseToolBarButton().setEnabled(false);
        getMainMenu().getExpandButton().setEnabled(false);
        getMainMenu().getCollapseButton().setEnabled(false);
        getMainMenu().getCloseAllMenuItem().setEnabled(false);
        getMainMenu().getExpandAllMenuItem().setEnabled(false);
        getMainMenu().getCollapseAllMenuItem().setEnabled(false);

        firstFile = true;
        dumpFile = null;

      } else {
        // rebuild jtree
        getMainMenu().getCloseMenuItem().setEnabled(false);
        getMainMenu().getCloseToolBarButton().setEnabled(false);
        createTree();
      }
      revalidate();
    }

  }

  /**
   * close all open dumps
   */
  private void closeAllDumps() {
    Object[] options = {"Close all", "Cancel close"};

    int selectValue = JOptionPane.showOptionDialog(null,
            "<html><body>Are you sure, you want to close all open dump files", "Confirm closing...",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

    // if first option "close file" is selected.
    if (selectValue == 0) {
      // remove stuff from the top nodes
      topNodes = new Vector();

      // simply do a reinit, as there is anything to display
      resetMainPanel();
    }
    firstFile = true;
  }

  private boolean isNotFromFile(DefaultMutableTreeNode node) {
    Object info = node.getUserObject();
    String result = null;
    if ((info != null) && (info instanceof AbstractInfo)) {
      result = ((AbstractInfo) info).getName();
    } else if ((info != null) && (info instanceof String)) {
      result = (String) info;
    }
    if (result.contains("Clipboard at") || result.contains("Merge between Dump")
            || result.contains("Long running thread detection")
            || result.contains("JMX Thread Dump")) {
      return true;
    }
    return false;
  }

  /**
   * reset the main panel to start up
   */
  private void resetMainPanel() {
    removeAll();
    revalidate();

    init(runningAsJConsolePlugin, runningAsVisualVMPlugin);
    revalidate();

    getMainMenu().getCloseMenuItem().setEnabled(false);
    getMainMenu().getSaveSessionMenuItem().setEnabled(false);
    getMainMenu().getCloseToolBarButton().setEnabled(false);
    getMainMenu().getExpandButton().setEnabled(false);
    getMainMenu().getCollapseButton().setEnabled(false);
    getMainMenu().getCloseAllMenuItem().setEnabled(false);
    getMainMenu().getExpandAllMenuItem().setEnabled(false);
    getMainMenu().getCollapseAllMenuItem().setEnabled(false);

  }

  /**
   * check if name of node starts with passed string
   *
   * @param node
   *          the node name to check
   * @param startsWith
   *          the string to compare.
   * @return true if startsWith and beginning of node name matches.
   */
  private boolean checkNameFromNode(DefaultMutableTreeNode node, String startsWith) {
    return (checkNameFromNode(node, 0, startsWith));
  }

  /**
   * check if name of node starts with passed string
   *
   * @param node
   *          the node name to check
   * @param startIndex
   *          the index to start with comparing, 0 if comparing should happen
   *          from the beginning.
   * @param startsWith
   *          the string to compare.
   * @return true if startsWith and beginning of node name matches.
   */
  private boolean checkNameFromNode(DefaultMutableTreeNode node, int startIndex, String startsWith) {
    Object info = node.getUserObject();
    String result = null;
    if ((info != null) && (info instanceof AbstractInfo)) {
      result = ((AbstractInfo) info).getName();
    } else if ((info != null) && (info instanceof String)) {
      result = (String) info;
    }

    if (startIndex > 0) {
      result = result.substring(startIndex);
    }

    return (result != null && result.startsWith(startsWith));
  }

  /**
   * open and parse loggc file
   */
  private void openLoggcFile() {
    int returnVal = fc.showOpenDialog(this.getRootPane());

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      loggcFile = file.getAbsolutePath();
      if (loggcFile != null) {
        try {
          final InputStream loggcFileStream = new ProgressMonitorInputStream(this, "Parsing " + loggcFile,
                  new FileInputStream(loggcFile));

          final SwingWorker worker = new SwingWorker() {

            public Object construct() {
              try {
                DefaultMutableTreeNode top = fetchTop(tree.getSelectionPath());

                ((Logfile) top.getUserObject()).getUsedParser().parseLoggcFile(loggcFileStream, top);

                addThreadDumps(top, loggcFileStream);
                createTree();
                getRootPane().revalidate();
                displayContent(null);
              } finally {
                if (loggcFileStream != null) {
                  try {
                    loggcFileStream.close();
                  } catch (IOException ex) {
                    ex.printStackTrace();
                  }
                }
              }
              return null;
            }
          };
          worker.start();
        } catch (FileNotFoundException ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * find long running threads either in all parsed thread dumps or in marked
   * thread dump range.
   */
  private void findLongRunningThreads() {
    TreePath[] paths = tree.getSelectionPaths();
    if ((paths == null) || (paths.length < 2)) {
      JOptionPane.showMessageDialog(this.getRootPane(),
              "You must select at least two dumps for long thread run detection!\n", "Error", JOptionPane.ERROR_MESSAGE);

    } else {
      DefaultMutableTreeNode mergeRoot = fetchTop(tree.getSelectionPath());
      Map dumpMap = dumpStore.getFromDumpFiles(mergeRoot.getUserObject().toString());

      LongThreadDialog longThreadDialog = new LongThreadDialog(this, paths, mergeRoot, dumpMap);

      if (frame != null) {
        frame.setEnabled(false);
      }

      // Display the window.
      longThreadDialog.reset();
      longThreadDialog.pack();
      longThreadDialog.setLocationRelativeTo(frame);
      longThreadDialog.setVisible(true);

    }
  }
  private int rootNodeLevel = 0;

  private int getRootNodeLevel() {
    return (rootNodeLevel);
  }

  private void setRootNodeLevel(int value) {
    rootNodeLevel = value;
  }

  private DefaultMutableTreeNode fetchTop(TreePath pathToRoot) {
    return ((DefaultMutableTreeNode) pathToRoot.getPathComponent(getRootNodeLevel()));
  }

  /**
   * save the application state to preferences.
   */
  private void saveState() {
    PrefManager.get().setWindowState(frame.getExtendedState());
    PrefManager.get().setSelectedPath(fc.getCurrentDirectory());
    PrefManager.get().setPreferredSize(frame.getRootPane().getSize());
    PrefManager.get().setWindowPos(frame.getX(), frame.getY());
    if (isThreadDisplay()) {
      PrefManager.get().setTopDividerPos(topSplitPane.getDividerLocation());
      PrefManager.get().setDividerPos(splitPane.getDividerLocation());
    }
    PrefManager.get().flush();
  }
  /**
   * trigger, if a file is opened
   */
  private boolean fileOpen = false;

  private boolean isFileOpen() {
    return fileOpen;
  }

  private void setFileOpen(boolean value) {
    fileOpen = value;
  }

  /**
   * Create the GUI and show it. For thread safety, this method should be
   * invoked from the event-dispatching thread.
   */
  private static void createAndShowGUI() {
    // Create and set up the window.
    frame = new JFrame("ThreadLogic - We'll do the analysis for you!");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Image image = Toolkit.getDefaultToolkit().getImage( "ThreadLogic.gif" );
    Image image = ThreadLogic.createImageIcon("ThreadLogic.gif").getImage();
    frame.setIconImage(image);

    frame.getRootPane().setPreferredSize(PrefManager.get().getPreferredSize());

    frame.setJMenuBar(new MainMenu(ThreadLogic.get(true)));
    ThreadLogic.get(true).init();

    // Create and set up the content pane.
    if (dumpFile != null) {
      ThreadLogic.get(true).initDumpDisplay(null);
    }

    ThreadLogic.get(true).setOpaque(true); // content panes must be opaque
    frame.setContentPane(ThreadLogic.get(true));

    // init filechooser
    fc = new JFileChooser();
    fc.setMultiSelectionEnabled(true);
    fc.setCurrentDirectory(PrefManager.get().getSelectedPath());

    /**
     * add window listener for persisting state of main frame
     */
    frame.addWindowListener(new WindowAdapter() {

      public void windowClosing(WindowEvent e) {
        ThreadLogic.get(true).saveState();
      }

      public void windowClosed(WindowEvent e) {
        ThreadDumpInfo.shutdownExecutor();
        System.exit(0);
      }
    });

    frame.setLocation(PrefManager.get().getWindowPos());

    // Display the window.
    frame.pack();

    // restore old window settings.
    frame.setExtendedState(PrefManager.get().getWindowState());

    frame.setVisible(true);

    Runtime.getRuntime().addShutdownHook(new Thread() {

      public void run() {
        ThreadLogic.get(true).finalize();
      }
    });

    // If a path to a directory has been provided as an argument,
    // load the log files within them.
    if (ThreadLogic.defaultTDumpDir != null) {
      String logDir = ThreadLogic.defaultTDumpDir;
      File logDirFile = new File(logDir);
      theLogger.fine("Log Directory: " + logDir);

      File[] logFiles = logDirFile.listFiles();
      if (logFiles.length < 0) {
        theLogger.warning("No files found under the specified Log Directory:" + logDir);
      }

      ThreadLogic.get(true).openFiles(logFiles, false);
    }
  }

  /**
   * display search dialog for current category
   */
  private void showSearchDialog() {
    // get the currently select category tree
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
    JComponent catComp = ((Category) node.getUserObject()).getCatComponent(this);

    // Create and set up the window.
    searchDialog = new SearchDialog(getFrame(), catComp);

    getFrame().setEnabled(false);
    // Display the window.
    searchDialog.reset();
    searchDialog.pack();
    searchDialog.setLocationRelativeTo(getFrame());
    searchDialog.setVisible(true);

    searchDialog.addWindowListener(new WindowAdapter() {

      public void windowClosed(WindowEvent e) {
        getFrame().setEnabled(true);
      }
    });
  }

  /**
   * main startup method for ThreadLogic
   */
  public static void main(String[] args) {
    if (args.length > 0) {

      File dumpDir = new File(args[0]);

      // Check for directory and load dumps within it
      if (dumpDir.isDirectory()) {
        defaultTDumpDir = args[0];
      } else {
        // Load single thread dump file
        dumpFile = args[0];
      }
      //dumpFile = args[0];
    }
    // Schedule a job for the event-dispatching thread:
    // creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {

      public void run() {
        createAndShowGUI();
      }
    });
  }

  /**
   * check file menu
   */
  public void menuSelected(MenuEvent e) {
    JMenu source = (JMenu) e.getSource();
    if ((source != null) && "File".equals(source.getText())) {
      // close menu item only active, if something is selected.
      getMainMenu().getCloseMenuItem().setEnabled(tree.getSelectionPath() != null);
      getMainMenu().getCloseToolBarButton().setEnabled(tree.getSelectionPath() != null);
    }
  }

  public void menuDeselected(MenuEvent e) {
    // nothing to do
  }

  public void menuCanceled(MenuEvent e) {
    // nothing to do
  }

  public static String getFontSizeModifier(int add) {
    String result = String.valueOf(fontSizeModifier + add);
    if ((fontSizeModifier + add) > 0) {
      result = "+" + (fontSizeModifier + add);
    }
    return (result);
  }

  public static void setFontSizeModifier(int value) {
    fontSizeModifier = value;
  }

  /**
   * handles dragging events for new files to open.
   */
  private class FileDropTargetListener extends DropTargetAdapter {

    public void drop(DropTargetDropEvent dtde) {
      try {
        DataFlavor[] df = dtde.getTransferable().getTransferDataFlavors();
        for (int i = 0; i < df.length; i++) {
          if (df[i].isMimeTypeEqual("application/x-java-serialized-object")) {
            dtde.acceptDrop(dtde.getDropAction());
            String[] fileStrings = ((String) dtde.getTransferable().getTransferData(df[i])).split("\n");
            File[] files = new File[fileStrings.length];
            for (int j = 0; j < fileStrings.length; j++) {
              files[j] = new File(fileStrings[j].substring(7));
            }
            openFiles(files, false);
            dtde.dropComplete(true);
          }
        }
      } catch (UnsupportedFlavorException ex) {
        ex.printStackTrace();
        dtde.rejectDrop();
      } catch (IOException ex) {
        ex.printStackTrace();
        dtde.rejectDrop();
      }

    }
  }

  protected void addToTempFileList(File tempFile) {
    tempFileList.add(tempFile);
  }

  protected void finalize() {
    // Clean up all temporary files created with additional markers...
    for (File tmpFile : tempFileList) {
      tmpFile.delete();
    }
    tempFileList.clear();
  }

  /**
   * Clone the stream and save temporarily
   *
   */
  private InputStream createTempFileFromClipboard(byte[] content) throws IOException {

    BufferedInputStream bis = null;
    try {
      File tempFile = null;
      tempFile = File.createTempFile("tlogic.tmp.", ".log");
      tempFile.deleteOnExit();
      addToTempFileList(tempFile);

      // Add the markers based on VM Type
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));

      bos.write(content);
      bos.flush();
      bos.close();
      bos = null;
      dumpFile = tempFile.getAbsolutePath();

      bis = new BufferedInputStream(new FileInputStream(tempFile));

    } catch (IOException e) {
      theLogger.warning("Unable to create a temporary file to store clipboard contents: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
    return bis;
  }

  public static Vector getTopNodes() {
    return ThreadLogic.get().topNodes;
  }
}
