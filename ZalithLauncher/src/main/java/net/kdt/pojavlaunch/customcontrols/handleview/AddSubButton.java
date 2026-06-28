package net.kdt.pojavlaunch.customcontrols.handleview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.ui.view.AnimButton;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;

@SuppressLint("AppCompatCustomView")
public class AddSubButton extends AnimButton implements ActionButtonInterface {
    public AddSubButton(Context context) {super(context); init();}
    public AddSubButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}

    public void init() {
        setOnClickListener(this);
        setText(R.string.customctrl_addsubbutton);
    }

    private ControlInterface mCurrentlySelectedButton = null;

    @Override
    public boolean shouldBeVisible() {
        return mCurrentlySelectedButton != null && mCurrentlySelectedButton instanceof ControlDrawer;
    }

    @Override
    public void setFollowedView(ControlInterface view) {
        mCurrentlySelectedButton = view;
    }

    @Override
    public void onClick() {
        if(mCurrentlySelectedButton instanceof ControlDrawer){
            ((ControlDrawer)mCurrentlySelectedButton).getControlLayoutParent().addSubButton(
                    (ControlDrawer)mCurrentlySelectedButton,
                    new ControlData(getContext().getString(R.string.controls_add_control_button))
            );
        }
    }


}
