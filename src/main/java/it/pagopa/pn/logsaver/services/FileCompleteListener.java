package it.pagopa.pn.logsaver.services;

import java.nio.file.Path;

public interface FileCompleteListener {

    public void notify(Path path);

}
