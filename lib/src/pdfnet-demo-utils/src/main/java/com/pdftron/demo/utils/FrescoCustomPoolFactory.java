// https://github.com/facebook/fresco/issues/2465
// This class is required to make java only fresco to work on API 19
package com.pdftron.demo.utils;

import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.core.MemoryChunkType;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;

public class FrescoCustomPoolFactory extends PoolFactory {

    private final int mChunkType;

    public FrescoCustomPoolFactory(PoolConfig config, @MemoryChunkType int chunkType) {
        super(config);
        this.mChunkType = chunkType;
    }

    @Override
    public PooledByteBufferFactory getPooledByteBufferFactory() {
        return getPooledByteBufferFactory(this.mChunkType);
    }
}
