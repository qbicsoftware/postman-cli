package life.qbic.io.commandline;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {
    public String[] getVersion() {
        String version = this.getClass().getPackage().getImplementationVersion();
        String title = this.getClass().getPackage().getImplementationTitle();

        return new String[] {title + " version " + version};
    }
}
