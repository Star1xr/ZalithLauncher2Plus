package net.kdt.pojavlaunch;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

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
 * Navigation: Settings -> Layout -> Legacy (Zalith 1) -> edit.
 */
public class CustomControlsActivity extends BaseAppCompatActivity implements EditorExitable {

    public static final String BUNDLE_CONTROL_PATH = "control_path";
    private ActivityCustomControlsBinding binding;
    private String mControlPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure dp↔px conversions work before loading any control layout
        Tools.currentDisplayMetrics.setTo(getResources().getDisplayMetrics());
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

        // When editing a legacy file, wire save/exit buttons to operate on the
        // original file path instead of opening a save-as dialog that targets
        // the ZL2 layout directory (DIR_CTRLMAP_PATH).
        if (mControlPath != null) {
            final String filePath = mControlPath;

            controlMenuBinding.save.setOnClickListener(v -> {
                try {
                    controlLayout.saveLayout(filePath);
                    com.movtery.zalithlauncher.game.control.legacy.LegacyControlManager.INSTANCE.refresh();
                    Toast.makeText(this,
                            getString(com.movtery.zalithlauncher.R.string.generic_saved),
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Tools.showError(this, e);
                }
            });

            controlMenuBinding.saveAndExit.setOnClickListener(v -> {
                try {
                    controlLayout.saveLayout(filePath);
                    com.movtery.zalithlauncher.game.control.legacy.LegacyControlManager.INSTANCE.refresh();
                    finish();
                } catch (Exception e) {
                    Tools.showError(this, e);
                }
            });

            // Exit still uses the standard ask-to-exit flow so the user can
            // choose to discard unsaved changes or cancel.
            controlMenuBinding.exit.setOnClickListener(v ->
                    controlLayout.askToExit(CustomControlsActivity.this));

            // Ensure CallbackBridge has real screen dimensions for insertDynamicPos()
            // before loading the layout, so buttons appear on-screen immediately.
            if (org.lwjgl.glfw.CallbackBridge.physicalWidth == 0) {
                org.lwjgl.glfw.CallbackBridge.physicalWidth = Tools.currentDisplayMetrics.widthPixels;
                org.lwjgl.glfw.CallbackBridge.physicalHeight = Tools.currentDisplayMetrics.heightPixels;
            }

            // Load the legacy layout file into the editor canvas.
            try {
                controlLayout.loadLayout(filePath);
            } catch (IOException e) {
                Tools.showError(this, e);
            }
        }

        // Floating action button opens/closes the control menu panel.
        binding.customctrlMenuToggle.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(drawerNavigationView)) {
                drawerLayout.closeDrawer(drawerNavigationView);
            } else {
                drawerLayout.openDrawer(drawerNavigationView);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.customctrlControllayout.askToExit(CustomControlsActivity.this);
            }
        });
    }

    /**
     * Called by ControlLayout's exit/save-and-exit dialogs to finish this activity.
     * The default implementation in EditorExitable is a no-op, so we override it here
     * to actually close the editor and return the user to the previous screen.
     */
    @Override
    public void exitEditor() {
        finish();
    }

    private void parseBundle() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mControlPath = bundle.getString(BUNDLE_CONTROL_PATH);
        }
    }
}
