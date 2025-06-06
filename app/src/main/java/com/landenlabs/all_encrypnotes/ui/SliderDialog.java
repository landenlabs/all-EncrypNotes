/*
 *  Copyright (c) 2015 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 *  following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial
 *  portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Dennis Lang  (Dec-2015)
 *  @see <a href="https://LanDenLabs.com">https://LanDenLabs.com</a>
 *
 */

package com.landenlabs.all_encrypnotes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.landenlabs.all_encrypnotes.R;

/**
 * Slider Dialog (ex: scale font)
 *
 * @author Dennis Lang
 * @see <a href="https://LanDenLabs.com">https://LanDenLabs.com</a>
 */
@SuppressWarnings("Convert2Lambda")
public class SliderDialog extends DialogFragment {
    
    private static final String STATE_LAYOUT = "Layout";
    private static final String STATE_MSGNUM = "MsgNum";
  
    private int m_layoutId;
    private int m_msgNum;
    
    private OnSeekBarChangeListener m_seekListener;
    // OnClickListener m_clickListener;

    public SliderDialog() {
        m_layoutId = -1;
        m_msgNum = 0;
    }

    public static SliderDialog create(int layoutId, int msgNum) {
        SliderDialog sliderDialog = new SliderDialog();
        sliderDialog.m_layoutId = layoutId;
        sliderDialog.m_msgNum = msgNum;
        return sliderDialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            m_seekListener = (OnSeekBarChangeListener) activity;
            // m_clickListener = (OnClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement listeners!");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_LAYOUT, m_layoutId);
        outState.putInt(STATE_MSGNUM, m_msgNum);
    }
    
    /*
     * // Implement onCreateView if you want to appear in an existing UI. // (see onCreateView)
     * 
     * @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
     * savedInstanceState) { View view = inflater.inflate(mLayoutId, container, false); return
     * view; }
     */

    // Implement if want a floating dialog (see onCreateView)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (null != savedInstanceState) {
            m_layoutId = savedInstanceState.getInt(STATE_LAYOUT );
            m_msgNum = savedInstanceState.getInt(STATE_MSGNUM );
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = getActivity().getLayoutInflater().inflate(m_layoutId, null);
        builder.setView(view);

        SeekBar seek = view.findViewById(R.id.dialog_seekbar);
        if (seek != null) {
            seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                    m_seekListener.onProgressChanged(arg0, m_msgNum, arg2);
                    View view = (View) arg0.getParent();
                    TextView numTV = view.findViewById(R.id.dialog_num);
                    if (numTV != null)
                        numTV.setText((50 + arg1) + "%");
                }
            });
        }

        Button okBtn = view.findViewById(R.id.dialog_button);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // DialogInterface dialog = (DialogInterface) SliderDialog.this;
                // m_clickListener.onClick(dialog, m_msgNum);
                SliderDialog.this.dismiss();
            }
        });

        return builder.create();
    }
}
