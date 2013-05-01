package com.atlarge.motionlog;

import android.os.Parcel;
import android.os.Parcelable;

public class StatusUpdatePacket implements Parcelable {
	private int mEventsCount;
	private float mTotalEventsRate;
	private float mLatestEventsRate;
	protected StatusUpdatePacket(int eventsCount, float totalEventsRate, float latestEventsRate) {
		mEventsCount = eventsCount;
		mTotalEventsRate = totalEventsRate;
		mLatestEventsRate = latestEventsRate;
	}
	public int eventsCount() { return mEventsCount; }
	public float totalEventsRate() { return mTotalEventsRate; }
	public float latestEventsRate() { return mLatestEventsRate; }
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mEventsCount);
		dest.writeFloat(mTotalEventsRate);
		dest.writeFloat(mLatestEventsRate);
	}
	
	public static final Parcelable.Creator<StatusUpdatePacket> CREATOR
		= new Parcelable.Creator<StatusUpdatePacket>() {
			public StatusUpdatePacket createFromParcel(Parcel in) {
					return new StatusUpdatePacket(in);
			}
		public StatusUpdatePacket[] newArray(int size) {
				return new StatusUpdatePacket[size];
		}
	};

	private StatusUpdatePacket(Parcel in) {
		mEventsCount = in.readInt();
		mTotalEventsRate = in.readFloat();
		mLatestEventsRate = in.readFloat();
	}
	
}
