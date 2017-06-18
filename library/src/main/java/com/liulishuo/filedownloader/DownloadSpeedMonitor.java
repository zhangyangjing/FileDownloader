/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader;

import android.os.SystemClock;

import java.security.InvalidParameterException;

/**
 * The downloading speed monitor.
 */

public class DownloadSpeedMonitor implements IDownloadSpeed.Monitor, IDownloadSpeed.Lookup {

    private long mLastRefreshTime;
    private long mLastRefreshSofarBytes;
    private long mStartSofarBytes;
    private long mStartTime;
    // KB/s
    private int mSpeed;

    private long mSoFarBytes;
    private long mTotalBytes;

    // The min interval millisecond for updating the download mSpeed.
    private int mMinIntervalUpdateSpeed = 5;

    private MeanArray mMeanArray = new MeanArray(10);

    @Override
    public void start() {
        this.mStartTime = SystemClock.uptimeMillis();
        this.mStartSofarBytes = this.mSoFarBytes;
    }

    @Override
    public void end(long sofarBytes) {
        if (mStartTime <= 0 || mStartSofarBytes <= 0) {
            return;
        }

        long downloadSize = sofarBytes - mStartSofarBytes;
        this.mLastRefreshTime = 0;
        long interval = SystemClock.uptimeMillis() - mStartTime;
        if (interval < 0) {
            mSpeed = (int) downloadSize;
        } else {
            mSpeed = (int) (downloadSize / interval);
        }
    }

    @Override
    public void update(long sofarBytes) {
        if (mMinIntervalUpdateSpeed <= 0) {
            return;
        }

        boolean isUpdateData = false;
        do {
            if (mLastRefreshTime == 0) {
                isUpdateData = true;
                break;
            }

            long interval = SystemClock.uptimeMillis() - mLastRefreshTime;
            if (interval >= mMinIntervalUpdateSpeed || (mSpeed == 0 && interval > 0)) {
                int speed = (int) ((sofarBytes - mLastRefreshSofarBytes) / interval);
                mMeanArray.addNum(speed);
                mSpeed = Math.max(0, mMeanArray.getMean());
                isUpdateData = true;
                break;
            }
        } while (false);

        if (isUpdateData) {
            mLastRefreshSofarBytes = sofarBytes;
            mLastRefreshTime = SystemClock.uptimeMillis();
        }

    }

    @Override
    public void reset() {
        this.mSpeed = 0;
        this.mLastRefreshTime = 0;
    }

    @Override
    public int getSpeed() {
        return this.mSpeed;
    }

    @Override
    public void setMinIntervalUpdateSpeed(int minIntervalUpdateSpeed) {
        this.mMinIntervalUpdateSpeed = minIntervalUpdateSpeed;
    }

    private static class MeanArray {
        private boolean mLooped;
        private int mIndex;
        private int[] mData;

        public MeanArray(int size) {
            if (size < 1)
                throw new InvalidParameterException();
            mData = new int[size];
        }

        public void addNum(int num) {
            if (mData.length == mIndex) {
                mIndex = 0;
                mLooped = true;
            }

            mData[mIndex++] = num;
        }

        public int getMean() {
            int sum = 0, length = mLooped ? mData.length : Math.min(mData.length, mIndex + 1);
            for (int i = 0; i < length; i++)
                sum += mData[i];
            return sum / length;
        }
    }
}
