package com.nickstephen.opensnap.util.tasks;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import android.content.Context;
import android.os.Bundle;

public class RegisterTask extends BaseRequestTask {
	private String mAge;
	private String mBirthday;
	private String mPassword;
	private String mUsername;
	//TODO: See if this works and implement a thingo to something with
	public RegisterTask(Context paramContext, String username, String password, GregorianCalendar dob) {
		super(paramContext);
		
		mUsername = username;
		mPassword = password;
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		format.setCalendar(dob);
		mBirthday = format.format(dob.getTime());
		mAge = getAge(dob).toString();
	}
	
	/**
	 * WTF does this do????
	 * @param dob
	 * @return
	 */
	private Integer getAge(GregorianCalendar dob) {
		GregorianCalendar calendar = new GregorianCalendar();
		int i = calendar.get(1);
		int j = calendar.get(2);
		int k = calendar.get(5);
		int m = i - dob.get(1);
		if ((j < dob.get(2)) || ((j == dob.get(2)) && (k < dob.get(5)))) {
			m--;
		}
	    return m;
	}

	@Override
	protected Bundle getParams() {
		Bundle bundle = new Bundle();
		bundle.putString("email", mUsername);
		bundle.putString("password", mPassword);
		bundle.putString("age", mAge);
		bundle.putString("birthday", mBirthday);
		return bundle;
	}

	@Override
	protected String getPath() {
		return "/bq/register/";
	}

	@Override
	protected String getTaskName() {
		return "RegisterTask";
	}

}
