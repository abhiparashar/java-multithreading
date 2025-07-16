import java.util.ArrayList;
import java.util.List;

public class FileDownloader implements Runnable {
    private final String url;
    private final String filename;

    public FileDownloader(String url, String filename) {
        this.url = url;
        this.filename = filename;
    }

    @Override
    public void run() {
        try {
            System.out.println("downloading start" + url);
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Download interrupted: " + filename);
        }
        System.out.println(filename + "downloaded successfully");
    }

    public static void main(String[] args) {
        List<String> urls = new ArrayList<>();
        urls.add(" http://example.com/file1.pdf");
        urls.add(" http://example.com/file2.pdf");
        urls.add(" http://example.com/file3.pdf");

        List<String>files = new ArrayList<>();
        files.add("file1");
        files.add("file2");
        files.add("file3");

        for (int i = 0; i < urls.size(); i++) {
            Thread thread = new Thread(new FileDownloader(urls.get(i), files.get(i)));
            thread.start();
        }
    }

}

