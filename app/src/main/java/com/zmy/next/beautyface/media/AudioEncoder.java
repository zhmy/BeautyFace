package com.zmy.next.beautyface.media;


public abstract class AudioEncoder {
	
	String rawAudioFile;
	int sampleRate;
	int channelCount;
	
	AudioEncoder(String rawAudioFile){
		this.rawAudioFile = rawAudioFile;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public void setChannelCount(int channelCount) {
		this.channelCount = channelCount;
	}
	
	public static AudioEncoder createAccEncoder(String rawAudioFile){
		return new AACAudioEncoder(rawAudioFile);
	}
	
	public abstract void encodeToFile(String outEncodeFile);
}
