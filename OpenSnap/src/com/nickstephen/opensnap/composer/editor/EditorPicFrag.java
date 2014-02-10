package com.nickstephen.opensnap.composer.editor;

import android.os.Bundle;

import com.nickstephen.opensnap.R;
import com.nickstephen.opensnap.gui.BaseContactSelectFrag;
import com.nickstephen.opensnap.gui.SnapEditorBaseFrag;
import com.nickstephen.opensnap.gui.SnapEditorPicFrag;

public class EditorPicFrag extends SnapEditorPicFrag {
	public EditorPicFrag() {
	}

    @Override
	protected void onSendCallback(Bundle args) {
		ContactSelectFrag contactFrag = new ContactSelectFrag();
		args.putString(BaseContactSelectFrag.SELECTED_CONTACT_KEY, getUser());
        args.putBoolean(SnapEditorBaseFrag.FIRST_TIME_KEY, isFirstTime());
		contactFrag.setArguments(args);
		this.getFragmentManager().beginTransaction()
			.setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out, R.anim.push_right_in, R.anim.push_right_out)
			//.add(R.id.fragment_container, contactFrag, BaseContactSelectFrag.FRAG_TAG)
                .replace(R.id.fragment_container, contactFrag, BaseContactSelectFrag.FRAG_TAG)
                .addToBackStack(null).commit();
	}
}
