package net.easymodo.asagi;

import net.easymodo.asagi.exception.*;

public abstract class Board {
    public abstract byte[] getMediaPreview(Post h) throws ContentGetException;
    public abstract byte[] getMedia(Post h) throws ContentGetException;
    public abstract Page getPage(int pageNum, String lastMod) throws ContentGetException;
    public abstract Topic getThread(int threadNum, String lastMod) throws ContentGetException;
    
    public Page content(Request.Page pageReq) throws ContentGetException {
        return this.getPage(pageReq.getPageNum(), pageReq.getLastMod());
    }
    
    public Topic content(Request.Thread threadReq) throws ContentGetException {
        return this.getThread(threadReq.getThreadNum(), threadReq.getLastMod());
    }
}
