package life.qbic.io.commandline;

import picocli.CommandLine;

public class ManifestVersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        String implementationVersion = getClass().getPackage().getImplementationVersion();
        return new String[]{
                "version: " + implementationVersion,
                "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                "OS: ${os.name} ${os.version} ${os.arch}"
        };
    }
}
