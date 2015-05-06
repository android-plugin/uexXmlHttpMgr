package org.zywx.wbpalmstar.plugin.uexmultiHttp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class BodyFileSource implements BodySource {

    private File mFile;
    private String mFileName;

    public BodyFileSource(File file) throws FileNotFoundException {
        mFile = file;
        if (file != null) {
            if (!file.isFile()) {
                throw new FileNotFoundException("File is not a normal file.");
            }
            if (!file.canRead()) {
                throw new FileNotFoundException("File is not readable.");
            }
            mFileName = file.getName();       
        }
    }

    public BodyFileSource(String fileName, File file)throws FileNotFoundException {
        this(file);
        if (fileName != null) {
            mFileName = fileName;
        }
    }
    
    public long getLength() {
        if (mFile != null) {
            return mFile.length();
        } else {
            return 0;
        }
    }

    public String getFileName() {
        return (mFileName == null) ? "noname" : mFileName;
    }

    public InputStream createInputStream() throws IOException {
        if (mFile != null) {
            return new FileInputStream(mFile);
        } else {
            return new ByteArrayInputStream(new byte[] {});
        }
    }

}
