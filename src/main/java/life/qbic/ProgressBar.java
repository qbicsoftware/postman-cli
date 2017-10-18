package life.qbic;


public class ProgressBar {

    private float nextProgressJump;
    private float stepSize;
    private String fileName;
    private Long totalFileSize;
    private Long downloadedSize;
    private final int BARSIZE=20;

    public ProgressBar(){}

    public ProgressBar(String fileName, long totalFileSize){
        this.fileName = fileName;
        this.totalFileSize = totalFileSize;
        this.downloadedSize = 0L;
        this.stepSize = totalFileSize / BARSIZE;
        this.nextProgressJump = this.stepSize;
    }

    public void updateProgress(int addDownloadedSize){
        this.downloadedSize += (long) addDownloadedSize;
        checkForJump();
    }

    private void checkForJump() {
        if(this.downloadedSize > this.nextProgressJump){
            this.nextProgressJump += this.stepSize;
            drawProgress();
        }
    }

    private void drawProgress() {
        System.out.print(String.format("%s: %s\r", this.fileName,buildProgressBar()));
    }

    private String buildProgressBar(){
        String progressBar = "[";
        int numberProgressStrings = Math.min((int) (this.downloadedSize / this.stepSize), BARSIZE);
        for (int i = 0; i<numberProgressStrings; i++){
            progressBar += "#";
        }
        for (int i = numberProgressStrings; i<BARSIZE; i++){
            progressBar += " ";
        }
        progressBar += "]";
        progressBar += String.format("%.02f/%.02f", (1.0/(1024*1024))*this.downloadedSize,
                (1.0/(1024*1024))*this.totalFileSize);
        return progressBar;
    }


}

