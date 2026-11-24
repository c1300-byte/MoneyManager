package com.carsen.moneymanager.ui.pages;

import com.carsen.moneymanager.ui.MainWindow;
import com.carsen.moneymanager.ui.Theme;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;

/** One screen in the window (Dashboard, Goals, ...). */
public abstract class BasePage extends JPanel {

    protected final MainWindow window;

    protected BasePage(MainWindow window) {
        super(new BorderLayout());
        this.window = window;
        setBackground(Theme.BG);
    }

    public abstract String title();

    public abstract String subtitle();

    /** Buttons shown at the top right while this page is open. */
    public List<JComponent> actions() {
        return Collections.emptyList();
    }

    /** Re-read the data and update what's on screen. */
    public abstract void refresh();

    /** Ctrl+F. */
    public void focusSearch() {
    }
}
