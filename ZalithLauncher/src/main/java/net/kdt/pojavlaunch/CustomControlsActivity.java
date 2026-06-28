package net.kdt.pojavlaunch;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.drawerlayout.widget.DrawerLayout;

import com.movtery.zalithlauncher.databinding.ActivityCustomControlsBinding;
import com.movtery.zalithlauncher.databinding.ViewControlMenuBinding;
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity;
import com.movtery.zalithlauncher.ui.subassembly.menu.ControlMenu;

import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;

import java.io.IOException;

/**
 * ZL1 Legacy Control Editor Activity (Backport).
 * Hosts the original Zalith Launcher 1 canvas-based control editor.
 * Navigation: Settings → Layout → Legacy (Zalith 1)
 */
public class CustomControlsActivity extends BaseAppCompatActivity implements EditorExitable {

    public static final String BUNDLE_CONTROL_PATH = "control_path";
    private ActivityCustomControlsBinding binding;
    private String mControlPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseBundle();
        binding = ActivityCustomControlsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final ControlLayout controlLayout = binding.customctrlControllayout;
        final DrawerLayout drawerLayout = binding.customctrlDrawerlayout;
        final FrameLayout drawerNavigationView = binding.customctrlNavigationView;

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerLayout.setScrimColor(Color.TRANSPARENT);

        final ViewControlMenuBinding controlMenuBinding =
                ViewControlMenuBinding.inflate(getLayoutInflater());
        new ControlMenu(this, this, controlMenuBinding, controlLayout, true);
        drawerNavigationView.addView(controlMenuBinding.getRoot());
        controlLayout.setModifiable(true);

        // Floating menu-toggle button (opens/closes the control menu drawer)
        binding.customctrlMenuToggle.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(drawerNavigationView)) {
                drawerLayout.closeDrawer(drawerNavigationView);
            } else {
                drawerLayout.openDrawer(drawerNavigationView);
            }
        });

        try {
            if (mControlPath == null) controlLayout.loadLayout((String) null);
            else controlLayout.loadLayout(mControlPath);
        } catch (IOException e) {
            Tools.showError(this, e);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.customctrlControllayout.askToExit(CustomControlsActivity.this);
            }
        });
    }

    private void parseBundle() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mControlPath = bundle.getString(BUNDLE_CONTROL_PATH);
        }
    }
}
