package net.easymodo.asagi.model.yotsuba;

@SuppressWarnings("UnusedDeclaration")
public class CatalogJson {
    private int page;
    private PostJson[] threads;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public PostJson[] getThreads() {
        return threads;
    }

    public void PostJson(PostJson[] threads) {
        this.threads = threads;
    }
}
