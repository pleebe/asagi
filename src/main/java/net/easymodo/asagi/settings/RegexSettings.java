package net.easymodo.asagi.settings;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class RegexSettings {
    private String subject;
    private String comment;
    private String mediaHash;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getMediaHash() {
        return mediaHash;
    }

    public void setMediaHash(String mediaHash) {
        this.mediaHash = mediaHash;
    }
}
