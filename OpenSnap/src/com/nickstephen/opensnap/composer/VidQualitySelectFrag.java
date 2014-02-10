package com.nickstephen.opensnap.composer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.nickstephen.lib.gui.ListFragment;
import com.nickstephen.opensnap.R;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.TextView;

import java.util.List;

/**
 * Created by Nick on 13/01/14.
 */
public class VidQualitySelectFrag extends ListFragment {
    public static final String FRAG_TAG = "VidQualitySelectFrag";
    public static final String KEY_CURRENT_CAM = "current_cam";
    public static final String KEY_CURRENT_QUAL = "current_q";

    int mCurrentCamera = 0;
    int mCurrentQuality = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = this.getArguments();
        if (args != null) {
            mCurrentCamera = args.getInt(KEY_CURRENT_CAM, 0);
            mCurrentQuality = args.getInt(KEY_CURRENT_QUAL, 1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.vid_quality_frag);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.setListAdapter(new QualityListAdapter(this.getActivity()));
        this.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ((CaptureActivity) VidQualitySelectFrag.this.getActivity()).setVidQualityResult(position);
                VidQualitySelectFrag.this.getFragmentManager().popBackStack();
            }
        });
    }

    private class QualityListAdapter extends ArrayAdapter<Integer> {
        boolean[] mQualList;

        @SuppressLint("NewApi")
        public QualityListAdapter(Context context) {
            super(context, 0);

            if (Build.VERSION.SDK_INT < 9) {
                mQualList = new boolean[2];
                for (int i = 0; i < 2; i++) {
                    try {
                        CamcorderProfile profile = CamcorderProfile.get(i);
                        mQualList[i] = true;
                    } catch (Exception e) {
                        mQualList[i] = false;
                    }
                }
            } else if (Build.VERSION.SDK_INT < 11) {
                mQualList = new boolean[2];
                for (int i = 0; i < 2; i++) {
                    try {
                        CamcorderProfile profile = CamcorderProfile.get(mCurrentCamera, i);
                        mQualList[i] = true;
                    } catch (Exception e) {
                        mQualList[i] = false;
                    }
                }
            } else if (Build.VERSION.SDK_INT < 15) {
                mQualList = new boolean[7];
                for (int i = 0; i < 7; i++) {
                    mQualList[i] = CamcorderProfile.hasProfile(mCurrentCamera, i);
                }
            } else {
                mQualList = new boolean[8];
                for (int i = 0; i < 8; i++) {
                    mQualList[i] = CamcorderProfile.hasProfile(mCurrentCamera, i);
                }
            }
        }

        @Override
        public int getCount() {
            return mQualList.length;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView txt = (TextView) VidQualitySelectFrag.this.getLayoutInflater().inflate(R.layout.vid_qual_item);

            switch (position) {
                case 0:
                    txt.setText(R.string.vid_low);
                    break;
                case 1:
                    txt.setText(R.string.vid_hi);
                    break;
                case 2:
                    txt.setText(R.string.vid_qcif);
                    break;
                case 3:
                    txt.setText(R.string.vid_cif);
                    break;
                case 4:
                    txt.setText(R.string.vid_480);
                    break;
                case 5:
                    txt.setText(R.string.vid_720);
                    break;
                case 6:
                    txt.setText(R.string.vid_1080);
                    break;
                case 7:
                    txt.setText(R.string.vid_qvga);
                    break;
            }

            if (mCurrentQuality == position) {
                txt.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.check, 0);
            }

            if (!mQualList[position]) {
                txt.setEnabled(false);
            }

            return txt;
        }

        @Override
        public boolean isEnabled(int position) {
            return mQualList[position];
        }
    }
}
