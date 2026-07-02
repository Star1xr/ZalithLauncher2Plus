package net.kdt.pojavlaunch.customcontrols;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;
import static org.lwjgl.glfw.CallbackBridge.isGrabbing;

import org.lwjgl.glfw.CallbackBridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.JsonSyntaxException;
import android.graphics.RectF;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.feature.log.Logging;
import com.movtery.zalithlauncher.game.control.legacy.LegacyButtonTracker;
import com.movtery.zalithlauncher.setting.AllSettings;
import com.movtery.zalithlauncher.task.Task;
import com.movtery.zalithlauncher.task.TaskExecutors;
import com.movtery.zalithlauncher.ui.dialog.EditControlInfoDialog;
import com.movtery.zalithlauncher.ui.dialog.SelectControlsDialog;
import com.movtery.zalithlauncher.ui.dialog.TipDialog;
import com.movtery.zalithlauncher.ui.subassembly.customcontrols.ControlInfoData;
import com.movtery.zalithlauncher.utils.path.PathManager;
import com.movtery.zalithlauncher.utils.stringutils.StringUtilsKt;

import net.kdt.pojavlaunch.MinecraftGLSurface;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlButton;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlJoystick;
import net.kdt.pojavlaunch.customcontrols.mouse.TouchEventProcessor;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlSubButton;
import net.kdt.pojavlaunch.customcontrols.handleview.ActionRow;
import net.kdt.pojavlaunch.customcontrols.handleview.ControlHandleView;
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlPopup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControlLayout extends FrameLayout {
        protected CustomControls mLayout;
        private ControlInfoData mInfoData;
        /* Accessible when inside the game by ControlInterface implementations, cached for perf. */
        private MinecraftGLSurface mGameSurface = null;

        /* Cache to buttons for performance purposes */
        private List<ControlInterface> mButtons;
        private boolean mModifiable = false;
        private boolean mIsModified;
        private boolean mControlVisible = false;

        private EditControlPopup mControlPopup = null;
        private ControlHandleView mHandleView;
        private ControlButtonMenuListener mMenuListener;
        public ActionRow mActionRow = null;
        public String mLayoutFileName;

        public ControlLayout(Context ctx) {
                super(ctx);
        }

        public ControlLayout(Context ctx, AttributeSet attrs) {
                super(ctx, attrs);
        }


        public void loadLayout(String jsonPath) throws IOException, JsonSyntaxException {
                File jsonFile = jsonPath != null ? new File(jsonPath) : new File(AllSettings.INSTANCE.getControlLayout().getValue());

                CustomControls layout;
                if (jsonFile.exists()) {
                        layout = LayoutConverter.loadAndConvertIfNecessary(getContext(), jsonFile.getAbsolutePath());
                } else {
                        layout = LayoutConverter.loadFromAssets(getContext(), "default.json");
                }
                if (layout != null) {
                        loadLayout(layout);
                        if (jsonFile.exists()) {
                                mLayoutFileName = StringUtilsKt.removeSuffix(jsonFile.getName(), ".json");
                        } else {
                                mLayoutFileName = "default";
                        }
                }
        }

        public void loadLayout(CustomControls controlLayout) {
                boolean sanitizedModified = false;
                if(controlLayout != null) {
                        sanitizedModified = LayoutSanitizer.sanitizeLayout(controlLayout);
                }
                mInfoData = controlLayout == null ? null : controlLayout.mControlInfoDataList;
                if (mInfoData == null) {
                        mInfoData = new ControlInfoData();
                }

                if(mActionRow == null){
                        mActionRow = new ActionRow(getContext());
                        addView(mActionRow);
                }

                removeAllButtons();
                if(mLayout != null) {
                        mLayout.mControlDataList = null;
                        mLayout = null;
                }

                System.gc();
                mapTable.clear();

                // When no layout provided, init to empty so add-button operations work
                if (controlLayout == null) {
                        mLayout = new CustomControls();
                        return;
                }

                mLayout = controlLayout;
                

                // Joystick(s) first, to workaround the touch dispatch
                for(ControlJoystickData joystick : mLayout.mJoystickDataList){
                        addJoystickView(joystick);
                }

                //CONTROL BUTTON
                for (ControlData button : controlLayout.mControlDataList) {
                        addControlView(button);
                }

                //CONTROL DRAWER
                for(ControlDrawerData drawerData : controlLayout.mDrawerDataList){
                        ControlDrawer drawer = addDrawerView(drawerData);
                        if(mModifiable) drawer.areButtonsVisible = true;
                }


                setModified(sanitizedModified);
                mButtons = null;
                getButtonChildren(); // Force refresh
        } // loadLayout

        //CONTROL BUTTON
        public void addControlButton(ControlData controlButton) {
                mLayout.mControlDataList.add(controlButton);
                addControlView(controlButton);
        }

        private void addControlView(ControlData controlButton) {
                final ControlButton view = new ControlButton(this, controlButton);

                if (!mModifiable) {
                        view.setAlpha(view.getProperties().opacity);
                        view.setFocusable(false);
                        view.setFocusableInTouchMode(false);
                }
                addView(view);

                setModified(true);
        }

        // CONTROL DRAWER
        public void addDrawer(ControlDrawerData drawerData){
                mLayout.mDrawerDataList.add(drawerData);
                addDrawerView();
        }

        private void addDrawerView(){
                addDrawerView(null);
        }

        private ControlDrawer addDrawerView(ControlDrawerData drawerData){

                final ControlDrawer view = new ControlDrawer(this,drawerData == null ? mLayout.mDrawerDataList.get(mLayout.mDrawerDataList.size()-1) : drawerData);

                if (!mModifiable) {
                        view.setAlpha(view.getProperties().opacity);
                        view.setFocusable(false);
                        view.setFocusableInTouchMode(false);
                }
                addView(view);
                //CONTROL SUB BUTTON
                for (ControlData subButton : view.getDrawerData().buttonProperties) {
                        addSubView(view, subButton);
                }

                setModified(true);
                return view;
        }

        //CONTROL SUB-BUTTON
        public void addSubButton(ControlDrawer drawer, ControlData controlButton){
                //Yep there isn't much here
                drawer.getDrawerData().buttonProperties.add(controlButton);
                addSubView(drawer, drawer.getDrawerData().buttonProperties.get(drawer.getDrawerData().buttonProperties.size()-1 ));
        }

        private void addSubView(ControlDrawer drawer, ControlData controlButton){
                final ControlSubButton view = new ControlSubButton(this, controlButton, drawer);

                if (!mModifiable) {
                        view.setAlpha(view.getProperties().opacity);
                        view.setFocusable(false);
                        view.setFocusableInTouchMode(false);
                }else{
                        view.setVisible(true);
                }

                addView(view);
                drawer.addButton(view);


                setModified(true);
        }

        // JOYSTICK BUTTON
        public void addJoystickButton(ControlJoystickData data){
                mLayout.mJoystickDataList.add(data);
                addJoystickView(data);
        }

        private void addJoystickView(ControlJoystickData data){
                ControlJoystick view = new ControlJoystick(this, data);

                if (!mModifiable) {
                        view.setAlpha(view.getProperties().opacity);
                        view.setFocusable(false);
                        view.setFocusableInTouchMode(false);
                }
                addView(view);

        }


        private void removeAllButtons() {
                for(ControlInterface button : getButtonChildren()){
                        removeView(button.getControlView());
                }

                System.gc();
                //i wanna be sure that all the removed Views will be removed after a reload
                //because if frames will slowly go down after many control changes it will be warm and bad
        }

        public void saveLayout(String path) throws Exception {
                mLayout.save(path);
                setModified(false);
        }

        public void toggleControlVisible(){
                mControlVisible = !mControlVisible;
                setControlVisible(mControlVisible);
        }

        public float getLayoutScale(){
                return mLayout.scaledAt;
        }

        public CustomControls getLayout(){
                return mLayout;
        }

        public void setControlVisible(boolean isVisible) {
                if (mModifiable) return; // Not using on custom controls activity

                mControlVisible = isVisible;
                for(ControlInterface button : getButtonChildren()){
                        button.setVisible(((button.getProperties().displayInGame && isGrabbing()) || (button.getProperties().displayInMenu && !isGrabbing())) && isVisible);
                }
        }

        public void setModifiable(boolean isModifiable) {
                if(!isModifiable && mModifiable){
                        removeEditWindow();
                }
                mModifiable = isModifiable;
                if(isModifiable){
                        // In edit mode, all controls have to be shown
                        for(ControlInterface button : getButtonChildren()){
                                button.setVisible(true);
                        }
                }
        }

        public boolean getModifiable(){
                return mModifiable;
        }

        public void setModified(boolean isModified) {
                mIsModified = isModified;
        }

        public List<ControlInterface> getButtonChildren(){
                if(mModifiable || mButtons == null){
                        mButtons = new ArrayList<>();
                        for(int i=0; i<getChildCount(); ++i){
                                View v = getChildAt(i);
                                if(v instanceof ControlInterface)
                                        mButtons.add(((ControlInterface) v));
                        }
                }

                return mButtons;
        }

        public void refreshControlButtonPositions(){
                for(ControlInterface button : getButtonChildren()){
                        button.setDynamicX(button.getProperties().dynamicX);
                        button.setDynamicY(button.getProperties().dynamicY);
                }
        }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if(child instanceof ControlInterface && mControlPopup != null){
            mControlPopup.disappearColor();
            mControlPopup.disappear();
        }
    }

    /**
         * Load the layout if needed, and pass down the burden of filling values
         * to the button at hand.
         */
        public void editControlButton(ControlInterface button){
                if(mControlPopup == null){
                        // When the panel is null, it needs to inflate first.
                        // So inflate it, then process it on the next frame
                        mControlPopup = new EditControlPopup(getContext(), this);
                        post(() -> editControlButton(button));
                        return;
                }

                mControlPopup.internalChanges = true;
                mControlPopup.setCurrentlyEditedButton(button);

                mControlPopup.appear(button.getControlView().getX() + button.getControlView().getWidth()/2f < currentDisplayMetrics.widthPixels/2f);
                button.loadEditValues(mControlPopup);

                mControlPopup.internalChanges = false;
                mControlPopup.disappearColor();

                if(mHandleView == null){
                        mHandleView = new ControlHandleView(getContext());
                        addView(mHandleView);
                }
                mHandleView.setControlButton(button);

                //mHandleView.show();
        }

        /** Swap the panel if the button position requires it */
        public void adaptPanelPosition(){
                if(mControlPopup != null) mControlPopup.adaptPanelPosition();
        }


        final HashMap<View, ControlInterface> mapTable = new HashMap<>();

        //While this is called onTouch, this should only be called from a ControlButton.
        public void onTouch(View v, MotionEvent ev) {
                ControlInterface lastControlButton = mapTable.get(v);

                // Map location to screen coordinates
                ev.offsetLocation(v.getX(), v.getY());


                //Check if the action is cancelling, reset the lastControl button associated to the view
                if (ev.getActionMasked() == MotionEvent.ACTION_UP
                                || ev.getActionMasked() == MotionEvent.ACTION_CANCEL
                                || ev.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
                        if (lastControlButton != null) lastControlButton.sendKeyPresses(false);
                        mapTable.put(v, null);
                        return;
                }

                if (ev.getActionMasked() != MotionEvent.ACTION_MOVE) return;


                //Optimization pass to avoid looking at all children again
                if (lastControlButton != null) {
                        System.out.println("last control button check" + ev.getX() + "-" + ev.getY() + "-" + lastControlButton.getControlView().getX() + "-" + lastControlButton.getControlView().getY());
                        if (ev.getX() > lastControlButton.getControlView().getX()
                                        && ev.getX() < lastControlButton.getControlView().getX() + lastControlButton.getControlView().getWidth()
                                        && ev.getY() > lastControlButton.getControlView().getY()
                                        && ev.getY() < lastControlButton.getControlView().getY() + lastControlButton.getControlView().getHeight()) {
                                return;
                        }
                }

                //Release last keys
                if (lastControlButton != null) lastControlButton.sendKeyPresses(false);
                mapTable.remove(v);

                // Update the state of all swipeable buttons
                for (ControlInterface button : getButtonChildren()) {
                        if (!button.getProperties().isSwipeable) continue;

                        if (ev.getX() > button.getControlView().getX()
                                        && ev.getX() < button.getControlView().getX() + button.getControlView().getWidth()
                                        && ev.getY() > button.getControlView().getY()
                                        && ev.getY() < button.getControlView().getY() + button.getControlView().getHeight()) {

                                //Press the new key
                                if (!button.equals(lastControlButton)) {
                                        button.sendKeyPresses(true);
                                        mapTable.put(v, button);
                                        return;
                                }

                        }
                }
        }

        // Game-area touch processor (legacy ZL1 mode only).
        // Routes bare-surface MotionEvents (no button/joystick child consumed them)
        // to InGUIEventProcessor (menu) via setGameTouchProcessor, set by PojavControlLayout.
        // Camera rotation in grab mode is handled by dispatchTouchEvent below.
        private TouchEventProcessor mGameTouchProcessor;
        public void setGameTouchProcessor(TouchEventProcessor processor) {
                mGameTouchProcessor = processor;
        }

        // Camera rotation tracking fields (grab mode, legacy ZL1 only).
        // Owned exclusively by dispatchTouchEvent; reset to -1 when not grabbing.
        private int mCameraPointerId = -1;
        private float mCameraLastX, mCameraLastY;

        /**
         * In game (non-editor) mode, intercepts every touch event to track camera rotation
         * for the pointer that lands on an empty area of the screen (not over any button/joystick).
         * Children (buttons, joysticks) are dispatched first via super so they continue to work
         * normally; the camera update runs afterwards using the same event object.
         *
         * This approach lives in the Android View layer and is always reliable, unlike a Compose
         * pointerInteropFilter which stops receiving ACTION_MOVE events when it returns false on
         * ACTION_DOWN (Compose interprets false as "gesture not claimed").
         */
        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
                // Camera tracking MUST run before super so that ControlLayout.onTouch()'s
                // ev.offsetLocation() call on the shared MotionEvent does not corrupt the
                // coordinates we read.  Children call offsetLocation to translate to local
                // View space and do not restore the event afterwards, so any read after
                // super.dispatchTouchEvent() would see shifted, invalid coordinates.
                if (!mModifiable) {
                        handleCameraEvent(event);
                        // On the first pointer down, refresh the button rect registry so the
                        // Touch Controller Compose modifier knows which screen areas belong to
                        // Legacy buttons and should not be forwarded as gameplay touch events.
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                                updateLegacyButtonTracker();
                        }
                }
                return super.dispatchTouchEvent(event);
        }

        /**
         * Collects the screen-local bounds of all currently visible Legacy control buttons
         * and pushes them into [LegacyButtonTracker] so the Touch Controller Compose modifier
         * can skip forwarding those pointer positions to the mod's proxy client.
         */
        private void updateLegacyButtonTracker() {
                List<RectF> rects = new ArrayList<>();
                for (ControlInterface button : getButtonChildren()) {
                        View v = button.getControlView();
                        if (v.getVisibility() == View.VISIBLE) {
                                rects.add(new RectF(v.getX(), v.getY(),
                                        v.getX() + v.getWidth(), v.getY() + v.getHeight()));
                        }
                }
                LegacyButtonTracker.INSTANCE.updateButtonRects(rects);
        }

        @SuppressLint("ClickableViewAccessibility")
        private void handleCameraEvent(MotionEvent event) {
                if (!isGrabbing()) {
                        mCameraPointerId = -1;
                        return;
                }

                // Read sensitivity per-event so in-session changes take effect immediately.
                float sensitivity = ((Number) AllSettings.INSTANCE.getMouseSpeed().getValue()).floatValue() / 100f;

                switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: {
                                float x = event.getX(0);
                                float y = event.getY(0);
                                if (!isPointOverAnyChild(x, y)) {
                                        mCameraPointerId = event.getPointerId(0);
                                        mCameraLastX = x;
                                        mCameraLastY = y;
                                }
                                break;
                        }
                        case MotionEvent.ACTION_POINTER_DOWN: {
                                if (mCameraPointerId == -1) {
                                        int idx = event.getActionIndex();
                                        float x = event.getX(idx);
                                        float y = event.getY(idx);
                                        if (!isPointOverAnyChild(x, y)) {
                                                mCameraPointerId = event.getPointerId(idx);
                                                mCameraLastX = x;
                                                mCameraLastY = y;
                                        }
                                }
                                break;
                        }
                        case MotionEvent.ACTION_MOVE: {
                                if (mCameraPointerId != -1) {
                                        int idx = event.findPointerIndex(mCameraPointerId);
                                        if (idx >= 0) {
                                                float x = event.getX(idx);
                                                float y = event.getY(idx);
                                                float dx = (x - mCameraLastX) * sensitivity;
                                                float dy = (y - mCameraLastY) * sensitivity;
                                                mCameraLastX = x;
                                                mCameraLastY = y;
                                                if (dx != 0f || dy != 0f) {
                                                        CallbackBridge.sendCursorDelta(dx, dy);
                                                }
                                        }
                                }
                                break;
                        }
                        case MotionEvent.ACTION_POINTER_UP: {
                                if (mCameraPointerId != -1 &&
                                        event.getPointerId(event.getActionIndex()) == mCameraPointerId) {
                                        mCameraPointerId = -1;
                                }
                                break;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                                mCameraPointerId = -1;
                                break;
                        }
                }
        }

          /**
           * Returns true if (x, y) — in ControlLayout local coordinates — falls within any
           * visible child.  Uses getX()/getY() because buttons are positioned with setX/setY.
           */
          public boolean isPointOverAnyChild(float x, float y) {
                  for (int i = getChildCount() - 1; i >= 0; i--) {
                          View child = getChildAt(i);
                          if (child.getVisibility() != View.VISIBLE) continue;
                          float cx = child.getX(), cy = child.getY();
                          if (x >= cx && x <= cx + child.getWidth()
                                          && y >= cy && y <= cy + child.getHeight()) {
                                  return true;
                          }
                  }
                  return false;
          }
          @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
                // Game mode (!mModifiable): delegate to ZL1-native touch processor.
                // Only reached when no button/joystick child consumed the event.
                if (!mModifiable) {
                        if (mGameTouchProcessor != null) {
                                return mGameTouchProcessor.processTouchEvent(event);
                        }
                        return false;
                }

                // Editor mode: suppress until ACTION_UP with popup present.
                if (event.getActionMasked() != MotionEvent.ACTION_UP || mControlPopup == null)
                        return true;

                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);

                // When the input window cannot be hidden, it returns false
                if(!imm.hideSoftInputFromWindow(getWindowToken(), 0)){
                        if(mControlPopup.disappearLayer()){
                                mActionRow.setFollowedButton(null);
                                mHandleView.hide();
                        }
                }
                return true;
        }

        public void removeEditWindow() {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);

                // When the input window cannot be hidden, it returns false
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
                if(mControlPopup != null) {
                        mControlPopup.disappearColor();
                        mControlPopup.disappear();
                }

                if(mActionRow != null) mActionRow.setFollowedButton(null);
                if(mHandleView != null) mHandleView.hide();
        }

        public void save(String path){
                try {
                        mLayout.save(path);
                } catch (IOException e) {
                        Logging.e("ControlLayout", "Failed to save the layout at:" + path);}
        }


        public boolean hasMenuButton() {
                for(ControlInterface controlInterface : getButtonChildren()){
                        for (int keycode : controlInterface.getProperties().keycodes) {
                                if (keycode == ControlData.SPECIALBTN_MENU) return true;
                        }
                }
                return false;
        }

        public void setMenuListener(ControlButtonMenuListener menuListener) {
                this.mMenuListener = menuListener;
        }

        public void notifyAppMenu() {
                if(mMenuListener != null) mMenuListener.onClickedMenu();
        }

        /** Cached getter for perf purposes */
        public MinecraftGLSurface getGameSurface(){
                if(mGameSurface == null){
                        mGameSurface = findViewById(R.id.main_game_render_view);
                }
                return mGameSurface;
        }

        public void askToExit(EditorExitable editorExitable) {
                if(mIsModified) {
                        openSaveAndExitDialog(editorExitable);
                }else{
                        openExitDialog(editorExitable);
                }
        }

        public String saveToDirectory(String name) throws Exception{
                String jsonPath = PathManager.DIR_CTRLMAP_PATH + "/" + name + ".json";
                saveLayout(jsonPath);
                return jsonPath;
        }

        private void saveDialog(String title, Task<?> confirmTask) {
                EditControlInfoDialog infoDialog = new EditControlInfoDialog(getContext(), true, mLayoutFileName, mInfoData);

                if (title != null && !title.isEmpty()) infoDialog.setTitle(title);

                infoDialog.setOnConfirmClickListener((fileName, controlInfoData) -> {
                        try {
                                String jsonPath = saveToDirectory(fileName);
                                Toast.makeText(getContext(), getContext().getString(R.string.generic_save) + ": " + jsonPath, Toast.LENGTH_SHORT).show();
                                if (confirmTask != null) confirmTask.execute();
                        } catch (Throwable th) {
                                Tools.showError(getContext(), th, true);
                        }

                        infoDialog.dismiss();
                });
                infoDialog.show();
        }

        public void openSaveDialog() {
                saveDialog(getContext().getString(R.string.generic_save), null);
        }

        public void openSaveAndExitDialog(EditorExitable editorExitable) {
                saveDialog(getContext().getString(R.string.global_save_and_exit),
                                Task.runTask(TaskExecutors.getAndroidUI(), () -> {
                                        editorExitable.exitEditor();
                                        return null;
                                }));
        }

        public void openLoadDialog() {
                SelectControlsDialog dialog = new SelectControlsDialog(getContext(), file -> {
                        try {
                                loadLayout(file.getAbsolutePath());
                        } catch (IOException e) {
                                Tools.showError(getContext(), e);
                        }
                });
                dialog.show();
        }

        public void openSetDefaultDialog() {
                SelectControlsDialog dialog = new SelectControlsDialog(getContext(), file -> {
                        String absolutePath = file.getAbsolutePath();
                        try {
                                AllSettings.INSTANCE.getControlLayout().save(absolutePath);
                                loadLayout(absolutePath);
                        } catch (IOException|JsonSyntaxException e) {
                                Tools.showError(getContext(), e);
                        }
                });
                dialog.setTitleText(R.string.customctrl_selectdefault);
                dialog.show();
        }

        public void openExitDialog(EditorExitable exitListener) {
                new TipDialog.Builder(getContext())
                                .setTitle(R.string.customctrl_editor_exit_title)
                                .setMessage(R.string.customctrl_editor_exit_msg)
                                .setConfirmClickListener(checked -> exitListener.exitEditor())
                                .showDialog();
        }

        public boolean areControlVisible(){
                return mControlVisible;
        }

        /**
         * Update CallbackBridge screen dimensions when the editor canvas is measured,
         * then refresh all button positions so they appear on-screen.
         */
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                if (w > 0 && h > 0) {
                        org.lwjgl.glfw.CallbackBridge.physicalWidth = w;
                        org.lwjgl.glfw.CallbackBridge.physicalHeight = h;
                        refreshControlButtonPositions();
                }
        }

}
