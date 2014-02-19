package com.apb.beacon;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.TextView;

import com.apb.beacon.calculator.Calculator;
import com.apb.beacon.trigger.HardwareTriggerService;
import com.apb.beacon.trigger.MultiClickEvent;
import com.google.inject.Inject;

import roboguice.inject.ContentView;

@ContentView(R.layout.calculator_layout)
public class CalculatorActivity extends PanicButtonActivity {
	private static final int[] buttons = {R.id.one, R.id.two, R.id.three,
		R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine,
		R.id.zero, R.id.equals_sign, R.id.plus, R.id.minus, R.id.multiply,
		R.id.divide};

	@Inject private Calculator calculator;
	private int lastClickId = -1;

    boolean mHasPerformedLongPress;
    Runnable mPendingCheckForLongPress;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerButtonEvents();
		startService(new Intent(this, HardwareTriggerService.class));

        ApplicationSettings.setWizardState(this, AppConstants.WIZARD_FLAG_COMPLETE);
	}

	private void registerButtonEvents() {
		for(int button : buttons) {
			Button equalsButton = (Button) findViewById(button);
            equalsButton.setOnTouchListener(touchListener);
//			equalsButton.setOnLongClickListener(longClickListener);
			equalsButton.setOnClickListener(clickListener);
		}
	}

	private View.OnClickListener clickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			int id = view.getId();
			switch(id) {
			case R.id.one:
				handleButtonPress(Calculator.Button.ONE);
				break;
			case R.id.two:
				handleButtonPress(Calculator.Button.TWO);
				break;
			case R.id.three:
				handleButtonPress(Calculator.Button.THREE);
				break;
			case R.id.four:
				handleButtonPress(Calculator.Button.FOUR);
				break;
			case R.id.five:
				handleButtonPress(Calculator.Button.FIVE);
				break;
			case R.id.six:
				handleButtonPress(Calculator.Button.SIX);
				break;
			case R.id.seven:
				handleButtonPress(Calculator.Button.SEVEN);
				break;
			case R.id.eight:
				handleButtonPress(Calculator.Button.EIGHT);
				break;
			case R.id.nine:
				handleButtonPress(Calculator.Button.NINE);
				break;
			case R.id.zero:
				handleButtonPress(Calculator.Button.ZERO);
				break;
			case R.id.equals_sign:
				handleButtonPress(Calculator.Button.EQUALS);
				break;
			case R.id.plus:
				handleButtonPress(Calculator.Button.PLUS);
				break;
			case R.id.minus:
				handleButtonPress(Calculator.Button.MINUS);
				break;
			case R.id.multiply:
				handleButtonPress(Calculator.Button.MULTIPLY);
				break;
			case R.id.divide:
				handleButtonPress(Calculator.Button.DIVIDE);
				break;
			}
			MultiClickEvent multiClickEvent = (MultiClickEvent) view.getTag();
			if (multiClickEvent == null) {
				multiClickEvent = resetEvent(view);
			}
			// Don't activate multi-click if different buttons are clicked
			if(id != lastClickId) multiClickEvent.reset();
			lastClickId = id;
			multiClickEvent.registerClick(System.currentTimeMillis());
			if (multiClickEvent.isActivated()) {
				CalculatorActivity.this.finish();
				getPanicAlert().activate();
				resetEvent(view);
			}
		}
	};


    private View.OnTouchListener touchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(final View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:

                    if (!mHasPerformedLongPress) {
                        // This is a tap, so remove the longpress check
                        if (mPendingCheckForLongPress != null) {
                            v.removeCallbacks(mPendingCheckForLongPress);
                        }
                        // v.performClick();
                    }

                    break;
                case MotionEvent.ACTION_DOWN:
                    if (mPendingCheckForLongPress == null) {
                        mPendingCheckForLongPress = new Runnable() {
                            public void run() {
                                startActivity(new Intent(CalculatorActivity.this, LoginActivity.class));
                                overridePendingTransition(R.anim.show_from_top, R.anim.hide_to_bottom);
                            }
                        };
                    }


                    mHasPerformedLongPress = false;
                    v.postDelayed(mPendingCheckForLongPress, 3000);

                    break;
                case MotionEvent.ACTION_MOVE:
                    final int x = (int) event.getX();
                    final int y = (int) event.getY();

                    // Be lenient about moving outside of buttons
                    int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
                    if ((x < 0 - slop) || (x >= v.getWidth() + slop) ||
                            (y < 0 - slop) || (y >= v.getHeight() + slop)) {

                        if (mPendingCheckForLongPress != null) {
                            v.removeCallbacks(mPendingCheckForLongPress);
                        }
                    }
                    break;
                default:
                    return false;
            }

            return false;
        }

    };

//	private View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
//		@Override
//		public boolean onLongClick(View view) {
//			startActivity(new Intent(CalculatorActivity.this, LoginActivity.class));
//			overridePendingTransition(R.anim.show_from_top, R.anim.hide_to_bottom);
//			return true;
//		}
//	};

	private MultiClickEvent resetEvent(View view) {
		MultiClickEvent multiClickEvent = new MultiClickEvent();
		view.setTag(multiClickEvent);
		return multiClickEvent;
	}

	private void handleButtonPress(Calculator.Button button) {
		TextView display = (TextView) findViewById(R.id.display);
		display.setText(calculator.handleButtonPress(button));
	}
}