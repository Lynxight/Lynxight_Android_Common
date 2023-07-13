package com.lynxight.common.data_model;

import android.media.MediaDataSource;

import java.io.IOException;

public class ByteArrayMediaDataSource extends MediaDataSource {

    private final byte[] data;

    public ByteArrayMediaDataSource(byte[] data) {
//        assert data != null;
        this.data = data;
    }

    @Override
    public synchronized int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        synchronized (data){
            int length = data.length;
            if (position >= length) {
                return -1; // -1 indicates EOF
            } if (position + size > length) {
                size -= (position + size) - length ;
            }     System.arraycopy(data, (int)position, buffer, offset, size);
            return size;
        }
    }@Override
    public synchronized long getSize() throws IOException {
        synchronized (data) {
            return data.length;
        }
    }

    @Override
    public void close() throws IOException {
        // Nothing to do here
    }
}