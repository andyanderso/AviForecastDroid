package com.sebnarware.avalanche;

/**
 * Created by andy on 9/12/14.
 */
import java.io.File;

abstract class AlbumStorageDirFactory {
    public abstract File getAlbumStorageDir(String albumName);
}
