package net.easymodo.asagi;

import java.io.InputStream;

import net.easymodo.asagi.exception.*;
import net.easymodo.asagi.model.MediaPost;
import net.easymodo.asagi.model.Page;
import net.easymodo.asagi.model.Topic;

public abstract class Board {
    public abstract InputStream getMediaPreview(MediaPost h) throws ContentGetException, CfBicClearParseException;
    public abstract InputStream getMedia(MediaPost h) throws ContentGetException, CfBicClearParseException;
    public abstract Page getPage(int pageNum, String lastMod) throws ContentGetException, ContentParseException, CfBicClearParseException;
    public abstract Topic getThread(int threadNum, String lastMod) throws ContentGetException, ContentParseException, CfBicClearParseException;
    public abstract Page getAllThreads(String lastMod) throws ContentGetException, ContentParseException, CfBicClearParseException;
}
